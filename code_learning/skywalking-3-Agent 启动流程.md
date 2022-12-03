## Skywalking Agent

### Agent 总体流程

启动方式：

- 静态启动

  - 使用 -javaagent 参数在服务启动时挂在 agent
  - 入口方法：premain

  - 特点：对字节码的操作自由度比较高，只要符合验证阶段要求，通过JVM验证即可

  - 适用场景：需要对字节码做大量修改（如apm）

- 动态附加

  - 在服务运行时使用 Attach API 挂在Agent
  - 入口方法：agentmain
  - 特点：对字节码的操作自由度比较低
  - 适用场景：系统诊断（如阿里 Arthas）

Skywalking Agent Premain方法

代码位置：/apm-sniffer/apm-agent，是 agent 执行的主入口

启动步骤：1. 初始化配置；2.加载插件；3.插桩定制 Agent；4.启动服务；5.注册关闭钩子

```java
public class SkyWalkingAgent {
/**
     * -javaagent:/path/to/agent.jar=k1=v1,k2=v2...
     * 等号之后都是参数,也就是入参agentArgs
     * -javaagent参数必须在-jar之前
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) throws PluginException {
        final PluginFinder pluginFinder;
        try {
            // 1.初始化配置
            SnifferConfigInitializer.initializeCoreConfig(agentArgs);
        } catch (Exception e) {
            // try to resolve a new logger, and use the new logger to write the error log here
            LogManager.getLogger(SkyWalkingAgent.class)
                    .error(e, "SkyWalking agent initialized failure. Shutting down.");
            return;
        } finally {
            // refresh logger again after initialization finishes
            LOGGER = LogManager.getLogger(SkyWalkingAgent.class);
        }

        try {
            // 2.加载插件
            pluginFinder = new PluginFinder(new PluginBootstrap().loadPlugins());
        } catch (AgentPackageNotFoundException ape) {
            LOGGER.error(ape, "Locate agent.jar failure. Shutting down.");
            return;
        } catch (Exception e) {
            LOGGER.error(e, "SkyWalking agent initialized failure. Shutting down.");
            return;
        }

        // 3.插桩定制化Agent行为
        final ByteBuddy byteBuddy = new ByteBuddy().with(TypeValidation.of(Config.Agent.IS_OPEN_DEBUGGING_CLASS));

        AgentBuilder agentBuilder = new AgentBuilder.Default(byteBuddy).ignore(
                nameStartsWith("net.bytebuddy.")
                        .or(nameStartsWith("org.slf4j."))
                        .or(nameStartsWith("org.groovy."))
                        .or(nameContains("javassist"))
                        .or(nameContains(".asm."))
                        .or(nameContains(".reflectasm."))
                        .or(nameStartsWith("sun.reflect"))
                        .or(allSkyWalkingAgentExcludeToolkit())
                        .or(ElementMatchers.isSynthetic()));

        JDK9ModuleExporter.EdgeClasses edgeClasses = new JDK9ModuleExporter.EdgeClasses();
        try {
            agentBuilder = BootstrapInstrumentBoost.inject(pluginFinder, instrumentation, agentBuilder, edgeClasses);
        } catch (Exception e) {
            LOGGER.error(e, "SkyWalking agent inject bootstrap instrumentation failure. Shutting down.");
            return;
        }

        try {
            agentBuilder = JDK9ModuleExporter.openReadEdge(instrumentation, agentBuilder, edgeClasses);
        } catch (Exception e) {
            LOGGER.error(e, "SkyWalking agent open read edge in JDK 9+ failure. Shutting down.");
            return;
        }

        if (Config.Agent.IS_CACHE_ENHANCED_CLASS) {
            try {
                agentBuilder = agentBuilder.with(new CacheableTransformerDecorator(Config.Agent.CLASS_CACHE_MODE));
                LOGGER.info("SkyWalking agent class cache [{}] activated.", Config.Agent.CLASS_CACHE_MODE);
            } catch (Exception e) {
                LOGGER.error(e, "SkyWalking agent can't active class cache.");
            }
        }

        agentBuilder.type(pluginFinder.buildMatch())
                    .transform(new Transformer(pluginFinder))
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                    .with(new RedefinitionListener())
                    .with(new Listener())
                    .installOn(instrumentation);

        try {
            // 4.启动服务
            ServiceManager.INSTANCE.boot();
        } catch (Exception e) {
            LOGGER.error(e, "Skywalking agent boot failure.");
        }

        // 5.注册关闭钩子
        Runtime.getRuntime()
                .addShutdownHook(new Thread(ServiceManager.INSTANCE::shutdown, "skywalking service shutdown thread"));
}
```



