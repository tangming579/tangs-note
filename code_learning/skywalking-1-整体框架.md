### 1. 概要

github：https://github.com/apache/skywalking

作用：分布式链路最终系统

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

### 3. ES索引结构

