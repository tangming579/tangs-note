## skywalking 服务生命周期

### Agent 建立连接与服务注册

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

   

### OAP 接收处理

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
public class ManagementServiceHandler extends ManagementServiceGrpc.ManagementServiceImplBase implements GRPCHandler {
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
    properties.addProperty(InstanceTraffic.PropertyUtil.IPV4S, ipv4List.stream().collect(Collectors.joini
    serviceInstanceUpdate.setProperties(properties);
    serviceInstanceUpdate.setTimeBucket(
        TimeBucket.getTimeBucket(System.currentTimeMillis(), DownSampling.Minute));
    sourceReceiver.receive(serviceInstanceUpdate);
    responseObserver.onNext(Commands.newBuilder().build());
    responseObserver.onCompleted();
  }
}
```

