### gRPC概念

gRPC  (Google Remote Procedure Calls)是一个高性能、开源和通用的 RPC 框架，面向服务端和移动端，基于 HTTP/2 设计

官方文档：https://grpc.io/docs/quickstart/

#### gRPC 特点

- 语言中立，支持多种语言；
- 基于 IDL 文件定义服务，通过 proto3 工具生成指定语言的数据结构、服务端接口以及客户端 Stub；
- 通信协议基于标准的 HTTP/2 设计，支持双向流、消息头压缩、单 TCP 的多路复用、服务端推送等特性，这些特性使得 gRPC 在移动端设备上更加省电和节省网络流量；
- 序列化支持 PB（Protocol Buffer）和 JSON，PB 是一种语言无关的高性能序列化框架，基于 HTTP/2 + PB, 保障了 RPC 调用的高性能。

#### Protocol Buffers

文档：https://developers.google.com/protocol-buffers/docs/overview

gRPC默认使用protocl buffers，protoc buffers 是谷歌成熟的开源的用于结构化数据序列化的机制。gRPC也可以使用其他数据格式，比如JSON

 *.proto文件定义待序列化数据的结构示例：

```protobuf
// greeter 服务定义.
service Greeter {
  // Sends a greeting
  rpc SayHello (HelloRequest) returns (HelloReply) {}
}

// 客户端请求消息包含用户名.
message HelloRequest {
  string name = 1;
}

// 服务端响应包含一条greeting消息
message HelloReply {
  string message = 1;
}
```

### Skywalking 中的 gRPC

位置：apm-protocol/apm-network，定义了 Agent 与后端 OAP 使用 gRPC 交互时的协议

#### Event 上报

```protobuf
service EventService {
  rpc collect (stream Event) returns (Commands) {
  }
}

message Event {
  string uuid = 1;
  //发生事件的源
  Source source = 2;
  //事件名称，如重启、升级
  string name = 3;
  //事件类型，Normal正常操作，Error意外操作
  Type type = 4;
  //事件的详细信息
  string message = 5;
  //事件信息的参数
  map<string, string> parameters = 6;
  int64 startTime = 7;
  int64 endTime = 8;
}
enum Type {
  Normal = 0;
  Error = 1;
}
message Source {
  string service = 1;
  string serviceInstance = 2;
  string endpoint = 3;
}
```

#### 服务最新配置发现

```protobuf
service ConfigurationDiscoveryService {
    rpc fetchConfigurations (ConfigurationSyncRequest) returns (Commands) {
    }
}

message ConfigurationSyncRequest {
	//当前agent service name
    string service = 1;
    //最后一次配置生成的签名
    string uuid = 2;
}
message Commands {
    repeated Command commands = 1;
}

message Command {
	//取决于代理的实现
    string command = 1;
    //配置的字符串值对
    repeated KeyStringValuePair args = 2;
}
```

#### JVM 指标上报

```protobuf
service JVMMetricReportService {
    rpc collect (JVMMetricCollection) returns (Commands) {
    }
}

message JVMMetricCollection {
    repeated JVMMetric metrics = 1;
    //服务名
    string service = 2;
    //服务实例名
    string serviceInstance = 3;
}

message JVMMetric {
	//生成时间戳
    int64 time = 1;
    //cpu使用百分比
    CPU cpu = 2;
    //内存使用情况
    repeated Memory memory = 3;
    //内存池情况
    repeated MemoryPool memoryPool = 4;
    //GC回收情况
    repeated GC gc = 5;
    //线程情况
    Thread thread = 6;
    //类加载情况
    Class clazz = 7;
}

message Memory {
    bool isHeap = 1;
    int64 init = 2;
    int64 max = 3;
    int64 used = 4;
    int64 committed = 5;
}

message MemoryPool {
    PoolType type = 1;
    int64 init = 2;
    int64 max = 3;
    int64 used = 4;
    int64 committed = 5;
}

enum PoolType {
    CODE_CACHE_USAGE = 0;
    NEWGEN_USAGE = 1;
    OLDGEN_USAGE = 2;
    SURVIVOR_USAGE = 3;
    PERMGEN_USAGE = 4;
    METASPACE_USAGE = 5;
}

message GC {
    GCPhrase phrase = 1;
    int64 count = 2;
    int64 time = 3;
}

enum GCPhrase {
    NEW = 0;
    OLD = 1;
}

message Thread {
    int64 liveCount = 1;
    int64 daemonCount = 2;
    int64 peakCount = 3;
    int64 runnableStateThreadCount = 4;
    int64 blockedStateThreadCount = 5;
    int64 waitingStateThreadCount = 6;
    int64 timedWaitingStateThreadCount = 7;
}
message Class {
    int64 loadedClassCount = 1;
    int64 totalUnloadedClassCount = 2;
    int64 totalLoadedClassCount = 3;
}
```

#### Trace 上报

