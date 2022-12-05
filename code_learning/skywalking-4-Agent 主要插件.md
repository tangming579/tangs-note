## Skywalking Agent

### GRPCChannelManager

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

### ServiceManagementClient

作用：1.向OAP汇报自身信息；2.保持心跳连接

同时实现了 GRPCChannelListener 和 BootService 接口

`statusChanged()`方法：

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

### CommandService

作用：接收 OAP 返回的Command，分发给不同的处理器去处理

`boot()` 方法：启动单线程线程池，执行在run里面，

```java
@DefaultImplementor
public class CommandService implements BootService, Runnable {
    private ExecutorService executorService = Executors.newSingleThreadExecutor(
    	new DefaultNamedThreadFactory("CommandService")
	);
    
    public void boot() throws Throwable {
    executorService.submit(
        	new RunnableWithExceptionProtection(this, t -> LOGGER.error(t, "CommandService failed"))
    	);
	}
}
```

`run()` 方法，取

```java
@Override
public void run() {
   final CommandExecutorService commandExecutorService = ServiceManager.INSTANCE.findService(CommandExecutorService.class);
        while (isRunning) {
               BaseCommand command = commands.take();
                if (isCommandExecuted(command)) {
                    continue;
                }
			  commandExecutorService.execute(command);
    }
}
```

### JVMService



### ProfileTaskChannelService

作用：用于处理性能剖析任务

开启两个定时器

- 20s执行一次

  1. 向skywalking查询当前服务实例有没有ProfileTask
  2. 如果有，就创建一个任务并把任务加入profileTaskList
  3. 然后开启一个定时器，在skywalking界面设置的开始时间后去执行任务
  4. 关闭老的剖析任务，然后开启两个线程：一个线程去执行当前性能剖析任务，另一个在到达指定时间后关闭当前性能剖析任务
  5. 执行剖析任务，开启死循环，不断去获取 currentProfiler ，拿到 dump 堆栈信息放到 snapshotQueue 里

- 500ms执行一次

  判断 snapshotQueue 中是否有 segment 快照，如果有就发送快照给skywalking

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
                            //TracingThreadSnapshot转换为ThreadSnapshot发送给server
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

每20s执行一次，把`服务ID`和`实例ID`传给skywalking 看看有没对应的 `ProfileTask` ， 如果有就让 `CommandService` 去执行对应的 `commands`，即 `ProfileTaskCommand` 

`ProfileTaskCommandExecutor` 代码：

```
public class ProfileTaskCommandExecutor implements CommandExecutor {
    @Override
    public void execute(BaseCommand command) throws CommandExecutionException {
        final ProfileTaskCommand profileTaskCommand = (ProfileTaskCommand) command;
        // 生成 profile task
        final ProfileTask profileTask = new ProfileTask();
        //任务id
        profileTask.setTaskId(profileTaskCommand.getTaskId());
        //监控端点名称
        profileTask.setFirstSpanOPName(profileTaskCommand.getEndpointName());
        //监控持续时间（minute）
        profileTask.setDuration(profileTaskCommand.getDuration());
        //起始监控时间（ms）
        profileTask.setMinDurationThreshold(profileTaskCommand.getMinDurationThreshold());
        //监控dump间隔（ms）
        profileTask.setThreadDumpPeriod(profileTaskCommand.getDumpPeriod());
        //最大采样数量
        profileTask.setMaxSamplingCount(profileTaskCommand.getMaxSamplingCount());
        //任务开始时间
        profileTask.setStartTime(profileTaskCommand.getStartTime());
        //任务创建时间
        profileTask.setCreateTime(profileTaskCommand.getCreateTime());
        // 添加到 ProfileTaskExecutionService的执行任务里
        ServiceManager.INSTANCE.findService(ProfileTaskExecutionService.class).addProfileTask(profileTask);
    }
}
```

`ProfileTaskExecutionService` 的 `addProfileTask`，把任务加入队列，然后开启一个定时器，在指定时间后去执行任务

```JAVA
public void addProfileTask(ProfileTask task) {        
    // 把ProfileTaskCommand 任务存入队列
    profileTaskList.add(task);
    //到达用户设置的时间后开启任务
    long timeToProcessMills = task.getStartTime() - System.currentTimeMillis();
    PROFILE_TASK_SCHEDULE.schedule(() -> processProfileTask(task),timeToProcessMills,TimeUnit.MILLISECONDS);
}
```

如果时间到了，就会执行下面的方法

```java
private synchronized void processProfileTask(ProfileTask task) {
        // 确保上一个 任务已经停止
        stopCurrentProfileTask(taskExecutionContext.get());
        // 创建一个新的任务
        final ProfileTaskExecutionContext currentStartedTaskContext = new ProfileTaskExecutionContext(task);
        taskExecutionContext.set(currentStartedTaskContext);
        // 开了一个线程去执行对应的性能剖析任务
        currentStartedTaskContext.startProfiling(PROFILE_EXECUTOR);
        PROFILE_TASK_SCHEDULE.schedule(
            () -> stopCurrentProfileTask(currentStartedTaskContext), task.getDuration(), TimeUnit.MINUTES);
}
```

最终执行的地方：

```java
public class ProfileThread implements Runnable {
  private void profiling(ProfileTaskExecutionContext executionContext) throws InterruptedException {
    int maxSleepPeriod = executionContext.getTask().getThreadDumpPeriod();
    // 开启一个死循环保证线程执行
    long currentLoopStartTime = -1;
    while (!Thread.currentThread().isInterrupted()) {
        currentLoopStartTime = System.currentTimeMillis();
        // 采集插槽
        AtomicReferenceArray<ThreadProfiler> profilers = executionContext.threadProfilerSlots();
        int profilerCount = profilers.length();
        for (int slot = 0; slot < profilerCount; slot++) {
            ThreadProfiler currentProfiler = profilers.get(slot);
            if (currentProfiler == null) {
                continue;
            }
            switch (currentProfiler.profilingStatus().get()) {
                case PENDING:
                    // check tracing context running time
                    currentProfiler.startProfilingIfNeed();
                    break;
                case PROFILING:
                    // 拿到 dump 堆栈信息
                    TracingThreadSnapshot snapshot = currentProfiler.buildSnapshot();
                    if (snapshot != null) {
                        profileTaskChannelService.addProfilingSnapshot(snapshot);
                    } else {
                        // tell execution context current tracing thread dump failed, stop it
                        executionContext.stopTracingProfile(currentProfiler.tracingContext());
                    }
                    break;
            }
        }
        // sleep 到下一个执行周期
        long needToSleep = (currentLoopStartTime + maxSleepPeriod) - System.currentTimeMillis();
        needToSleep = needToSleep > 0 ? needToSleep : maxSleepPeriod;
        Thread.sleep(needToSleep);
    }
}
```



### SamplingService

作用：正常的情况下，每一次的调用信息都会发送给 `skywalking`，但是这样可能会对系统性能造成一定的影响，`SamplingService` 的作用就是可以限定 `3S`内发送多少 `trace` 数据，超过的数据将会被丢弃

```java
@DefaultImplementor
public class SamplingService implements BootService {
	@Override
	public void boot() {
    	ServiceManager.INSTANCE.findService(ConfigurationDiscoveryService.class)
                           .registerAgentConfigChangeWatcher(samplingRateWatcher);
    	handleSamplingRateChanged();
	}
}
```



### 