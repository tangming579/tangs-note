## 概念

`Kubernetes` 中每个Node上都部署着一个以 `DaemonSet` 形式运行的 `kube-proxy`，当一个Pod需要访问service时，kube-proxy会根据service的定义将请求转发到正确的Pod上。

`kube-proxy` 有三种模式

- `userspace`：它在用户空间监听一个端口，所有服务通过iptables转发到这个端口，然后在其内部负载均衡到实际的Pod。该方式最主要的问题是效率低，有明显的性能瓶颈。
- `iptables`：完全以 iptables 规则的方式来实现 service 负载均衡。服务多的时候产生太多的 iptables 规则，非增量式更新会引入一定的时延，大规模情况下有明显的性能问题。
-  `IPVS`：为解决iptables模式的性能问题，采用增量式更新，并可以保证 service 更新期间连接保持不断开

### **Iptables**

kube-proxy 通过 Service 的 Informer 感知到API Server中service和endpoint的变化情况。而作为对这个事件的响应，它就会在宿主机上创建这样一条 iptables 规则（你可以通过 iptables-save 看到它）。这些规则捕获到service的clusterIP和port的流量，并将这些流量随机重定向到service后端Pod。对于每个endpoint对象，它生成选择后端Pod的iptables规则。

iptables 是一个 Linux 内核功能，是一个高效的防火墙，并提供了大量的数据包处理和过滤方面的能力。它可以在核心数据包处理管线上用 Hook 挂接一系列的规则。iptables 模式中 kube-proxy 在 NAT pre-routing Hook 中实现它的 NAT 和负载均衡功能。这种方法简单有效，依赖于成熟的内核功能，并且能够和其它跟 iptables 协作的应用融洽相处。

因为它纯粹是为防火墙而设计且基于内核规则列表，kube-proxy 使用的是一种 O(n) 算法，其中的 n 随集群规模同步增长，所以这里的集群规模越大，更明确的说就是服务和后端 Pod 的数量越大，查询的时间就会越长。

一个例子是，在5000节点集群中使用 NodePort 服务，如果我们有2000个服务并且每个服务有10个 pod，这将在每个工作节点上至少产生20000个 iptable 记录，这会使内核非常繁忙。

### ipvs

在 IPVS 模式下，kube-proxy监视Kubernetes服务和端点，调用 netlink 接口创建 IPVS 规则， 并定期将 IPVS 规则与 Kubernetes 服务和端点同步。访问服务时，IPVS 将流量定向到后端Pod之一。

IPVS代理模式基于类似于 iptables 模式的 netfilter 挂钩函数， 但是使用哈希表作为基础数据结构，并且在内核空间中工作。这意味着，与 iptables 模式下的 kube-proxy 相比，IPVS 模式下的 kube-proxy 重定向通信的延迟要短，并且在同步代理规则时具有更好的性能。与其他代理模式相比，IPVS 模式还支持更高的网络流量吞吐量。

IPVS 模式的工作原理，其实跟 iptables 模式类似。当我们创建了前面的 Service 之后，kube-proxy 首先会在宿主机上创建一个虚拟网卡（叫作：kube-ipvs0），并为它分配 Service VIP 作为 IP 地址。接下来，kube-proxy 就会通过 Linux 的 IPVS 模块，为这个 IP 地址设置三个 IPVS 虚拟主机，并设置这三个虚拟主机之间使用轮询模式 (rr) 来作为负载均衡策略
