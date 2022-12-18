## skywalking 服务生命周期

### 概念介绍：OAP 的分布式计算

Skywalking 中需要计算的数据类型：

- Record数据，即明细数据，如Trace、访问日志等数据，由`RecordStreamProcessor`进行处理。
- Metrisc数据，即指标数据，绝大部分的OAL指标都会生成这种数据，由`MetricsStreamProcessor`进行处理。
- TopN数据，即周期性采样数据，如慢SQL的周期性采集，由`TopNStreamProcessor`进行处理。

### Agent 端：建立连接与服务注册

1. java 服务启动，执行 - javaagent，进入agent 中的`premain()`方法，在方法中执行 `ServiceManager.INSTANCE.boot()` 启动插件服务

   ```java
   public class SkyWalkingAgent {
   	public static void premain(String agentArgs, Instrumentation instrumentation) throws PluginException{
           ……
           try {
       		ServiceManager.INSTANCE.boot();
   		} catch (Exception e) {
       		LOGGER.error(e, "Skywalking agent boot failure.");
   		}
       }
   }
   ```

2. ServiceManager 加载所有插件服务，之后依次执行所有插件的 prepare、startup、onComplete方法

   ```java
   public enum ServiceManager {
     public void boot() {
       bootedServices = loadAllServices();
       prepare();
       startup();
       onComplete();
     }
     //加载插件方法
     void load(List<BootService> allServices) {
       for (BootService bootService:ServiceLoader.load(BootService.class,AgentClassLoader.getDefault())){ 
           	allServices.add(bootService);
           }
       }
   }
   ```

3. GRPCChannelManager 插件，读取 -javaagent参数中的服务器ip和端口，创建 managedChannel与OAP实例建立连接，通知所有GRPCChannelListener 当前连接状态

4. ServiceManagementClient 插件，本身也是GRPCChannelListener ，每5分钟向OAP汇报一次Agent Client Properties

   ```java
   @Override
   public void run() {
       ……    
       managementServiceBlockingStub
   		//设置请求超时时间,默认30秒
   		.withDeadlineAfter(GRPC_UPSTREAM_TIMEOUT, TimeUnit.SECONDS)
   		.reportInstanceProperties(
           	InstanceProperties.newBuilder()
           	.setServiceInstance(Config.Agent.INSTANCE_NAME)
           	.addAllProperties(OSUtil.buildOSInfo(Config.OsInfo.IPV4_LIST_SIZE))
           	.addAllProperties(SERVICE_INSTANCE_PROPERTIES).build()
             );
           ……
         } 
       }
   }
   ```


### OAP 端：接收、缓存、批量入库

模块位置：server-receiver-plugin/skywalking-management-receiver-plugin

该模块的 resource/META-INF/services 定义了 ModulProvider实现类 RegisterModuleProvider，主要代码：

```java
public class RegisterModuleProvider extends ModuleProvider {
	@Override
    public void start() {
    //gRPC 方式上报的处理
    GRPCHandlerRegister grpcHandlerRegister = getManager().find(SharingServerModule.NAME)
                                                          .provider()
                                                          .getService(GRPCHandlerRegister.class);
    ManagementServiceHandler managementServiceHandler = new ManagementServiceHandler(getManager());
    grpcHandlerRegister.addHandler(managementServiceHandler);
    grpcHandlerRegister.addHandler(new ManagementServiceHandlerCompat(managementServiceHandler));
    JettyHandlerRegister jettyHandlerRegister = getManager().find(SharingServerModule.NAME)
                                                            .provider()
                                                            .getService(JettyHandlerRegister.class);
    //前端 Rest 方式的处理
    jettyHandlerRegister.addHandler(new ManagementServiceReportPropertiesHandler(getManager()));
    jettyHandlerRegister.addHandler(new ManagementServiceKeepAliveHandler(getManager()));
	}
}
```

所以服务通过 gRPC 上报的最终处理逻辑在 ManagementServiceHandler

```java
@Override
public void reportInstanceProperties(final InstanceProperties request,
                                     final StreamObserver<Commands> responseObserver) {
    ServiceInstanceUpdate serviceInstanceUpdate = new ServiceInstanceUpdate();
    final String serviceName = namingControl.formatServiceName(request.getService());
    final String instanceName = namingControl.formatInstanceName(request.getServiceInstance());
    serviceInstanceUpdate.setServiceId(IDManager.ServiceID.buildId(serviceName, NodeType.Normal));
    serviceInstanceUpdate.setName(instanceName);
    JsonObject properties = new JsonObject();
    List<String> ipv4List = new ArrayList<>();
    request.getPropertiesList().forEach(prop -> {
        if (InstanceTraffic.PropertyUtil.IPV4.equals(prop.getKey())) {
            ipv4List.add(prop.getValue());
        } else {
            properties.addProperty(prop.getKey(), prop.getValue());
        }
    });
    properties.addProperty(InstanceTraffic.PropertyUtil.IPV4S, ipv4List.stream().collect(Collectors.joining(",")));
    serviceInstanceUpdate.setProperties(properties);
    serviceInstanceUpdate.setTimeBucket(
        TimeBucket.getTimeBucket(System.currentTimeMillis(), DownSampling.Minute));
    //调用receive
    sourceReceiver.receive(serviceInstanceUpdate);
    responseObserver.onNext(Commands.newBuilder().build());
    responseObserver.onCompleted();
}
```



```java
public class SourceReceiverImpl implements SourceReceiver {
    @Getter
    private final DispatcherManager dispatcherManager;

    @Override
    public void receive(ISource source) {
        dispatcherManager.forward(source);
    }
}
```



