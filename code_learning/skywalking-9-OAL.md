参考：https://skywalking.apache.org/docs/main/next/en/concepts-and-designs/oal/

### 概念

OAL（Observability Analysis Language）观测分析语言，Skywalking定义的一套高级语法，用于聚焦 service、serviceInstance、endPoint 的度量指标。

语法分析使用的 Antlr4 框架，主要是要定义.g4文件用于描述词法分析和语法解析

- 对应的.g4文件位置：oap-server/oal-grammar

- 文件：OALLexer.g4 和 OALParser.g4

可以理解成oal定义了一套高级查询语法，主要目的是对于agent端上传的服务、服务实例、端点等数据，通过聚合、过滤等操作，形成对应的度量指标。

### 过程

1. 定义core.oal文件，内容即oal语法，例如：
   service_success = from(Service.*).filter(status == true).count();
2. Skywalking启动时拉起oal运行时，通过读取core.oal文件，得到各oal指标定义
3. 基于Antlr4框架解析core.oal语法，得到OALScripts对象，该对象中包含了一个AnalysisResult的集合，即将core.oal文件中的每行定义转换成了一个AnalysisResult对象。
4. 基于每个AnalysisResult对象，生成动态运行时代码。
5. 基于动态生成的指标对象(ServiceSuccessMetrics)和指标存储对象(ServiceSuccessMetricsBuilder)创建worker对象，并添加到对应的容器中，用于后续操作
6. 动态生成dispatcher分发器对象，用于分发各类源数据
7. oap收到grpc数据，根据dispacher进行分发，然后通过各worker进行数据流操作后得到指标数据，进行存储和告警规则匹配。

### 语法

OAL 脚本文件应该以 `.oal` 为后缀。

```
// 声明一个指标
METRICS_NAME = from(SCOPE.(* | [FIELD][,FIELD ...])) // 从某一个SCOPE中获取数据
[.filter(FIELD OP [INT | STRING])] // 可以过滤掉部分数据
.FUNCTION([PARAM][, PARAM ...]) // 使用某个聚合函数将数据聚合

// 禁用一个指标
disable(METRICS_NAME);
```

#### Filter

使用在使用过滤器的时候，通过指定字段名或表达式来构建字段值的过滤条件。

表达式可以使用 `and`，`or` 和 `()` 进行组合。 操作符包含`==`，`!=`，`>`，`<`，`>=`，`<=`，`in [...]`，`like %...`，`like ...%`，`like %...%`，他们可以基于字段类型进行类型检测, 如果类型不兼容会在编译/代码生成期间报错。

#### Aggregation Function

默认的聚合函数由 SkyWalking OAP 核心实现。并可自由扩展更多函数。

提供的函数：

- `longAvg`：某个域实体所有输入的平均值，输入字段必须是 `long` 类型。

```javascript
instance_jvm_memory_max = from(ServiceInstanceJVMMemory.max).longAvg();
```

在上面的例子中，输入是 `ServiceInstanceJVMMemory` 域的每个请求，平均值是基于字段 `max` 进行求值的。

- `doubleAvg`：某个域实体的所有输入的平均值，输入的字段必须是 `double` 类型。

```javascript
instance_jvm_cpu = from(ServiceInstanceJVMCPU.usePercent).doubleAvg();
```

在上面的例子中，输入是 `ServiceInstanceJVMCPU` 域的每个请求，平均值是基于 `usePercent` 字段进行求值的。

- `percent`：对于输入中匹配指定条件的百分比数.

```javascript
endpoint_percent = from(Endpoint.*).percent(status == true);
```

在上面的例子中，输入是每个端点的请求，条件是 `endpoint.status == true`。

- `rate`：对于条件匹配的输入，比率以100的分数表示。

```javascript
browser_app_error_rate = from(BrowserAppTraffic.*).rate(trafficCategory == BrowserAppTrafficCategory.FIRST_ERROR, trafficCategory == BrowserAppTrafficCategory.NORMAL);
```

在上面的例子中，所有的输入都是每个浏览器应用流量的请求， 分子的条件是`trafficCategory == BrowserAppTrafficCategory.FIRST_ERROR`， 分母的条件是`trafficCategory == BrowserAppTrafficCategory.NORMAL`。 其中，第一个参数是分子的条件，第二个参数是分母的条件。

- `sum`：某个域实体的调用总数。

```javascript
service_calls_sum = from(Service.*).sum();
```

在上面的例子中，统计每个服务的调用数。

- `histogram`：热力图，更多详见Heatmap in WIKI。

```javascript
all_heatmap = from(All.latency).histogram(100, 20);
```

在上面的例子中，计算了所有传入请求的热力学热图。 第一个参数是计算延迟的精度，在上面的例子中，在101-200ms组中，113ms和193ms被认为是相同的. 第二个参数是分组数量，在上面的例子中，一共有21组数据分别为0-100ms，101-200ms……1901-2000ms，2000ms以上.

- `apdex`：应用性能指数(Application Performance Index)，更多详见Apdex in WIKI。

```javascript
service_apdex = from(Service.latency).apdex(name, status);
```

