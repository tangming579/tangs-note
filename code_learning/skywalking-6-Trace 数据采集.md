### 概念回顾

- spanId : 同一个线程中唯一, 从0始，按照调用链路从0递增
- parentSpanId : 在同一个`线程的链路`中，用来连接 `span`
- segmentId : 同一个 `线程链路`中，这个值都是相同的，不同线程链路中这个值不同
- traceId: 在一个 `链路` 中 `traceId` 唯一

### TracingContext 类详解

 TracingContext表示核心跟踪逻辑控制器

位置：apm-sniffer/apm-agent-core/context/TracingContext.java

```

```



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

#### 生产者

#### 消费者