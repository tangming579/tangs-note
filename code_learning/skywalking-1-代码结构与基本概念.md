## Skywalking-1-框架与概念

### 1. 概要

github：https://github.com/apache/skywalking

官方文档：https://skywalking.apache.org/docs/main/next/readme/

### 2. 项目代码

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

#### 源代码调试

1. 先决条件：安装 git、JDK8+、Maven 3.6+

2. 使用 git clone 命令下载源码

   ```sh
   git clone https://github.com/apache/skywalking.git
   cd skywalking/
   git checkout v8.7.0
   git submodule init
   git submodule update
   ```

3. 删除 apm-webapp项目 pom 下的打包plugin

4. 全局搜索删除所有 pom 中的 checkstyle 引用

5. 打包

   ```sh
   #只处理指定部分的源码 通过-P参数指定：
   mvn clean package -Pagent //只处理 javaAgent部分，这在调试Agent的时候就减少许多时间
   mvn package -Pbackend,dist//只处理OapServer并打包压缩
   mvn package -Pui,dist//只处理UI并打包压缩
   mvn package -Pagent,dist//只处理 javaAgent 并打包压缩
   
   #处理全部源码
   mvn package 
   ```

6. 设置 Genenated Sources Root

   右键文件夹 -> Mark Directory as -> Genenated Sources Root

   - `oap-server\server-configuration\grpc-configuration-sync\target\generated-sources\protobuf` 目录下，的 [grpc-java]  和  [java] 
   - `apm-protocol\apm-network\target\generated-sources\protobuf` 目录下，的 [grpc-java] 和  [java] 
   - `oap-server\server-core\target\generated-sources\protobuf` 目录下，的 [grpc-java] 和  [java] 
   - `oap-server/oal-grammar/target/generated-sources/antlr4` 目录下，的 [antlr4]

7. 修改 server-bootstrap 的 application.yml 配置，如集群模式、Elasticsearch信息

8. 找到 OAPServerStartUp，直接启动

### 3. 基本概念

#### skywalking 核心功能

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
  - EntrySpan：入口span，当请求进入服务时创建的span，它也是segment中的第一个
  - LocalSpan：在本地方法调用时创建的span，不与远程服务交互
  - ExitSpan：出口span，当请求离开当前服务，进入其他服务时创建的span