在上面的例子中，计算了所有服务的应用性能指数。 第一个参数是服务名称，该名称的Apdex阈值在配置文件`service-apdex-threshold.yml`中定义。 第二个参数是请求状态，状态(成功或失败)影响Apdex的计算。

- `P99`，`P95`，`P90`，`P75`，`P50`：百分位，更多详见Percentile in WIKI。

**百分位**是自7.0版本引入的第一个多值度量。由于有多个值，可以通过`getMultipleLinearIntValues`GraphQL查询进行查询。

```javascript
all_percentile = from(All.latency).percentile(10);
```

在上面的例子中，计算了所有传入请求的 `P99`，`P95`，`P90`，`P75`，`P50`。参数是百分位计算的精度，在上例中120ms和124被认为是相同的。

#### Metrics Name

存储实现，告警以及查询模块的度量指标名称，SkyWalking 内核支持自动类型推断。

#### Group

所有度量指标数据都会使用 `Scope.ID` 和最小时间桶(min-level time bucket) 进行分组.

- 在端点的域中，Scope.ID 为端点的 ID（基于服务及其端点的唯一标志）。

#### Disable

`Disable`是OAL中的高级语句，只在特定情况下使用。 一些聚合和度量是通过核心硬代码定义的，这个`Disable`语句是设计用来让它们停止活动的， 比如`segment`, `top_n_database_statement`。 在默认情况下，没有被禁用的。

#### 示例

```javascript
// 计算每个端点的响应平均时长
endpoint_avg = from(Endpoint.latency).avg()

// 计算每个端点 p50，p75，p90，p95 and p99 的延迟柱状图，每隔 50 毫秒一条柱
endpoint_percentile = from(Endpoint.latency).percentile(10)

// 统计每个服务响应状态为 true 的百分比
endpoint_success = from(Endpoint.*).filter(status == true).percent()

// 计算每个服务的响应码为[404, 500, 503]的总和
endpoint_abnormal = from(Endpoint.*).filter(responseCode in [404, 500, 503]).sum()

// 计算每个服务的请求类型为[PRC, gRPC]的总和
endpoint_rpc_calls_sum = from(Endpoint.*).filter(type in [RequestType.PRC, RequestType.gRPC]).sum()

// 计算每个端点的端点名称为["/v1", "/v2"]的总和
endpoint_url_sum = from(Endpoint.*).filter(endpointName in ["/v1", "/v2"]).sum()

// 统计每个服务的调用总量
endpoint_calls = from(Endpoint.*).sum()

disable(segment);
disable(endpoint_relation_server_side);
disable(top_n_database_statement);
```

### Skywalking 中的OAL文件

#### core.oal

