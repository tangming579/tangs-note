Apache Kafka 是一个分布式流处理平台

数据流是无边界数据集的抽象表示。无边界意味着无限和持续增长。流式处理是指实时地处理一个或多个事件流。流式处理是一种编程范式，就像请求与响应范式和批处理范式那样

Kafka名词解释：

1. Producer：消息生产者，就是向 Kafka broker 发消息的客户端。

2. Consumer：消息消费者，向 Kafka broker 拉取消息的客户端。

3. Consumer Group: 消费者组，由多个 consumer 组成。消费者组内每个消费者负责消费不同分区的数据，一个分区只能由一个组内消费者消费; 消费者组之间互不影响。所有的消费者都属于某个消费者组，即消费者组是逻辑上的一个订阅者。

4. Broker：一台 Kafka 服务器就是一个 broker。一个 broker可以容纳多个 topic

5. Topic：可以理解为一个队列，生产者和消费者面向的都是一个 topic。

6. Partition：一个大的 topic 可以分布到多个 broker上，一个 topic 可以分为多个 partition，每个 partition 是一个有序的队列。
   partition中的每条消息都会被分配一个有序的id(offset)。kafka只保证按一个partition中的顺序将消息发给consumer，不保证一个topic的整体(多 个partition间)的顺序。

7. Replica：副本，为保证集群中的某个节点发生故障时，该节点上的 partition 数据不丢失，且 kafka 仍然能够继续工作，kafka 提供了副本机制，一 个 topic 的每个分区都有若干个副本，一个 leader 和若干个 follower。

8. Leader：每个分区多个副本的“主”，生产者发送数据的对象，以及消费者消费数据的对象都是 leader。

9. Follower：每个分区多个副本中的“从”，实时从 leader 中同步数据，保持和 leader 数据的同步。leader 发生故障时，某个 follower 会成为新的 follower。

10. Offset ：用来标记每个Topic已经消费到的消息位置。

    - Current Offset：保存在Consumer客户端中，它表示Consumer希望收到的下一条消息的序号
    - Committed Offset：保存在Broker上，它表示Consumer已经确认消费过的消息的序号

    

同一个partition内的消息只能被同一个组中的一个consumer消费,当消费者数量多于partition的数量时，**多余的消费者空闲**。