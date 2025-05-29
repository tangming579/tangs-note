### 概念回顾

- traceId: 在一个 `链路` 中 `traceId` 唯一
- segmentId : 同一个 `线程链路`中，这个值都是相同的，不同线程链路中这个值不同
- spanId : 同一个线程中唯一, 从0始，按照调用链路从0递增
- parentSpanId : 在同一个`线程的链路`中，用来连接 `span`

### TracingContext 类详解

 TracingContext 是链路跟踪逻辑核心控制器。

位置：apm-sniffer/apm-agent-core/context/TracingContext.java

#### AbstractTracerContext

TracingContext 继承 AbstractTracerContext，先看一下AbstractTracerContext：

```java
public interface AbstractTracerContext {
	/**
     * 将 carrier 信息跨进程传递
     */
    void inject(ContextCarrier carrier);
    
    /**
     * 解出跨进程传递的 carrier
     */
    void extract(ContextCarrier carrier);

    /**
     * 捕获跨线程的快照
     */
    ContextSnapshot capture();

    /**
     * 在当前segment 和跨线程的segment 建构建引用
     */
    void continued(ContextSnapshot snapshot);

    /**
     * 获取全局traceId
     */
    String getReadablePrimaryTraceId();   

    /**
     * 创建 entry span
     */
    AbstractSpan createEntrySpan(String operationName);

    /**
     * 创建 local span
     */
    AbstractSpan createLocalSpan(String operationName);

    /**
     * 创建 exit span
     */
    AbstractSpan createExitSpan(String operationName, String remotePeer);

    /**
     * 返回当前trace上下文活跃的span
     */
    AbstractSpan activeSpan();

    /**
     * 完成给定的 span, 给定的span应该是当前上下文活跃的span
     */
    boolean stopSpan(AbstractSpan span);

    /**
     * 通知此上下文，当前span将在另一个线程中异步完成
     */
    AbstractTracerContext awaitFinishAsync();
}
```

每个 `TraceSegment` 都绑定一个 `TracingContext`上下文对象，记录了 `TraceSegment` 的上下文信息。
提供的功能有：

- 管理 `TraceSegment`生命周期
- 创建`Span` 比如三个创建Span的方法`createEntrySpan`、`createLocalSpan` 方法、`createExitSpan`
- 跨进程传播上下文
- 跨线程传播上下文

#### 跨进程传播

1. 远程调用的 Client 端会调用 `inject(ContextCarrier)`方法，将当前 `TracingContext`中记录的 `Trace` 上下文信息填充到传入的`ContextCarrier` 对象。
2. 后续 Client 端的插件会将 `ContextCarrier` 对象序列化成字符串并将其作为附加信息添加到请求中，这样，`ContextCarrier` 字符串就会和请求一并到达 Server 端。
3. Server 端接收请求的插件会检查请求中是否携带了 `ContextCarrier`字符串，如果存在 `ContextCarrier` 字符串，就会将其进行反序列化，然后调用`extract()`方法从 `ContextCarrier` 对象中取出 Context 上下文信息，填充到当前 `TracingContext`（以及 `TraceSegmentRef`) 中。

#### 跨线程转播

跨线程转播，是在同一个进程中，不同的线程之间传递，这个传递过程不需要序列化，遵循以下步骤实现：

- 调用`ContextManager#capture` 方法获取`ContextSnapshot`对象
- 把这个`ContextSnapshot`对象传递给子线程
- 在子线程中调用`ContextManager#continued(ContextSnapshot snapshot)`方法

### Span 的创建

以 spring-plugins 为例，该插件会代理所有打了 `@requestMapping` 注解的方法，让其在进入对应前以及方法结束后做一些事件。

代码位置：apm-sniffer/apm-sdk-plugin/spring-plugins/mvc-annotation-commons