### 1. 初始化配置流程

Agent打包目录下的config目录中有个`agent.config`的文件，这就是Agent的配置文件

配置信息的优先级：java -javaagent后面携带的参数优先级最高，配置文件夹中的优先级最低

```java
public class SnifferConfigInitializer {
    public static void initializeCoreConfig(String agentOptions) {
        // 加载配置信息 优先级:agent参数 > 系统环境变量 > /config/agent.config
        AGENT_SETTINGS = new Properties();
        //agent 配置文件
        try (final InputStreamReader configFileStream = loadConfig()) {
            AGENT_SETTINGS.load(configFileStream);
            for (String key : AGENT_SETTINGS.stringPropertyNames()) {
                String value = (String) AGENT_SETTINGS.get(key);
                // 配置值里的占位符替换
                AGENT_SETTINGS.put(key, PropertyPlaceholderHelper.INSTANCE.replacePlaceholders(value, AGENT_SETTINGS));
            }

        } catch (Exception e) {
            LOGGER.error(e, "Failed to read the config file, skywalking is going to run in default config.");
        }

        try {
            // 系统环境变量
            overrideConfigBySystemProp();
        } catch (Exception e) {
            LOGGER.error(e, "Failed to read the system properties.");
        }

        // agent 参数
        agentOptions = StringUtil.trim(agentOptions, ',');
        if (!StringUtil.isEmpty(agentOptions)) {
            try {
                agentOptions = agentOptions.trim();
                LOGGER.info("Agent options is {}.", agentOptions);

                overrideConfigByAgentOptions(agentOptions);
            } catch (Exception e) {
                LOGGER.error(e, "Failed to parse the agent options, val is {}.", agentOptions);
            }
        }

        // 1)将配置信息映射到Config类
        initializeConfig(Config.class);
        // 根据配置信息重新指定日志解析器
        configureLogger();
        LOGGER = LogManager.getLogger(SnifferConfigInitializer.class);

        // 检查agent名称和后端地址是否配置
        if (StringUtil.isEmpty(Config.Agent.SERVICE_NAME)) {
            throw new ExceptionInInitializerError("`agent.service_name` is missing.");
        }
        if (StringUtil.isEmpty(Config.Collector.BACKEND_SERVICE)) {
            throw new ExceptionInInitializerError("`collector.backend_service` is missing.");
        }
        // peer 字段长度判断
        if (Config.Plugin.PEER_MAX_LENGTH <= 3) {
            LOGGER.warn(
                "PEER_MAX_LENGTH configuration:{} error, the default value of 200 will be used.",
                Config.Plugin.PEER_MAX_LENGTH
            );
            Config.Plugin.PEER_MAX_LENGTH = 200;
        }

        // 标记配置加载完成
        IS_INIT_COMPLETED = true;
    }
}
```

### 2. 加载插件

```java
public List<AbstractClassEnhancePluginDefine> loadPlugins() throws AgentPackageNotFoundException {
    //初始化自定义类加载器AgentClassLoader
    AgentClassLoader.initDefaultLoader();
    PluginResourcesResolver resolver = new PluginResourcesResolver();
    List<URL> resources = resolver.getResources();
    if (resources == null || resources.size() == 0) {
        LOGGER.info("no plugin files (skywalking-plugin.def) found, continue to start application.");
        return new ArrayList<AbstractClassEnhancePluginDefine>();
    }
    for (URL pluginUrl : resources) {
        try {
            PluginCfg.INSTANCE.load(pluginUrl.openStream());
        } catch (Throwable t) {
            LOGGER.error(t, "plugin file [{}] init failure.", pluginUrl);
        }
    }
    List<PluginDefine> pluginClassList = PluginCfg.INSTANCE.getPluginClassList();
    List<AbstractClassEnhancePluginDefine> plugins = new ArrayList<AbstractClassEnhancePluginDefine>();
    for (PluginDefine pluginDefine : pluginClassList) {
        try {
            LOGGER.debug("loading plugin class {}.", pluginDefine.getDefineClass());
            AbstractClassEnhancePluginDefine plugin = (AbstractClassEnhancePluginDefine) Class.forName(pluginDefine.getDefineClass(), true, AgentClassLoader
                .getDefault()).newInstance();
            plugins.add(plugin);
        } catch (Throwable t) {
            LOGGER.error(t, "load plugin [{}] failure.", pluginDefine.getDefineClass());
        }
    }
    plugins.addAll(DynamicPluginLoader.INSTANCE.load(AgentClassLoader.getDefault()));
    return plugins;
}
```

