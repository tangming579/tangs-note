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

proto文件定义位置：apm-protocol/apm-network

主要接口

