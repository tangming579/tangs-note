### 1. 概要

github：https://github.com/apache/skywalking

官方文档：https://skywalking.apache.org/docs/main/next/readme/

作用：分布式链路追踪系统

### 2. 项目结构

#### 主服务结构

```
skywalking

|-- apm-protocol # grpc协议相关
|-- docker #生成docker镜像
|-- docs # 官方项目文档
|-- oap-server # OAP服务，所有核心逻辑都在该包下面
|   |-- analyzer # 数据解析模块
|   |-- exporter # 数据导出模块，经mereics数据从OAP再次导出到第三方分析平台
|   |-- microbench # 接口，方法性能测试模块，使用OpenJDK的JMK对接口或者方法进行性能测试
|   |-- oal-grammar # OAL语法定义
|   |-- oal-rt # AOL引擎相关
|   |-- oap-server.iml
|   |-- pom.xml
|   |-- server-alarm-plugin # OAP告警模块
|   |-- server-cluster-plugin # OAP集群配置，默认单机
|   |-- server-configuration # OAP动态配置模块，默认静态配置
|   |-- server-core # 服务核心
|   |-- server-fetcher-plugin # 信息抓取，主要针对普罗米修斯，kafka等第三方插件
|   |-- server-health-checker # 服务健康检查
|   |-- server-library # OAP的一些依赖模块
|   |-- server-query-plugin # OAP查询模块
|   |-- server-receiver-plugin # OAP接受Agent、zabbix等信息的插件
|   |-- server-starter # OAP启动模块
|   |-- server-storage-plugin # OAP存储模块，application.yml中不配置默认H2
|   |-- server-telemetry # OAP遥测模块，基于OpenTelemetry
|   |-- server-testing # 测试
|   |-- server-tools # 一些工具
|-- oap-server-bom
`-- tools # 工具
```

#### Agent 结构

```
|-- apm-application-toolkit # 工具包，提供 日志打印ID、跨线程传递TID等功能
|   |-- apm-application-toolkit.iml
|   |-- apm-toolkit-kafka
|   |-- apm-toolkit-log4j-1.x
|   |-- apm-toolkit-log4j-2.x
|   |-- apm-toolkit-logback-1.x
|   |-- apm-toolkit-meter
|   |-- apm-toolkit-micrometer-registry
|   |-- apm-toolkit-opentracing
|   |-- apm-toolkit-trace
|-- apm-commons # 公共包，提供工具，生产者消费者的封装
|-- apm-protocol # grpc通信协议相关
|-- apm-sniffer # Skywalking探针相关
|   |-- apm-agent # java Agent premain方法所在的包，Agent核心加载逻辑所在
|   |-- apm-agent-core # 探针核心包，Agent服务所在的类
|   |-- apm-sdk-plugin # Agent插件定义包
|   |-- apm-test-tools # 测试工具
|   |-- apm-toolkit-activation # 需要激活使用的工具
|   |-- bootstrap-plugins # JDK相关的插件
|   |-- java-agent-sniffer.iml
|   |-- optional-plugins # 可选择插件，默认不生效
|   |-- optional-reporter-plugins
|-- skywalking-agent # 打包后生成的Agent jar位置所在
|   |-- activations
|   |-- bootstrap-plugins
|   |-- plugins
|   `-- skywalking-agent.jar
```

### 4. 基本概念

#### 核心功能

- APM指标分析
- 服务拓扑图分析
- 服务、服务实例和端点依赖性分析
- 检测到慢速服务和端点(metrics)
- 性能剖析
- 链路追踪(trace)
- 数据库访问指标。检测慢速数据库访问语句（包括 SQL 语句）
- 告警
- 日志(log)

#### Open Tracing协议

Open Tracing 是一套分布式追踪协议，与平台和语言无关，具有统一的接口规范，方便接入不同的分布式追踪系统。

Skywalking 中 Trace 的相关概念：