```protobuf
service TraceSegmentReportService {
	//推荐使用的报告方式
    rpc collect (stream SegmentObject) returns (Commands) {
    }
    //第三方集成提供的，可能影响网络和客户端的性能
    rpc collectInSync (SegmentCollection) returns (Commands) {
    }
}
//span的集合
message SegmentObject {
    string traceId = 1;
    string traceSegmentId = 2;
    repeated SpanObject spans = 3;
    string service = 4;
    string serviceInstance = 5;
    bool isSizeLimited = 6;
}
//表示两个现有span之间的链接
message SegmentReference {
    //引用类型，跨线程/跨进程
    RefType refType = 1;
    string traceId = 2;
    string parentTraceSegmentId = 3;
    int32 parentSpanId = 4;
    string parentService = 5;
    string parentServiceInstance = 6;
    string parentEndpoint = 7;

    string networkAddressUsedAtPeer = 8;
}

//在实际操作中，为了避免负载开销，需要时才增加span。
//建议在跨进程(RPC/MQ的客户端/服务器)和跨线程的情况下创建span
message SpanObject {
    // span 的id，在同一个segment中唯一，从0开始
    int32 spanId = 1;
    // 在当前segment中父span的id，如果为-1表示没有父span
    int32 parentSpanId = 2;
    // span开始时间的时间戳
    int64 startTime = 3;
	// span结束时间的时    // Span 层级，可选值 Unknown、Database、RPCFramework、Http、MQ、Cache
    SpanLayer spanLayer = 9;
    int32 componentId = 10;
	// span 是否发生异常，影响后台的成功率统计
    bool isError = 11;
	// 字符串键值对，保存一些额外信息
    repeated KeyStringValuePair tags = 12;
    // 日志信息，如异常时获取的堆栈信息
    repeated Log logs = 13;
    // 是否跳过分析？
    bool skipAnalysis = 14;
}

message SegmentCollection {
    repeated SegmentObject segments = 1;
}
```

#### Log 上报

```protobuf
service LogReportService {
    rpc collect (stream LogData) returns (Commands) {
    }
}

//通过agent的file scratcher收集日志数据。
//本地，Satellite提供了多种收集日志的方式。
message LogData {
	//生成日志的时间戳，如果不传已OAP接收时间为准
    int64 timestamp = 1;
    // 服务
    string service = 2;
	// 服务实例
    string serviceInstance = 3;
    // 端点
    string endpoint = 4;
    // 日志内容
    LogDataBody body = 5;
    // trace context
    TraceContext traceContext = 6;
    // 一些可用的 tags，OAP server 基于此提供查询分析功能
    LogTags tags = 7;
}

message LogDataBody {
    string type = 1;
    oneof content {
        TextLog text = 2;
        JSONLog json = 3;
        YAMLLog yaml = 4;
    }
}

message TextLog {
    string text = 1;
}

message JSONLog {
    string json = 1;
}

message YAMLLog {
    string yaml = 1;
}

message TraceContext {
    string traceId = 1;
    string traceSegmentId = 2;
    int32 spanId = 3;
}

message LogTags {
    // String key, String value pair.
    repeated KeyStringValuePair data = 1;
}
```

#### 服务信息上报

```protobuf
service ManagementService {
    // 上报服务实例及相关属性信息
    rpc reportInstanceProperties (InstanceProperties) returns (Commands) {
    }
	//在后台保证实例存活
	//只有在不需要报告跟踪和度量时，才建议单独做keepAlive报告。
    rpc keepAlive (InstancePingPkg) returns (Commands) {

    }
}

message InstanceProperties {
    string service = 1;
    string serviceInstance = 2;
    repeated KeyStringValuePair properties = 3;
}

message InstancePingPkg {
    string service = 1;
    string serviceInstance = 2;
}
```

#### 性能剖析任务

```protobuf
service ProfileTask {
    // 获取所有的性能剖析任务
    rpc getProfileTaskCommands (ProfileTaskCommandQuery) returns (Commands) {
    }
    // 收集线程堆栈快照
    rpc collectSnapshot (stream ThreadSnapshot) returns (Commands) {
    }
    // 上报性能剖析任务已完成
    rpc reportTaskFinish (ProfileTaskFinishReport) returns (Commands) {
    }
}

message ProfileTaskCommandQuery {
    string service = 1;
    string serviceInstance = 2;
    int64 lastCommandTime = 3;
}

message ThreadSnapshot {
    string taskId = 1;
    string traceSegmentId = 2;
    int64 time = 3;
    // dump 快照的序列
    int32 sequence = 4;
    // 保存快照的 stack
    ThreadStack stack = 5;
}

message ThreadStack {
    repeated string codeSignatures = 1;
}

message ProfileTaskFinishReport {
    string service = 1;
    string serviceInstance = 2;
    // 性能剖析任务id
    string taskId = 3;
}
```

