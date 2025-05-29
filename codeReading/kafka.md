Apache Kafka 是一个分布式流处理平台

数据流是无边界数据集的抽象表示。无边界意味着无限和持续增长。流式处理是指实时地处理一个或多个事件流。流式处理是一种编程范式，就像请求与响应范式和批处理范式那样

### Kafka 名词解释

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

### 多副本机制

1. Kafka 通过给特定 Topic 指定多个 Partition, 而各个 Partition 可以分布在不同的 Broker 上, 这样便能提供比较好的并发能力
2. Partition 可以指定对应的 Replica 数, 这也极大地提高了消息存储的安全性, 提高了容灾能力，不过也相应的增加了所需要的存储空间
3. 多个副本之间会有一个 leader ，其他副本称为 follower。我们发送的消息会被发送到 leader 副本，然后 follower 副本才能从 leader 副本中拉取消息进行同步

### Zookeeper 作用

1. **Broker 注册** ：在 Zookeeper 上会有一个专门**用来进行 Broker 服务器列表记录**的节点。每个 Broker 在启动时，都会到 Zookeeper 上进行注册，即到 `/brokers/ids` 下创建属于自己的节点。每个 Broker 就会将自己的 IP 地址和端口等信息记录到该节点中去
2. **Topic 注册** ： 在 Kafka 中，同一个**Topic 的消息会被分成多个分区**并将其分布在多个 Broker 上，**这些分区信息及与 Broker 的对应关系**也都是由 Zookeeper 在维护。比如我创建了一个名字为 my-topic 的主题并且它有两个分区，对应到 zookeeper 中会创建这些文件夹：`/brokers/topics/my-topic/Partitions/0`、`/brokers/topics/my-topic/Partitions/1`
3. **负载均衡** ：上面也说过了 Kafka 通过给特定 Topic 指定多个 Partition, 而各个 Partition 可以分布在不同的 Broker 上, 这样便能提供比较好的并发能力。 对于同一个 Topic 的不同 Partition，Kafka 会尽力将这些 Partition 分布到不同的 Broker 服务器上。当生产者产生消息后也会尽量投递到不同 Broker 的 Partition 里面。当 Consumer 消费的时候，Zookeeper 可以根据当前的 Partition 数量以及 Consumer 数量来实现动态负载均衡。

### 保证消息的消费顺序

Kafka 中发送 1 条消息的时候，可以指定 topic, partition, key,data（数据） 4 个参数。如果你发送消息的时候指定了 Partition 的话，所有消息都会被发送到指定的 Partition。并且，同一个 key 的消息可以保证只发送到同一个 partition，这个我们可以采用表/对象的 id 来作为 key 。

### 保证消息不丢失

- 生产者

  1. 添加回调函数，检查失败原因重新发送
  2. retries （重试次数）设置大一些，出现网络问题能够自动重试消息发送，避免消息丢失

- 消费者：手动提交 offset

- Kafka

  1. **acks = all**：leader 副本所在的 broker 突然挂掉，没来得及同步到 follower（延迟会很高）

  2. **replication.factor >= 3**：增加副本数量

  3. **min.insync.replicas > 1**：消息至少要被写入到 2 个副本才算是被成功发送

     （推荐 **replication.factor = min.insync.replicas + 1**）

### 保证消息不重复消费

- 消费消息服务做幂等校验，比如 Redis 的set、MySQL 的主键等天然的幂等功能。

- 将`enable.auto.commit`参数设置为 false，关闭自动提交，开发者在代码中手动提交 offset。那么这里会有个问题：

  什么时候提交offset合适？

  - 处理完消息再提交：依旧有消息重复消费的风险，和自动提交一样
  - 拉取到消息即提交：会有消息丢失的风险。允许消息延时的场景，一般会采用这种方式。然后，通过定时任务在业务不繁忙（比如凌晨）的时候做数据兜底。