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

