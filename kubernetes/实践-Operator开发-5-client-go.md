## 整体流程

<div>
    <image src="./template/informer.png"></image>
</div>

### Informer 端

- Reflector：Reflector 从 apiserver 监听特定类型的资源，对比resourceVersion，拿到变更通知后，将其放到 DeltaFIFO 队列中。
- Informer：Informer 会不断地从 DeltaFIFO 中读取对象
  - 通知 Indexer：根据对象创建或更新本地的缓存，也就是 store
  - 通知 controller：controller 会调用事先注册的 ResourceEventHandler 回调函数进行处理。
- Indexer：Indexer 中有 informer 维护的指定资源对象的相对于etcd数据的一份本地内存缓存，可通过该缓存获取资源对象，以减少对apiserver、对etcd的请求压力；

### Controller 端

- Resource Event Handlers：用于添加一些过滤条件，判断哪些对象需要加到 WorkQueue 中进一步处理
- WorkQueue：WorkQueue 一般使用的是延时队列实现
- Worker：我们自己业务代码的处理过程
  - 收到 WorkQueue 中的任务
  - 通过 Indexer 从本地缓存检索对象
  - 通过 ClientSet 实现对象的增删改查

## Informer

client-go 中提供了几种不同的 Informer：

- 通过调用 NewInformer 函数创建一个简单的不带 indexer 的 Informer。
- 通过调用 NewIndexerInformer 函数创建一个简单的带 indexer 的 Informer。
- 通过调用 NewSharedIndexInformer 函数创建一个 Shared 的 Informer。
- 通过调用 NewDynamicSharedInformerFactory 函数创建一个为 Dynamic 客户端的 Shared 的 Informer。



### Informer如何保证数据一致性

list 和 watch 一起保证了消息的可靠性，避免因消息丢失而造成状态不一致场景。具体而言，list API可以查询当前的资源及其对应的状态(即期望的状态)，客户端通过拿期望的状态和实际的状态进行对比，纠正状态不一致的资源。Watch API 和 apiserver保持一个长链接，接收资源的状态变更事件并做相应处理。如果仅调用 watch API，若某个时间点连接中断，就有可能导致消息丢失，所以需要通过list API解决消息丢失的问题。从另一个角度出发，我们可以认为list API获取全量数据，watch API获取增量数据。虽然仅仅通过轮询 list API，也能达到同步资源状态的效果，但是存在开销大，实时性不足的问题。