#### 2.1 类加载器的并行加载模式

AgentClassLoader的静态代码块里调用ClassLoader的方法

```
public class AgentClassLoader extends ClassLoader {
    static {
        registerAsParallelCapable();
    }
```

并行能力的类加载器:

在JDK 1.7之前，类加载器在加载类的时候是串行加载的，比如有100个类需要加载，那么就排队，加载完上一个再加载下一个，这样加载效率就很低

在JDK 1.7之后，就提供了类加载器并行能力，就是把锁的粒度变小，之前ClassLoader加载类的时候加锁的时候是用自身作为锁的

#### 2.2 AgentClassLoader 加载流程

1. 调用 `registerAsParallelCapable()` 方法开启并行加载
2. 调用 `initDefaultLoader`() 初始化默认类加载器
3. 加载在 agent 主目录下的plugins和activations的所有插件
4. 如果被加载的类标注了@PluginConfig注解，会加载插件的配置

#### 2.3 Agent 插件定义

**插件代码位置：**

- apm-sniffer/apm-sdk-plugin
- apm-sniffer/apm-toolkit-activation

**插件定义：**

命名规则：XXXInstrumentation.java

```
public class JedisInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {


}
```

**插件声明：**

resources目录下的`skywalking-plugin.def`文件是对插件的声明

```
# 插件名=插件定义全类名
jedis-2.x=org.apache.skywalking.apm.plugin.jedis.v2.define.JedisClusterInstrumentation
jedis-2.x=org.apache.skywalking.apm.plugin.jedis.v2.define.JedisInstrumentation
```

### 3. 插桩定制 Agent

### 4. 启动插件服务

Agent 在 premain 中调用`ServiceManager.INSTANCE.boot()`来启动插件服务，并利用SPI机制加载配置的各种插件的`BootService`，然后依次遍历BootService实例，调用他们的prepare()准备方法、startup();启动方法和onComplete()完成方法。

 负责加载所有的`BootService` 实现类，以及启动这些实现类

```java
public enum ServiceManager {
	public void boot() {
        //去加载 BootService接口的实现类
        bootedServices = loadAllServices();
        //调用 BootService接口 的prepare 方法
        prepare();
        //调用 BootService接口 boot 方法
        startup();
        //调用 BootService接口 onComplete 方法
        onComplete();
    }
    
    //加载插件方法
    void load(List<BootService> allServices) {
    	for (final BootService bootService : ServiceLoader.load(BootService.class, AgentClassLoader.getDefault())){ 
        	allServices.add(bootService);
        }
    }
}
```

`BootService` 接口定义：

```java
public interface BootService {
    void prepare() throws Throwable;
    void boot() throws Throwable;
    void onComplete() throws Throwable;
    void shutdown() throws Throwable;
}
```

主要实现类

#### GRPCChannelManager

作用：Agent到OAP的GRPC网络连接管理

它只实现了boot() 和 shutdown() 方法

```java
@Override
public void boot() {
    //解析用户配置的 skywalking.collector.backend_service 参数，如果写了多个IP地址用逗号分隔
    grpcServers = Arrays.asList(Config.Collector.BACKEND_SERVICE.split(","));
    //这里开了一个定时任务，30s执行一次
    connectCheckFuture = Executors.newSingleThreadScheduledExecutor(
        new DefaultNamedThreadFactory("GRPCChannelManager")
    ).scheduleAtFixedRate(
        new RunnableWithExceptionProtection(
            this,
            t -> LOGGER.error("unexpected exception.", t)
        ), 0, Config.Collector.GRPC_CHANNEL_CHECK_INTERVAL, TimeUnit.SECONDS
    );
}
```

run方法：

