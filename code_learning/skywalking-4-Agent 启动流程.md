## Skywalking Agent

### Agent 总体流程

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

位置：/apm-sniffer/apm-agent

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

        // 3.定制化Agent行为
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

### Agent 配置加载流程

Agent打包目录下的config目录中有个`agent.config`的文件，这就是Agent的配置文件

配置信息的优先级：java -javaagent后面携带的参数优先级最高，配置文件夹中的优先级最低

```java
public class SnifferConfigInitializer {
    public static void initializeCoreConfig(String agentOptions) {
        // 加载配置信息 优先级:agent参数 > 系统环境变量 > /config/agent.config
        AGENT_SETTINGS = new Properties();
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

        // agent参数
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
  
    /**
     * 根据配置信息重新指定日志解析器 JSON和PATTERN两种日志格式
     */
    static void configureLogger() {
        switch (Config.Logging.RESOLVER) {
            case JSON:
                LogManager.setLogResolver(new JsonLogResolver());
                break;
            case PATTERN:
            default:
                LogManager.setLogResolver(new PatternLogResolver());
        }
}
```

### Agent 配置说明

| 配置项                                               | 说明                                                         |
| ---------------------------------------------------- | ------------------------------------------------------------ |
| agent.service_name                                   | 在SkyWalking UI中展示的服务名。                              |
| collector.backend_service                            | oap地址                                                      |
| agent.sample_n_per_3_secs                            | 每3秒取多少次采样；默认情况下-1，代表全采样；这个值可通过Skywalking的动态配置功能来实现运行期的动态调整 |
| logging.level                                        | 调试阶段可将日志级别修改为DEBUG                              |
| agent.span_limit_per_segment                         | 单个segment中的span的最大个数。通过这个配置项，Skywalking可评估应用程序内存使用量。默认值300 |
| collector.grpc_channel_check_interval                | 检查grpc的channel状态的时间间隔。                            |
| collector.app_and_service_register_check_interval    | 检查应用和服务的注册状态的时间间隔。                         |
| plugin.springmvc.use_qualified_name_as_endpoint_name | 如果为true，endpoint的name为方法的全限定名，而不是请求的URL。默认为false。 |
| plugin.toolit.use_qualified_name_as_operation_name   | 如果为true，operation的name为方法的全限定名，而不是给定的operation name。默认为false。 |
| plugin.postgresql.trace_sql_parameters               | 如果设置为true，则将收集sql的参数（通常为`java.sql.PreparedStatement`）。 |
|                                                      |                                                              |