- reference：表示跨线程、进程父子关系，Reference包含上游的trace ID, segment ID, span ID, service name, service instance name, endpoint name，和客户端的目标地址 （跨线程场景中没有该字段）. reference中的这些字段是通过[Cross Process Propagation Headers Protocol v3](https://skywalking.apache.org/docs/main/next/en/protocols/skywalking-cross-process-propagation-headers-protocol-v3/) 在agent与agent之间传递的。

#### Span 结构

在 Elasticsearch 中，span信息存储在 sw_segment 索引中，结构如下：

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

使用GraphQL调用 queryTrace 接口返回值的 span 结构：

```
traceId：本次调用的唯一id，通过snowflake模式生成
segmentId：数据本身的唯一id，类似于主键，和trace_id是同一个函数生成的
spanId：跨度id，当前segment唯一
endpointName：被调用的接口
serviceCode：被调用的服务名
serviceInstanceName：被调用的服务实例名
startTime：开始时间戳
endTime：结束时间戳
dataBinary：里面保存了本次调用的所有Span的数据，序列化并用Base64编码，不会进行分析和用于查询
isError：是否失败
component：组件类型，如Tomcat、HttpClient等，可以在 ComponentsDefine 中看到所有定义
layer：当前span所处的位置，Spanlayer是枚举值，可选项有Unknown、Database、RPCFramework、Http、MQ、Cache
peer：请求的目标地址，ExitSpan才有。参考：https://wu-sheng.github.io/STAM/README-cn
tags：keyValue形式，接口的请求url、请求方式，使用@Trace注解时Tag信息
log：span中包含的日志，可包含多条
type：span类型，EntrySpan、LocalSpan、ExitSpan
refs：跨线程或跨进程时，span关联的TraceSegment
	tranceSegmentId：父TraceSegmentId
	spanId：父Span的Id
	type：即 SegmentRefType，枚举类型，可选值CROSS_PROCESS、CROSS_THREAD，分别表示跨进程调用和跨线程调用
```

#### Span传递方式

SkyWalking 跨进程传播协议是用于上下文的传播，8.0+的skywalking使用版本是**3.0**，也被称为为`sw8`协议

Header 项：

- Header名称：`sw8`.
- Header值：由`-`分隔的8个字段组成，每个字段使用Base64编码。Header值的长度应该小于2KB。

Header值中具体包含以下8个字段：

- 采样（Sample），0 或 1，0 表示上下文存在, 但是可以（也很可能）被忽略；1 表示这个追踪需要采样并发送到后端。
- 追踪ID（Trace Id），是 BASE64 编码的字符串，其内容是由 . 分割的三个 long 类型值, 表示此追踪的唯一标识。
- 父追踪片段ID（Parent trace segment Id），是 BASE64 编码的字符串，其内容是字符串且全局唯一。
- 父跨度ID（Parent span Id），是一个从 0 开始的整数，这个跨度ID指向父追踪片段（segment）中的父跨度（span）。
- 父服务名称（Parent service），是 BASE64 编码的字符串，其内容是一个长度小于或等于50个UTF-8编码的字符串。
- 父服务实例标识（Parent service instance），是 BASE64 编码的字符串，其内容是一个长度小于或等于50个UTF-8编码的字符串。
- 父服务的端点（Parent endpoint），是 BASE64 编码的字符串，其内容是父追踪片段（segment）中第一个入口跨度（span）的操作名，由长度小于或等于50个UTF-8编码的字符组成。
- 本请求的目标地址（Peer），是 BASE64 编码的字符串，其内容是客户端用于访问目标服务的网络地址（不一定是 IP + 端口）。

#### 监测指标

- Apdex：全称是Application Performance Index，是由Apdex联盟开发的用于评估应用性能的工业标准。Apdex标准从用户的角度出发，将对应用响应时间的表现，转为用户对于应用性能的可量化范围为0-1的满意度评价。
- cpm： 全称 call per minutes，是吞吐量(Throughput)指标。
- SLA：全称 Service-Level Agreement，直译为 “服务等级协议”，用来表示提供服务的水平
- Response Time：表示请求响应时间，对于人来说，响应时间最好不要超过2秒，超过就会觉得卡顿。对于系统接口交互来说，时间自然越短越好，500ms以内是比较好的。
- percentile：表示采集样本中某些值的占比，Skywalking 有 “p50、p75、p90、p95、p99” 一些列值。

Global

| 图表                       | 对应指标          | 说明                                                         |
| -------------------------- | ----------------- | ------------------------------------------------------------ |
| Services Load              | service_cpm       | 对于HTTP 1/2、gRPC、RPC服务，这意味着每分钟请求数（CPM），对于TCP服务，这意味着每分钟包数（PPM） |
| Slow Services              | service_resp_time | 慢响应服务，单位ms                                           |
| Un-Health Services (Apdex) | service_apdex     | Apdex性能指标，1为满分。                                     |
| Slow Endpoints             | endpoint_avg      | 慢响应端口，单位ms                                           |
| Global Response Latency    | all_percentile    | 百分比响应延时，不同百分比的延时时间，单位ms                 |
| Global Heatmap             | all_heatmap       | 服务响应时间热力分布图，根据时间段内不同响应时间的数量显示颜色深度 |

Service

| 图表                             | 对应指标           | 说明                                                         |
| -------------------------------- | ------------------ | ------------------------------------------------------------ |
| Service Apdex                    | service_apdex      | 当前服务的评分                                               |
| Successful Rate                  | service_sla        | 请求成功率                                                   |
| Service Load                     | service_cpm        | 对于HTTP 1/2、gRPC、RPC服务，这意味着每分钟请求数（CPM），对于TCP服务，这意味着每分钟包数（PPM） |
| Service Avg Response Time        | service_resp_time  | 平均响应延时，单位 ms                                        |
| Service Response Time Percentile | service_percentile | 请求响应时间百分比；举例：15：18的时候，有99%的请求在20ms以内，有95%的请求在10ms以内 |

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

### 4. 数据存储结构

Skywalking AOP服务端采用模块化开放方式，在Storage模块，支持多种数据库存储，通过Selector配置来确定选择哪种存储方式,不配置的情况下默认H2

官方推荐使用Es内存数据库作为存储，如果要支持Oracle, Mysql需要加载特定的包到服务器。

#### 链路相关索引

| 索引名称                                 | 说明                             |
| ---------------------------------------- | -------------------------------- |
| sw_service_traffic                       | 注册的服务信息                   |
| sw_endpoint_traffic                      | 注册的服务端点信息               |
| sw_instance_traffic                      | 注册的服务实例信息               |
| sw_service_relation_server_side          | 服务在服务端检测到的调用关系     |
| sw_service_relation_client_side          | 服务在客户端检测到的调用关系     |
| sw_service_instance_relation_server_side | 服务实例在服务端检测到的调用关系 |
| sw_service_instance_relation_client_side | 服务实例在客户端检测到的调用关系 |
| sw_segment                               | 链路追踪信息                     |

#### 指标类索引

| 索引名称              | 说明                                                    |
| --------------------- | ------------------------------------------------------- |
| sw_metrics-apdex      | 服务apdex分值信息                                       |
| sw_metrics-count      | 记录服务事件                                            |
| sw_metrics-cpm        | 服务、端点每分钟请求数（cpm）                           |
| sw_metrics-doubleavg  | jvm实例cpu占用率                                        |
| sw_metrics-histogram  | 热力图                                                  |
| sw_metrics-longavg    | 端点平均耗时、端点响应时间、jvm实例 memory_heap使用内存 |
| sw_metrics-max        | 最大值统计                                              |
| sw_metrics-percent    | 服务、端点的请求成功率（sla）                           |
| sw_metrics-percentile | 服务、端点百分位数                                      |
| sw_metrics-rate       | 比率                                                    |
| sw_metrics-sum        | jvm实例 YoungGC 和 OldGC 耗时、次数                     |

#### 性能剖析

| 索引名称                         | 说明                     |
| -------------------------------- | ------------------------ |
| sw_profile_task                  | 性能剖析任务             |
| sw_profile_task_log              | 性能剖析任务记录         |
| sw_profile_task_segment_snapshot | 性能剖析任务链路信息快照 |

#### 其他索引

| 索引名称                                 | 说明                             |
| ---------------------------------------- | -------------------------------- |
| sw_events                                | 服务事件                         |