```java
@Override
public void run() {
    //如果需要重连和刷新dns，刷新当前配置的域名对应的ip地址，进行格式化，组成OAP地址列表
    if (IS_RESOLVE_DNS_PERIODICALLY && reconnect) {
        String backendService = Config.Collector.BACKEND_SERVICE.split(",")[0];
        try {
            String[] domainAndPort = backendService.split(":");
            List<String> newGrpcServers = Arrays
                    .stream(InetAddress.getAllByName(domainAndPort[0]))
                    .map(InetAddress::getHostAddress)
                    .map(ip -> String.format("%s:%s", ip, domainAndPort[1]))
                    .collect(Collectors.toList());
            grpcServers = newGrpcServers;
        } catch (Throwable t) {
            LOGGER.error(t, "Failed to resolve {} of backend service.", backendService);
        }
    }
    //如果需要重连网络连接
    if (reconnect) {
        if (grpcServers.size() > 0) {
            String server = "";
            try {
                //随机选择一个服务器
                int index = Math.abs(random.nextInt()) % grpcServers.size();
                //如果这次选到的IP和上次选到的不同就生成 GRPCChannel 对象
                if (index != selectedIdx) {
                    selectedIdx = index;
                    server = grpcServers.get(index);
                    String[] ipAndPort = server.split(":");
                    if (managedChannel != null) {
                        managedChannel.shutdownNow();
                    }
                   //选出来的 ip地址创建一个 managedChannel
                   managedChannel = GRPCChannel.newBuilder(ipAndPort[0], Integer.parseInt(ipAndPort[1]))
                            .addManagedChannelBuilder(new StandardChannelBuilder())
                            .addManagedChannelBuilder(new TLSChannelBuilder())
                            .addChannelDecorator(new AgentIDDecorator())
                            .addChannelDecorator(new AuthenticationDecorator())
                            .build();
                    //通知所有的GRPCChannelListener已连接
                    notify(GRPCChannelStatus.CONNECTED);
                    reconnectCount = 0;
                    reconnect = false;
                } else if (managedChannel.isConnected(++reconnectCount > Config.Agent.FORCE_RECON
                    // Reconnect to the same server is automatically done by GRPC,
                    // therefore we are responsible to check the connectivity and
                    // set the state and notify listeners
                    reconnectCount = 0;
                    notify(GRPCChannelStatus.CONNECTED);
                    reconnect = false;
                }
                return;
            } catch (Throwable t) {
                LOGGER.error(t, "Create channel to {} fail.", server);
            }
        }
    }
```

总结：

1. 随机选择一个服务器ip和端口
2. 根据ip和端口创建 managedChannel 用于与OAP实例建立连接
3. 调用notify()方法通知所有的GRPCChannelListener连接成功的状态
4. 设置重新连接为false
5. 所有使用到 `ManagedChannel` 对象的地方，如果发送失败了，都会去调用 `reportError()` 的方法，重新设置`reconnect = true`

#### ServiceManagementClient

作用：1.向OAP汇报自身信息；2.保持心跳连接

同时实现了 GRPCChannelListener 和 BootService 接口，`statusChanged()`方法

```java
/**
 * 1.将当前Agent Client的基本信息汇报给OAP
 * 2.和OAP保持心跳
 */
@DefaultImplementor
public class ServiceManagementClient implements BootService, Runnable, GRPCChannelListener {
	// 当前网络连接状态
    private volatile GRPCChannelStatus status = GRPCChannelStatus.DISCONNECT;   
    // 网络服务
    private volatile ManagementServiceGrpc.ManagementServiceBlockingStub managementServiceBlockingStub; 
    @Override
    public void statusChanged(GRPCChannelStatus status) {
        // 网络是否是已连接状态
        if (GRPCChannelStatus.CONNECTED.equals(status)) {
            // 找到GRPCChannelManager服务,拿到网络连接
            Channel channel = ServiceManager.INSTANCE.findService(GRPCChannelManager.class).getChannel();
            // grpc的stub可以理解为在protobuf中定义的XxxService
            managementServiceBlockingStub = ManagementServiceGrpc.newBlockingStub(channel);
        } else {
            managementServiceBlockingStub = null;
        }
        this.status = status;
    }
```

prepare阶段：