```java
public class DispatcherManager implements DispatcherDetectorListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(DispatcherManager.class);

    private Map<Integer, List<SourceDispatcher>> dispatcherMap;

    public DispatcherManager() {
        this.dispatcherMap = new HashMap<>();
    }

    public void forward(ISource source) {
        if (source == null) {
            return;
        }

        List<SourceDispatcher> dispatchers = dispatcherMap.get(source.scope());

        /**
         * Dispatcher  oal script analysis result.
         * So these will/could be possible, the given source doesn't have the dispatcher,
         * when the receiver is open, and oal script doesn't ask for analysis.
         */
        if (dispatchers != null) {
            source.prepare();
            // DispatcherManager会对Source进行分发
            for (SourceDispatcher dispatcher : dispatchers) {
                dispatcher.dispatch(source);
            }
        }
    }

    /**
     * Scan all classes under `org.apache.skywalking` package,
     * <p>
     * If it implement {@link org.apache.skywalking.oap.server.core.analysis.SourceDispatcher}, then, it will be added
     * into this DispatcherManager based on the Source definition.
     */
    public void scan() throws IOException, IllegalAccessException, InstantiationException {
        ClassPath classpath = ClassPath.from(this.getClass().getClassLoader());
        ImmutableSet<ClassPath.ClassInfo> classes = classpath.getTopLevelClassesRecursive("org.apache.skywalking");
        for (ClassPath.ClassInfo classInfo : classes) {
            Class<?> aClass = classInfo.load();

            addIfAsSourceDispatcher(aClass);
        }
    }
}

```



```java
public class InstanceUpdateDispatcher implements SourceDispatcher<ServiceInstanceUpdate> {
    @Override
    public void dispatch(final ServiceInstanceUpdate source) {
        InstanceTraffic traffic = new InstanceTraffic();
        traffic.setTimeBucket(source.getTimeBucket());
        traffic.setName(source.getName());
        traffic.setServiceId(source.getServiceId());
        traffic.setLastPingTimestamp(source.getTimeBucket());
        traffic.setProperties(source.getProperties());
        // 指标Stream聚合处理器，执行Stream流式处理
        // 执行MetricsAggregateWorker.in() 进行L1聚合处理，之后传递给下一个Worker
        MetricsStreamProcessor.getInstance().in(traffic);
    }
}
```



```java
public class MetricsStreamProcessor implements StreamProcessor<Metrics> {
	@Override
	public void in(Metrics metrics) {
    	MetricsAggregateWorker worker = entryWorkers.get(metrics.getClass());
    	if (worker != null) {
        	worker.in(metrics);
    	}
	}
}
```

实际上就是把metrics放到缓存里了：

```java
public class MetricsAggregateWorker extends AbstractWorker<Metrics> {
    // 维护了一个本地的轻量级消息队列模型
    // 主要目的为了防止收集方生成数据速度大于往后端发送数据速度造成的数据积压和生成方阻塞。
    private final DataCarrier<Metrics> dataCarrier;
    
    @Override
    public final void in(Metrics metrics) {
        dataCarrier.produce(metrics);
    }
}
```

关于持久化，由PersistenceTimer定义了一个任务，每次批量的从dataCarrier中消费缓存的metrics，并最终持久化到存储中。

```java
public enum PersistenceTimer {
    public void start(ModuleManager moduleManager, CoreModuleConfig moduleConfig) {
        prepareExecutorService = Executors.newFixedThreadPool(moduleConfig.getPrepareThreads());
        if (!isStarted) {
            // 默认值 25, 25秒执行一次数据的批量存储
            Executors.newSingleThreadScheduledExecutor()
                     .scheduleWithFixedDelay(new RunnableWithExceptionProtection(() -> 		extractDataAndSave(batchDAO), t -> log
                             .error("Extract data and save failure.", t)), 5, moduleConfig.getPersistentPeriod(),
                         TimeUnit.SECONDS
                     );

            this.isStarted = true;
        }
    }

    private void extractDataAndSave(IBatchDAO batchDAO) {
		long startTime = System.currentTimeMillis();
        try (HistogramMetrics.Timer allTimer = allLatency.createTimer()) {
            List<PersistenceWorker<? extends StorageData>> persistenceWorkers = new ArrayList<>();
            persistenceWorkers.addAll(TopNStreamProcessor.getInstance().getPersistentWorkers());
            persistenceWorkers.addAll(MetricsStreamProcessor.getInstance().getPersistentWorkers());

            CountDownLatch countDownLatch = new CountDownLatch(persistenceWorkers.size());
            persistenceWorkers.forEach(worker -> {
                prepareExecutorService.submit(() -> {
                    List<PrepareRequest> innerPrepareRequests = null;
                    try {
                        // 预处理阶段
                        try (HistogramMetrics.Timer timer = prepareLatency.createTimer()) {
                            // worker 中包含 dataCarrier
                            innerPrepareRequests = worker.buildBatchRequests();
                            worker.endOfRound();
                        } catch (Throwable e) {
                            log.error(e.getMessage(), e);
                        }

                        // 执行阶段
                        try (HistogramMetrics.Timer executeLatencyTimer = executeLatency.createTimer()) {
                            if (CollectionUtils.isNotEmpty(innerPrepareRequests)) {
                                // 以异步模式将数据推入数据库
                                batchDAO.flush(innerPrepareRequests);
                            }
                        } catch (Throwable e) {
                            log.error(e.getMessage(), e);
                        }
                    } finally {
                        countDownLatch.countDown();
                    }
                });
            });

            countDownLatch.await();
        } catch (Throwable e) {
            errorCounter.inc();
        } 
    }
}

```

