## skywalking 服务生命周期

概念

SkyWalking Agent收集硬件、JVM监控指标以及指标上报等都是一个个服务模块组织起来的

SkyWalking Agent中的服务就是实现了`org.apache.skywalking.apm.agent.core.boot.BootService`接口的类，这个接口定义了一个服务的生命周期

```java
public interface BootService {
    /**
     * 准备阶段
     */
    void prepare() throws Throwable;
	/**
     * 启动阶段
     */
    void boot() throws Throwable;
	/**
     * 启动完成阶段
     */
    void onComplete() throws Throwable;
	/**
     * 关闭阶段
     */
    void shutdown() throws Throwable;
}
```

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

4. ServiceManagementClient 插件，本身也是GRPCChannelListener ，收到

### OAP 接收处理