```java
@Override
public void prepare() {
    //向GRPCChannelManager注册自己为监听器
    ServiceManager.INSTANCE.findService(GRPCChannelManager.class).addChannelListener(this);
    SERVICE_INSTANCE_PROPERTIES = new ArrayList<>();
    //把配置文件中的Agent Client信息放入集合,等待发送
    for (String key : Config.Agent.INSTANCE_PROPERTIES.keySet()) {
        SERVICE_INSTANCE_PROPERTIES.add(KeyStringValuePair.newBuilder().setKey(key)
    	.setValue(Config.Agent.INSTANCE_PROPERTIES.get(key)).build());
    }
    //服务实例名是否存在，不存在就使用 UUID + IP 生成一个
    Config.Agent.INSTANCE_NAME = StringUtil.isEmpty(Config.Agent.INSTANCE_NAME)
        ? UUID.randomUUID().toString().replaceAll("-", "") + "@" + OSUtil.getIPV4()
        : Config.Agent.INSTANCE_NAME;
}
```

boot阶段：

```java
@DefaultImplementor
public class ServiceManagementClient implements BootService, Runnable, GRPCChannelListener {
    private volatile ScheduledFuture<?> heartbeatFuture; // 心跳定时任务  	
	@Override
    public void boot() {
        heartbeatFuture = Executors.newSingleThreadScheduledExecutor(
            new DefaultNamedThreadFactory("ServiceManagementClient")
        ).scheduleAtFixedRate(
            new RunnableWithExceptionProtection(
                this,
                t -> LOGGER.error("unexpected exception.", t)
            ), 0, Config.Collector.HEARTBEAT_PERIOD,
            TimeUnit.SECONDS
        );
}
```

`boot()`方法中初始化心跳定时任务heartbeatFuture，Runnable传入的是this，实际上执行的是ServiceManagementClient的`run()`方法

```java
@Override
public void run() {
    LOGGER.debug("ServiceManagementClient running, status:{}.", status);
    //网络是否是已连接状态
    if (GRPCChannelStatus.CONNECTED.equals(status)) {
        try {
            if (managementServiceBlockingStub != null) {
                //心跳周期 = 30s, 信息汇报频率因子 = 10 => 每5分钟向OAP汇报一次Agent Client Properties
                if (Math.abs(sendPropertiesCounter.getAndAdd(1)) % Config.Collector.PROPERTIES_REPORT_PERIOD_FACTOR == 0) {
                    managementServiceBlockingStub
                        //设置请求超时时间,默认30秒
                        .withDeadlineAfter(GRPC_UPSTREAM_TIMEOUT, TimeUnit.SECONDS)
                        .reportInstanceProperties(InstanceProperties.newBuilder()
                                                                    .setService(Config.Agent.SERVICE_NAME)
                                                                    .setServiceInstance(Config.Agent.INSTANCE_NAME)
                                                                    .addAllProperties(OSUtil.buildOSInfo(
                                                                        Config.OsInfo.IPV4_LIST_SIZE))
                                                                    .addAllProperties(SERVICE_INSTANCE_PROPERTIES)
                                                                    .build());
                } else {
                    //服务端给到的响应交给CommandService去处理
                    final Commands commands = managementServiceBlockingStub.withDeadlineAfter(
                        GRPC_UPSTREAM_TIMEOUT, TimeUnit.SECONDS
                    ).keepAlive(InstancePingPkg.newBuilder()
                                               .setService(Config.Agent.SERVICE_NAME)
                                               .setServiceInstance(Config.Agent.INSTANCE_NAME)
                                               .build());
                    ServiceManager.INSTANCE.findService(CommandService.class).receiveCommand(commands);
                }
            }
        } catch (Throwable t) {
            LOGGER.error(t, "ServiceManagementClient execute fail.");
            ServiceManager.INSTANCE.findService(GRPCChannelManager.class).reportError(t);
        }
    }
}
```

心跳周期为30s，信息汇报频率因子为10，所以每5分钟向OAP汇报一次Agent Client Properties

判断本次是否是Agent信息上报

1）如果本次是信息上报，上报Agent信息，包括：服务名、实例名、Agent Client的信息、当前操作系统的信息、JVM信息

2）如果本次不是信息上报，请求服务端，将服务端给到的响应交给CommandService去处理

#### CommandService

作用：接收 OAP 返回的Command，分发给不同的处理器去处理