```java
@Override
public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
    String operationName;
    //如果为true，则完全限定的方法名将被用作端点名，而不是请求URL（默认false）
    if (SpringMVCPluginConfig.Plugin.SpringMVC.USE_QUALIFIED_NAME_AS_ENDPOINT_NAME) {
        operationName = MethodUtil.generateOperationName(method);
    } else {
        EnhanceRequireObjectCache pathMappingCache = (EnhanceRequireObjectCache) objInst.getSkyWalkingDynamicField();
        //pathMappingCache 这个属性里存放是的是 method -> url 的数据,如果没有对应的缓存,就重新获取一下,然后存入缓存里
        String requestURL = pathMappingCache.findPathMapping(method);
        if (requestURL == null) {
            requestURL = getRequestURL(method);
            pathMappingCache.addPathMapping(method, requestURL);
            requestURL = pathMappingCache.findPathMapping(method);
        }
        //以 @RequestMapping 或 @RestMapping 注解标记的入口，操作名前会加上请求方式，如 {GET}、{POST}
        operationName = getAcceptedMethodTypes(method) + requestURL;
    }
     //获取了当前请求的 request
    RequestHolder request = (RequestHolder) ContextManager.getRuntimeContext()
                                                          .get(REQUEST_KEY_IN_RUNTIME_CONTEXT);
    if (request != null) {
         //获取 stackDepth, 用来记录本次调用链的深度
        StackDepth stackDepth = (StackDepth) ContextManager.getRuntimeContext().get(CONTROLLER_METHOD_STACK_DEPTH);
        //等于null，说明是入口 span
        if (stackDepth == null) {
            //获取了跨进程的 context
            ContextCarrier contextCarrier = new ContextCarrier();
            CarrierItem next = contextCarrier.items();
            while (next.hasNext()) {
                next = next.next();
                //从header 里获取了远程调用时候传送过来的数据，放入contextCarrier                
                next.setHeadValue(request.getHeader(next.getHeadKey()));
            }
            //创建 Entryspan
            AbstractSpan span = ContextManager.createEntrySpan(operationName, contextCarrier);
            Tags.URL.set(span, request.requestURL());
            Tags.HTTP.METHOD.set(span, request.requestMethod());
            span.setComponent(ComponentsDefine.SPRING_MVC_ANNOTATION);
            SpanLayer.asHttp(span);
            //是否收集http参数信息
            if (SpringMVCPluginConfig.Plugin.SpringMVC.COLLECT_HTTP_PARAMS) {
                collectHttpParam(request, span);
            }
            //是否收集http头信息
            if (!CollectionUtil.isEmpty(SpringMVCPluginConfig.Plugin.Http.INCLUDE_HTTP_HEADERS)) {
                collectHttpHeaders(request, span);
            }
            //需要给EntrySpan创建一个新的 stackDepth
            stackDepth = new StackDepth();
            ContextManager.getRuntimeContext().put(CONTROLLER_METHOD_STACK_DEPTH, stackDepth);
        } else {
            //stackDepth不是null，说明是LocalSpan
            AbstractSpan span = ContextManager.createLocalSpan(buildOperationName(objInst, method));
            span.setComponent(ComponentsDefine.SPRING_MVC_ANNOTATION);
        }
        //深度 +1，在afterMethod 的时候会去 -1
        stackDepth.increment();
    }
}
```



### Span 数据发送

发送`span` 数据不能够阻塞业务线程，而且是等到积累了有一定的数据量，批量发送。所以 `skywalking` 使用了 `生产-消费` 的模型

#### 生产者

每当一个方法结束后，都需要调用一下 `ContextManager.stopSpan()` 方法，这个方法就是 将 `span`塞入队列的 方法

```java
private void finish() {
    if (isRunningInAsyncMode) {
        asyncFinishLock.lock();
    }
    try {
        boolean isFinishedInMainThread = activeSpanStack.isEmpty() && running;
        if (isFinishedInMainThread) {
            // 通知所有注册了的 listener，本TraceSegment成功结束了
            // 其中TraceSegmentServiceClient 的listener 收到这个事件后，会把 TraceSegment 放入队列 等待消费
            TracingThreadListenerManager.notifyFinish(this);
        }
        if (isFinishedInMainThread && (!isRunningInAsyncMode || asyncSpanCounter == 0)) {
            TraceSegment finishedSegment = segment.finish(isLimitMechanismWorking());
            TracingContext.ListenerManager.notifyFinish(finishedSegment);
            running = false;
        }
    } finally {
        if (isRunningInAsyncMode) {
            asyncFinishLock.unlock();
        }
    }
}
```

#### 消费者

agent 启动的时候会去加载模块 `apm-agent-core` 中的 `TraceSegmentServiceClient`

TraceSegmentServiceClient 中 `boot`方法

```java
@Override
public void boot() {
    lastLogTime = System.currentTimeMillis();
    segmentUplinkedCounter = 0;
    segmentAbandonedCounter = 0;
    carrier = new DataCarrier<>(CHANNEL_SIZE, BUFFER_SIZE, BufferStrategy.IF_POSSIBLE);
    //定义了消费者，Trace 的数据将会由这个消费者去发送给 skywalking,这里传了 this 消费者就是自己
    //参数2 定义了有几个消费线程，每个线程会持有不同的队列
    carrier.consume(this, 1);
}
```

TraceSegmentServiceClient 的 `consume`方法：

```java
 @Override
    public void consume(List<TraceSegment> data) {
        if (CONNECTED.equals(status)) {
            final GRPCStreamServiceStatus status = new GRPCStreamServiceStatus(false);
            StreamObserver<UpstreamSegment> upstreamSegmentStreamObserver = serviceStub.withDeadlineAfter(
                Config.Collector.GRPC_UPSTREAM_TIMEOUT, TimeUnit.SECONDS
            ).collect(new StreamObserver<Commands>() {..GRPC 的一些回调..});

            for (TraceSegment segment : data) {
           			//转换一下 segment 成 proto 数据
                    UpstreamSegment upstreamSegment = segment.transform();
                    //GRPC 发送
                    upstreamSegmentStreamObserver.onNext(upstreamSegment);
            }
			//告诉 GRPC 流已经完全写入进去了，等待他全部把数据发送后会回调上面的 StreamObserver定义的回调方法
            upstreamSegmentStreamObserver.onCompleted();

            status.wait4Finish();
            segmentUplinkedCounter += data.size();
        } else {
            segmentAbandonedCounter += data.size();
        }
    }
```