```
/ All scope metrics
all_percentile = from(All.latency).percentile(10);  // Multiple values including p50, p75, p90, p95, p99
all_heatmap = from(All.latency).histogram(100, 20); //

// Service scope metrics 服务
service_resp_time = from(Service.latency).longAvg(); // 服务的平均响应时间
service_sla = from(Service.*).percent(status == true); // 服务的请求成功率
service_cpm = from(Service.*).cpm(); //服务的每分钟调用次数
service_percentile = from(Service.latency).percentile(10); // Multiple values including p50, p75, p90, p95, p99
service_apdex = from(Service.latency).apdex(name, status); // 服务的应用性能指标，apdex的衡量的是衡量满意的响应时间与不满意的响应时间的比率，默认的请求满意时间是500ms

// Service relation scope metrics for topology 服务与服务间调用的调用度量指标
service_relation_client_cpm = from(ServiceRelation.*).filter(detectPoint == DetectPoint.CLIENT).cpm();//在客户端检测到的每分钟调用次数
service_relation_server_cpm = from(ServiceRelation.*).filter(detectPoint == DetectPoint.SERVER).cpm();//在服务端检测到的每分钟调用的次数
service_relation_client_call_sla = from(ServiceRelation.*).filter(detectPoint == DetectPoint.CLIENT).percent(status == true);//在客户端检测到成功率
service_relation_server_call_sla = from(ServiceRelation.*).filter(detectPoint == DetectPoint.SERVER).percent(status == true);//在服务端检测到的成功率
service_relation_client_resp_time = from(ServiceRelation.latency).filter(detectPoint == DetectPoint.CLIENT).longAvg();//在客户端检测到的平均响应时间
service_relation_server_resp_time = from(ServiceRelation.latency).filter(detectPoint == DetectPoint.SERVER).longAvg();//在服务端检测到的平均响应时间
service_relation_client_percentile = from(ServiceRelation.latency).filter(detectPoint == DetectPoint.CLIENT).percentile(10); // Multiple values including p50, p75, p90, p95, p99
service_relation_server_percentile = from(ServiceRelation.latency).filter(detectPoint == DetectPoint.SERVER).percentile(10); // Multiple values including p50, p75, p90, p95, p99

// Service Instance relation scope metrics for topology 服务实例与服务实例之间的调用度量指标
service_instance_relation_client_cpm = from(ServiceInstanceRelation.*).filter(detectPoint == DetectPoint.CLIENT).cpm();//在客户端实例检测到的每分钟调用次数
service_instance_relation_server_cpm = from(ServiceInstanceRelation.*).filter(detectPoint == DetectPoint.SERVER).cpm();//在服务端实例检测到的每分钟调用次数
service_instance_relation_client_call_sla = from(ServiceInstanceRelation.*).filter(detectPoint == DetectPoint.CLIENT).percent(status == true);//在客户端实例检测到的成功率
service_instance_relation_server_call_sla = from(ServiceInstanceRelation.*).filter(detectPoint == DetectPoint.SERVER).percent(status == true);//在服务端实例检测到的成功率
service_instance_relation_client_resp_time = from(ServiceInstanceRelation.latency).filter(detectPoint == DetectPoint.CLIENT).longAvg();//在客户端实例检测到的平均响应时间
service_instance_relation_server_resp_time = from(ServiceInstanceRelation.latency).filter(detectPoint == DetectPoint.SERVER).longAvg();//在服务端实例检测到的平均响应时间
service_instance_relation_client_percentile = from(ServiceInstanceRelation.latency).filter(detectPoint == DetectPoint.CLIENT).percentile(10); // Multiple values including p50, p75, p90, p95, p99
service_instance_relation_server_percentile = from(ServiceInstanceRelation.latency).filter(detectPoint == DetectPoint.SERVER).percentile(10); // Multiple values including p50, p75, p90, p95, p99

// Service Instance Scope metrics
service_instance_sla = from(ServiceInstance.*).percent(status == true);//服务实例的成功率
service_instance_resp_time= from(ServiceInstance.latency).longAvg();//服务实例的平均响应时间
service_instance_cpm = from(ServiceInstance.*).cpm();//服务实例的每分钟调用次数

// Endpoint scope metrics
endpoint_cpm = from(Endpoint.*).cpm();//端点的每分钟调用次数
endpoint_avg = from(Endpoint.latency).longAvg();//端口平均响应时间
endpoint_sla = from(Endpoint.*).percent(status == true);//端点的成功率
endpoint_percentile = from(Endpoint.latency).percentile(10); // Multiple values including p50, p75, p90, p95, p99

// Endpoint relation scope metrics
endpoint_relation_cpm = from(EndpointRelation.*).filter(detectPoint == DetectPoint.SERVER).cpm();//在服务端端点检测到的每分钟调用次数
endpoint_relation_resp_time = from(EndpointRelation.rpcLatency).filter(detectPoint == DetectPoint.SERVER).longAvg();//在服务端检测到的rpc调用的平均耗时
endpoint_relation_sla = from(EndpointRelation.*).filter(detectPoint == DetectPoint.SERVER).percent(status == true);//在服务端检测到的请求成功率
endpoint_relation_percentile = from(EndpointRelation.rpcLatency).filter(detectPoint == DetectPoint.SERVER).percentile(10); // Multiple values including p50, p75, p90, p95, p99

database_access_resp_time = from(DatabaseAccess.latency).longAvg();//数据库的处理平均响应时间
database_access_sla = from(DatabaseAccess.*).percent(status == true);//数据库的请求成功率
database_access_cpm = from(DatabaseAccess.*).cpm();//数据库的每分钟调用次数
database_access_percentile = from(DatabaseAccess.latency).percentile(10);
```

#### java-agent.oal

```
// JVM instance metrics
instance_jvm_cpu = from(ServiceInstanceJVMCPU.usePercent).doubleAvg();//jvm 平均cpu耗时百分比
instance_jvm_memory_heap = from(ServiceInstanceJVMMemory.used).filter(heapStatus == true).longAvg();//jvm 堆空间的平均使用空间
instance_jvm_memory_noheap = from(ServiceInstanceJVMMemory.used).filter(heapStatus == false).longAvg();//jvm 非堆空间的平均使用空间
instance_jvm_memory_heap_max = from(ServiceInstanceJVMMemory.max).filter(heapStatus == true).longAvg();//jvm 最大堆内存的平均值
instance_jvm_memory_noheap_max = from(ServiceInstanceJVMMemory.max).filter(heapStatus == false).longAvg();//jvm 最大非堆内存的平均值
instance_jvm_young_gc_time = from(ServiceInstanceJVMGC.time).filter(phrase == GCPhrase.NEW).sum();//年轻代gc的耗时
instance_jvm_old_gc_time = from(ServiceInstanceJVMGC.time).filter(phrase == GCPhrase.OLD).sum();//老年代gc的耗时
instance_jvm_young_gc_count = from(ServiceInstanceJVMGC.count).filter(phrase == GCPhrase.NEW).sum();//年轻代gc的次数
instance_jvm_old_gc_count = from(ServiceInstanceJVMGC.count).filter(phrase == GCPhrase.OLD).sum();//老年代gc的次数
instance_jvm_thread_live_count = from(ServiceInstanceJVMThread.liveCount).longAvg();//存活的线程数
instance_jvm_thread_daemon_count = from(ServiceInstanceJVMThread.daemonCount).longAvg();//守护线程数
instance_jvm_thread_peak_count = from(ServiceInstanceJVMThread.peakCount).longAvg();//峰值线程数
```