#### ProfileTaskChannelService

作用：用于处理性能剖析任务

```java
@Override
    public void prepare() {
        //向GRPCChannelManager注册自己为监听器
        ServiceManager.INSTANCE.findService(GRPCChannelManager.class).addChannelListener(this);
    }

    @Override
    public void boot() {
        //找到发送segment的sender
        sender = ServiceManager.INSTANCE.findService(ProfileSnapshotSender.class);
        if (Config.Profile.ACTIVE) {
            // 创建获取任务列表定时器，20s执行一次
            getTaskListFuture = Executors.newSingleThreadScheduledExecutor(
                new DefaultNamedThreadFactory("ProfileGetTaskService")
            ).scheduleWithFixedDelay(
                new RunnableWithExceptionProtection(
                    this,
                    t -> LOGGER.error("Query profile task list failure.", t)
                ), 0, Config.Collector.GET_PROFILE_TASK_INTERVAL, TimeUnit.SECONDS
            );
			// 创建发送segment快照定时器，500ms执行一次
            sendSnapshotFuture = Executors.newSingleThreadScheduledExecutor(
                new DefaultNamedThreadFactory("ProfileSendSnapshotService")
            ).scheduleWithFixedDelay(
                new RunnableWithExceptionProtection(
                    () -> {
                        List<TracingThreadSnapshot> buffer = new ArrayList<>(Config.Profile.SNAPSHOT_TRANSPORT_BUFFER_SIZE);
                        snapshotQueue.drainTo(buffer);
                        if (!buffer.isEmpty()) {
                            sender.send(buffer);
                        }
                    },
                    t -> LOGGER.error("Profile segment snapshot upload failure.", t)
                ), 0, 500, TimeUnit.MILLISECONDS
            );
        }
    }
```

getTaskListFuture 的run方法：

```java
@Override
public void run() {
    if (status == GRPCChannelStatus.CONNECTED) {
        try {
            ProfileTaskCommandQuery.Builder builder = ProfileTaskCommandQuery.newBuilder();
            // 把服务ID和实例ID传给skywalking 看看有没对应的 ProfileTask
            builder.setService(Config.Agent.SERVICE_NAME).setServiceInstance(Config.Agent.INSTANCE_NAME);
 builder.setLastCommandTime(ServiceManager.INSTANCE.findService(ProfileTaskExecutionService.class)
                                                              .getLastCommandCreateTime());
            Commands commands = profileTaskBlockingStub.withDeadlineAfter(GRPC_UPSTREAM_TIMEOUT, TimeUnit.SECONDS)
                                                       .getProfileTaskCommands(builder.build());
            ServiceManager.INSTANCE.findService(CommandService.class).receiveCommand(commands);
        } catch (Throwable t) {
    }
}
```



#### SamplingService

作用：采样搜集，可以限定 `3S`内发送多少 `trace` 数据，超过的数据将会被丢弃

### Agent Config 主要配置说明

| 配置项                                               | 说明                                                         |
| ---------------------------------------------------- | ------------------------------------------------------------ |
| agent.service_name                                   | 在SkyWalking UI中展示的服务名。                              |
| collector.backend_service                            | oap地址，grpc数据上报                                        |
| agent.sample_n_per_3_secs                            | 每3秒取多少次采样；默认情况下-1，代表全采样；这个值可通过Skywalking的动态配置功能来实现运行期的动态调整 |
| logging.level                                        | 调试阶段可将日志级别修改为DEBUG                              |
| agent.span_limit_per_segment                         | 单个segment中的span的最大个数。通过这个配置项，Skywalking可评估应用程序内存使用量。默认值300 |
| collector.grpc_channel_check_interval                | 检查grpc的channel状态的时间间隔。                            |
| collector.app_and_service_register_check_interval    | 检查应用和服务的注册状态的时间间隔。                         |
| plugin.springmvc.use_qualified_name_as_endpoint_name | 如果为true，endpoint的name为方法的全限定名，而不是请求的URL。默认为false。（仅针对springmvc） |
| plugin.toolit.use_qualified_name_as_operation_name   | 如果为true，operation的name为方法的全限定名，而不是给定的operation name。默认为false。 |
| plugin.postgresql.trace_sql_parameters               | 如果设置为true，则将收集sql的参数（通常为`java.sql.PreparedStatement`）。 |