- Trace：描述一个分布式系统中请求从接收到处理完成的完整调用链路。

- Segment：是skywalking独有的一个概念，在一次调用里所经历的一个线程生成一个Segment，与Trace是一对多关系。
- Span：一个请求在某个进程/线程里的逻辑内的轨迹片段，与Segment是一对多关系，共三种类型span： 
  - EntrySpan：入口span，http服务、rpc服务、MQ消费者
  - LocalSpan：不与远程服务交互的span
  - ExitSpan：出口span，各种clientSpan，比如httpclient的请求
- reference：表示跨线程、进程父子关系，Reference包含上游的trace ID, segment ID, span ID, service name, service instance name, endpoint name，和客户端的目标地址 （跨线程场景中没有该字段）. reference中的这些字段是通过[Cross Process Propagation Headers Protocol v3](https://skywalking.apache.org/docs/main/next/en/protocols/skywalking-cross-process-propagation-headers-protocol-v3/) 在agent与agent之间传递的。

#### Span结构

```
trace_id：本次调用的唯一id，通过snowflake模式生成
endpoint_name：被调用的接口
latency：耗时
end_time：结束时间戳
endpoint_id：被调用的接口的唯一id
service_instance_id：被调用的实例的唯一id
version：本数据结构的版本号
start_time：开始时间戳
data_binary：里面保存了本次调用的所有Span的数据，序列化并用Base64编码，不会进行分析和用于查询
service_id：服务的唯一id
time_bucket：调用所处的时段
is_error：是否失败
segment_id：数据本身的唯一id，类似于主键，通过snowflake模式生成
```

#### 监测指标

- Apdex：全称是Application Performance Index，是由Apdex联盟开发的用于评估应用性能的工业标准。Apdex标准从用户的角度出发，将对应用响应时间的表现，转为用户对于应用性能的可量化范围为0-1的满意度评价。
- cpm： 全称 call per minutes，是吞吐量(Throughput)指标。下图是拼接的全局、服务、实例和接口的吞吐量及平均吞吐量。
  第一条185cpm=185/60=3.08个请求/秒。
- SLA：全称 Service-Level Agreement，直译为 “服务等级协议”，用来表示提供服务的水平
- Response Time：表示请求响应时间，对于人来说，响应时间最好不要超过2秒，超过就会觉得卡顿。对于系统接口交互来说，时间自然越短越好，500ms以内是比较好的。
- percentile：表示采集样本中某些值的占比，Skywalking 有 “p50、p75、p90、p95、p99” 一些列值。

Global

```
Services load：对于HTTP 1/2、gRPC、RPC服务，这意味着每分钟请求数（CPM），对于TCP服务，这意味着每分钟包数（PPM）
Slow Services：慢响应服务，单位ms
Un-Health Services (Apdex)：Apdex性能指标，1为满分。
Global Response Latency：百分比响应延时，不同百分比的延时时间，单位ms
Global Heatmap：服务响应时间热力分布图，根据时间段内不同响应时间的数量显示颜色深度
```

Service

```
Service Apdex（总分）：当前服务的评分
Service Avg Response Times：平均响应延时，单位 ms
Successful Rate（数字）：请求成功率
Services Load （数字）：对于HTTP 1/2、gRPC、RPC服务，这意味着每分钟请求数（CPM），对于TCP服务，这意味着每分钟包数（PPM）
Service Apdex（百分比）：当前服务的评分
Service Response Time Percentile：请求响应时间百分比；举例：15：18的时候，有99%的请求在20ms以内，有95%的请求在10ms以内…
Successful Rate（百分比）：请求成功率
Service Load（折线图）：对于HTTP 1/2、gRPC、RPC服务，这意味着每分钟请求数（CPM），对于TCP服务，这意味着每分钟包数（PPM）

```

Instance

```
Service Instances load：当前实例每分钟请求数
Service Instances Successful Rate：当前实例的请求成功率
Service Instances Latency ：当前实例的响应延迟
JVM CPU：jvm占用CPU百分比
JVM Memory：jvm内存占用大小，单位m
JVM GC Time：jvm垃圾回收时间，包含YGC和OGC
JVM GC Count：jvm垃圾回收次数，包含YGC和OGC
JVM Thread Count：JVM线程计数
– instance_jvm_thread_live_count 实例jvm线程活动计数
– instance_jvm_thread_daemon_count 实例
– jvm线程守护进程计数，
– instance_jvm_thread_peak_count 实例jvm线程峰值计数
JVM Thread State Count：JVM线程状态计数
– instance jvm thread runnable state thread count 实例jvm线程可运行状态线程计数
– instance jvm thread blocked state thread count 实例jvm线程阻塞状态线程计数
– instance jvm thread waiting state thread count 实例jvm线程等待状态线程计数
– instance jvm thread timed waiting state thread count 实例jvm线程定时等待状态线程计数
JVM Class Count：JVM类计数
– instance jvm class loaded class count 实例jvm类加载类计数
– instance jvm class total unloaded class count 实例jvm类总卸载类计数
– instance jvm class total loaded class count 实例jvm类总加载类计数
```

Endpoint

```
Endpoint Load in Current Service：对于HTTP 1/2、gRPC、RPC服务，这意味着每分钟请求数（CPM），对于TCP服务，这意味着每分钟包数（PPM）
Slow Endpoints in Current Service：当前服务中的慢速终结点
Successful Rate in Current Service： 当前服务中的成功率
Endpoint load：端口每分钟请求数
Endpoint Avg Response Time：端口评价响应耗时，单位ms
Endpoint Response Time Percentile：端口请求响应时间百分比
Endpoint Successful Rate：请求成功率
```

### 5. 存储结构

Skywalking AOP服务端采用模块化开放方式，在Storage模块，支持多种数据库存储，通过Selector配置来确定选择哪种存储方式,不配置的情况下默认H2

官方推荐使用Es内存数据库作为存储，如果要支持Oracle, Mysql需要加载特定的包到服务器。

#### JVM 相关

|                                  |      |      |
| -------------------------------- | ---- | ---- |
| instance_jvm_cpu                 |      |      |
| instance_jvm_memory_heap         |      |      |
| instance_jvm_memory_heap_max     |      |      |
| instance_jvm_memory_noheap       |      |      |
| instance_jvm_memory_noheap_max   |      |      |
| instance_jvm_old_gc_count        |      |      |
| instance_jvm_old_gc_time         |      |      |
| instance_jvm_thread_daemon_count |      |      |
| instance_jvm_thread_live_count   |      |      |
| instance_jvm_thread_peak_count   |      |      |
| instance_jvm_young_gc_count      |      |      |
| instance_jvm_young_gc_time       |      |      |



#### APM全局指标（7个索引）

#### 服务之间调用指标（8个索引）

#### 服务实例之间的调用指标（11个索引）

#### 端点指标（8个索引）

| 索引名                        |                                      |                   |
| ----------------------------- | ------------------------------------ | ----------------- |
| endpoint_avg                  |                                      |                   |
| endpoint_cpm                  | 端点每分钟请求调用的次数             |                   |
| endpoint_percentile           | 端点采样占比                         |                   |
| endpoint_relation_cpm         | 端点每分钟请求调用的次数             |                   |
| endpoint_relation_percentile  | 端点采样占比关系                     |                   |
| endpoint_relation_resp_time   | 端点与响应时间关系                   |                   |
| endpoint_relation_server_side | 端点头和目标关系                     |                   |
| endpoint_relation_sla         | 端点等级协议关系                     |                   |
| endpoint_sla                  | 端点等级协议，用来表示提供服务的水平 |                   |
| endpoint_traffic              | 端点信息，主要为服务id、端点名称     | {GET}/api/user/v1 |



#### 数据库性能指标（4个索引）

#### 其他索引（19）