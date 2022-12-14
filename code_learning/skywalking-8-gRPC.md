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
    string command = 1;
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
    string service = 2;
    string serviceInstance = 3;
}

message JVMMetric {
    int64 time = 1;
    CPU cpu = 2;
    repeated Memory memory = 3;
    repeated MemoryPool memoryPool = 4;
    repeated GC gc = 5;
    Thread thread = 6;
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

```
service TraceSegmentReportService {
    rpc collect (stream SegmentObject) returns (Commands) {
    }
    rpc collectInSync (SegmentCollection) returns (Commands) {
    }
}

message SegmentObject {
    string traceId = 1;
    string traceSegmentId = 2;
    repeated SpanObject spans = 3;
    string service = 4;
    string serviceInstance = 5;
    bool isSizeLimited = 6;
}

message SegmentReference {
    // Represent the reference type. It could be across thread or across process.
    // Across process means there is a downstream RPC call for this.
    // Typically, refType == CrossProcess means SpanObject#spanType = entry.
    RefType refType = 1;
    // A string id represents the whole trace.
    string traceId = 2;
    // Another segment id as the parent.
    string parentTraceSegmentId = 3;
    // The span id in the parent trace segment.
    int32 parentSpanId = 4;
    // The service logic name of the parent segment.
    // If refType == CrossThread, this name is as same as the trace segment.
    string parentService = 5;
    // The service logic name instance of the parent segment.
    // If refType == CrossThread, this name is as same as the trace segment.
    string parentServiceInstance = 6;
    // The endpoint name of the parent segment.
    // **Endpoint**. A path in a service for incoming requests, such as an HTTP URI path or a gRPC service class + method signature.
    // In a trace segment, the endpoint name is the name of first entry span.
    string parentEndpoint = 7;
    // The network address, including ip/hostname and port, which is used in the client side.
    // Such as Client --> use 127.0.11.8:913 -> Server
    // then, in the reference of entry span reported by Server, the value of this field is 127.0.11.8:913.
    // This plays the important role in the SkyWalking STAM(Streaming Topology Analysis Method)
    // For more details, read https://wu-sheng.github.io/STAM/
    string networkAddressUsedAtPeer = 8;
}

// Span represents a execution unit in the system, with duration and many other attributes.
// Span could be a method, a RPC, MQ message produce or consume.
// In the practice, the span should be added when it is really necessary, to avoid payload overhead.
// We recommend to creating spans in across process(client/server of RPC/MQ) and across thread cases only.
message SpanObject {
    // The number id of the span. Should be unique in the whole segment.
    // Starting at 0.
    int32 spanId = 1;
    // The number id of the parent span in the whole segment.
    // -1 represents no parent span.
    // Also, be known as the root/first span of the segment.
    int32 parentSpanId = 2;
    // Start timestamp in milliseconds of this span,
    // measured between the current time and midnight, January 1, 1970 UTC.
    int64 startTime = 3;
    // End timestamp in milliseconds of this span,
    // measured between the current time and midnight, January 1, 1970 UTC.
    int64 endTime = 4;
    // <Optional>
    // In the across thread and across process, these references targeting the parent segments.
    // The references usually have only one element, but in batch consumer case, such as in MQ or async batch process, it could be multiple.
    repeated SegmentReference refs = 5;
    // A logic name represents this span.
    //
    // We don't recommend to include the parameter, such as HTTP request parameters, as a part of the operation, especially this is the name of the entry span.
    // All statistic for the endpoints are aggregated base on this name. Those parameters should be added in the tags if necessary.
    // If in some cases, it have to be a part of the operation name,
    // users should use the Group Parameterized Endpoints capability at the backend to get the meaningful metrics.
    // Read https://github.com/apache/skywalking/blob/master/docs/en/setup/backend/endpoint-grouping-rules.md
    string operationName = 6;
    // Remote address of the peer in RPC/MQ case.
    // This is required when spanType = Exit, as it is a part of the SkyWalking STAM(Streaming Topology Analysis Method).
    // For more details, read https://wu-sheng.github.io/STAM/
    string peer = 7;
    // Span type represents the role in the RPC context.
    SpanType spanType = 8;
    // Span layer represent the component tech stack, related to the network tech.
    SpanLayer spanLayer = 9;
    // Component id is a predefinited number id in the SkyWalking.
    // It represents the framework, tech stack used by this tracked span, such as Spring.
    // All IDs are defined in the https://github.com/apache/skywalking/blob/master/oap-server/server-bootstrap/src/main/resources/component-libraries.yml
    // Send a pull request if you want to add languages, components or mapping defintions,
    // all public components could be accepted.
    // Follow this doc for more details, https://github.com/apache/skywalking/blob/master/docs/en/guides/Component-library-settings.md
    int32 componentId = 10;
    // The status of the span. False means the tracked execution ends in the unexpected status.
    // This affects the successful rate statistic in the backend.
    // Exception or error code happened in the tracked process doesn't mean isError == true, the implementations of agent plugin and tracing SDK make the final decision.
    bool isError = 11;
    // String key, String value pair.
    // Tags provides more informance, includes parameters.
    //
    // In the OAP backend analysis, some special tag or tag combination could provide other advanced features.
    // https://github.com/apache/skywalking/blob/master/docs/en/guides/Java-Plugin-Development-Guide.md#special-span-tags
    repeated KeyStringValuePair tags = 12;
    // String key, String value pair with an accurate timestamp.
    // Logging some events happening in the context of the span duration.
    repeated Log logs = 13;
    // Force the backend don't do analysis, if the value is TRUE.
    // The backend has its own configurations to follow or override this.
    //
    // Use this mostly because the agent/SDK could know more context of the service role.
    bool skipAnalysis = 14;
}

message Log {
    // The timestamp in milliseconds of this event.,
    // measured between the current time and midnight, January 1, 1970 UTC.
    int64 time = 1;
    // String key, String value pair.
    repeated KeyStringValuePair data = 2;
}

// Map to the type of span
enum SpanType {
    // Server side of RPC. Consumer side of MQ.
    Entry = 0;
    // Client side of RPC. Producer side of MQ.
    Exit = 1;
    // A common local code execution.
    Local = 2;
}

// A ID could be represented by multiple string sections.
message ID {
    repeated string id = 1;
}

// Type of the reference
enum RefType {
    // Map to the reference targeting the segment in another OS process.
    CrossProcess = 0;
    // Map to the reference targeting the segment in the same process of the current one, just across thread.
    // This is only used when the coding language has the thread concept.
    CrossThread = 1;
}

// Map to the layer of span
enum SpanLayer {
    // Unknown layer. Could be anything.
    Unknown = 0;
    // A database layer, used in tracing the database client component.
    Database = 1;
    // A RPC layer, used in both client and server sides of RPC component.
    RPCFramework = 2;
    // HTTP is a more specific RPCFramework.
    Http = 3;
    // A MQ layer, used in both producer and consuer sides of the MQ component.
    MQ = 4;
    // A cache layer, used in tracing the cache client component.
    Cache = 5;
}

// The segment collections for trace report in batch and sync mode.
message SegmentCollection {
    repeated SegmentObject segments = 1;
}
```

#### Log 上报

```protobuf
// Report collected logs into the OAP backend
service LogReportService {
    // Recommend to report log data in a stream mode.
    // The service/instance/endpoint of the log could share the previous value if they are not set.
    // Reporting the logs of same service in the batch mode could reduce the network cost.
    rpc collect (stream LogData) returns (Commands) {
    }
}

// Log data is collected through file scratcher of agent.
// Natively, Satellite provides various ways to collect logs.
message LogData {
    // [Optional] The timestamp of the log, in millisecond.
    // If not set, OAP server would use the received timestamp as log's timestamp, or relies on the OAP server analyzer.
    int64 timestamp = 1;
    // [Required] **Service**. Represents a set/group of workloads which provide the same behaviours for incoming requests.
    //
    // The logic name represents the service. This would show as a separate node in the topology.
    // The metrics analyzed from the spans, would be aggregated for this entity as the service level.
    //
    // If this is not the first element of the streaming, use the previous not-null name as the service name.
    string service = 2;
    // [Optional] **Service Instance**. Each individual workload in the Service group is known as an instance. Like `pods` in Kubernetes, it
    // doesn't need to be a single OS process, however, if you are using instrument agents, an instance is actually a real OS process.
    //
    // The logic name represents the service instance. This would show as a separate node in the instance relationship.
    // The metrics analyzed from the spans, would be aggregated for this entity as the service instance level.
    string serviceInstance = 3;
    // [Optional] **Endpoint**. A path in a service for incoming requests, such as an HTTP URI path or a gRPC service class + method signature.
    //
    // The logic name represents the endpoint, which logs belong.
    string endpoint = 4;
    // [Required] The content of the log.
    LogDataBody body = 5;
    // [Optional] Logs with trace context
    TraceContext traceContext = 6;
    // [Optional] The available tags. OAP server could provide search/analysis capabilities based on these.
    LogTags tags = 7;
}

// The content of the log data
message LogDataBody {
    // A type to match analyzer(s) at the OAP server.
    // The data could be analyzed at the client side, but could be partial
    string type = 1;
    // Content with extendable format.
    oneof content {
        TextLog text = 2;
        JSONLog json = 3;
        YAMLLog yaml = 4;
    }
}

// Literal text log, typically requires regex or split mechanism to filter meaningful info.
message TextLog {
    string text = 1;
}

// JSON formatted log. The json field represents the string that could be formatted as a JSON object.
message JSONLog {
    string json = 1;
}

// YAML formatted log. The yaml field represents the string that could be formatted as a YAML map.
message YAMLLog {
    string yaml = 1;
}

// Logs with trace context, represent agent system has injects context(IDs) into log text.
message TraceContext {
    // [Optional] A string id represents the whole trace.
    string traceId = 1;
    // [Optional] A unique id represents this segment. Other segments could use this id to reference as a child segment.
    string traceSegmentId = 2;
    // [Optional] The number id of the span. Should be unique in the whole segment.
    // Starting at 0.
    int32 spanId = 3;
}

message LogTags {
    // String key, String value pair.
    repeated KeyStringValuePair data = 1;
}
```

#### 服务信息上报

```protobuf
// Define the service reporting the extra information of the instance.
service ManagementService {
    // Report custom properties of a service instance.
    rpc reportInstanceProperties (InstanceProperties) returns (Commands) {
    }

    // Keep the instance alive in the backend analysis.
    // Only recommend to do separate keepAlive report when no trace and metrics needs to be reported.
    // Otherwise, it is duplicated.
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

    // query all sniffer need to execute profile task commands
    rpc getProfileTaskCommands (ProfileTaskCommandQuery) returns (Commands) {
    }

    // collect dumped thread snapshot
    rpc collectSnapshot (stream ThreadSnapshot) returns (Commands) {
    }

    // report profiling task finished
    rpc reportTaskFinish (ProfileTaskFinishReport) returns (Commands) {
    }

}

message ProfileTaskCommandQuery {
    // current sniffer information
    string service = 1;
    string serviceInstance = 2;

    // last command timestamp
    int64 lastCommandTime = 3;
}

// dumped thread snapshot
message ThreadSnapshot {
    // profile task id
    string taskId = 1;
    // dumped segment id
    string traceSegmentId = 2;
    // dump timestamp
    int64 time = 3;
    // snapshot dump sequence, start with zero
    int32 sequence = 4;
    // snapshot stack
    ThreadStack stack = 5;
}

message ThreadStack {
    // stack code signature list
    repeated string codeSignatures = 1;
}

// profile task finished report
message ProfileTaskFinishReport {
    // current sniffer information
    string service = 1;
    string serviceInstance = 2;

    // profile task
    string taskId = 3;
}
```

