# IPTables

`iptables` 是 Linux 系统中自带的开源包过滤工具，它允许用户设置、维护和检查网络流量的过滤规则。

现在 redhat 系统默认使用`firewalld`，`ubuntu`默认使用`ufw`防火墙，但在容器环境，Docker 默认直接管理 iptables 规则来实现网络隔离和端口映射。

ipatbles中的基本概念（4表5链）

1. **表（Tables）**：`iptables` 中的规则被分为不同的表，每个表有不同的处理目标。常见的表包括：
   - **filter**：默认表，用于处理网络流量的过滤。
   - **nat**：用于网络地址转换，处理源地址和目标地址的转换。
   - **mangle**：用于修改数据包的某些属性，如 TOS（服务类型）字段。
   - **raw**：用于处理数据包的原始数据，通常用于调试目的。
2. **链（Chains）**：每个表中包含多个链，每条链包含一组规则。常见的链包括：
   - **INPUT**：处理进入本地系统的数据包。
   - **FORWARD**：处理经过本地系统转发的数据包。
   - **OUTPUT**：处理从本地系统发出的数据包。
   - **PREROUTING**：在路由决策之前处理数据包，用于 NAT 和 Mangle 表。
   - **POSTROUTING**：在路由决策之后处理数据包，用于 NAT 和 Mangle 表。
3. **规则（Rules）**：每条规则定义了如何处理匹配特定条件的数据包。规则包括匹配条件和相应的动作（如 ACCEPT、DROP、REJECT 等）。

## netfilter 与 iptables 的关系

- netfilter：Linux 内核中的包过滤框架（内核态）
- iptables：用户空间的防火墙管理工具（用户态）

我们日常使用 `iptables` 命令（如配置端口转发、流量拦截），本质是在 “通过工具操作内核的 netfilter 模块

## 常见应用场景

| 应用类型      | 使用的表 | 使用的链                |
| :------------ | :------- | :---------------------- |
| 防火墙过滤    | filter   | INPUT, FORWARD, OUTPUT  |
| 地址转换(NAT) | nat      | PREROUTING, POSTROUTING |
| 数据包修改    | mangle   | 全部五链                |
| 关闭连接跟踪  | raw      | PREROUTING, OUTPUT      |

# IPVS

（IP Virtual Server）是专门为高性能场景设计的流量分发工具。它的特点是在于用哈希表存储规则，查找效率高，而且还能玩出更多花样，比如不同的负载均衡算法。

`kube-proxy`支持 iptables 和 ipvs 两种模式，ipvs 和 iptables 都是基于`netfilter`的，那么 ipvs 模式和 iptables 模式之间有哪些差异呢？

- ipvs 为大型集群提供了更好的可扩展性和性能
- ipvs 支持比 iptables 更复杂的复制均衡算法（最小负载、最少连接、加权等等）
- ipvs 支持服务器健康检查和连接重试等功能

ipvs 依赖 iptables

ipvs 会使用 iptables 进行包过滤、SNAT、masquared(伪装)。具体来说，ipvs 将使用`ipset`来存储需要`DROP`或`masquared`的流量的源或目标地址，以确保 iptables 规则的数量是恒定的，这样我们就不需要关心我们有多少服务了

# Kube-proxy

在 IPVS 模式下，kube-proxy监视Kubernetes服务和端点，调用 netlink 接口创建 IPVS 规则， 并定期将 IPVS 规则与 Kubernetes 服务和端点同步。访问服务时，IPVS 将流量定向到后端Pod之一。IPVS代理模式基于类似于 iptables 模式的 netfilter 挂钩函数， 但是使用哈希表作为基础数据结构，并且在内核空间中工作。这意味着，与 iptables 模式下的 kube-proxy 相比，IPVS 模式下的 kube-proxy 重定向通信的延迟要短，并且在同步代理规则时具有更好的性能。与其他代理模式相比，IPVS 模式还支持更高的网络流量吞吐量。

 IPVS 模式的工作原理，跟 iptables 模式类似。当我们创建了前面的 Service 之后，kube-proxy 首先会在宿主机上创建一个虚拟网卡（叫作：kube-ipvs0），并为它分配 Service VIP 作为 IP 地址。接下来，kube-proxy 就会通过 Linux 的 IPVS 模块，为这个 IP 地址设置三个 IPVS 虚拟主机，并设置这三个虚拟主机之间使用轮询模式 (rr) 来作为负载均衡策略。

