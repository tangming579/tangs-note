## 概念

K8s 中有很多方便扩展的 Interface，包括 CNI, CSI, CRI 等，将这些接口抽象出来，是为了更好的提供开放、扩展、规范等能力。

CNI (Container Network Interface, 容器网络接口) 是 CoreOS 提出的一种容器网络规范，负责实现 Pod 网络通信，解决核心问题包括：

- Pod 间跨节点通信
- Pod 与 Service 网络互通
- 网络策略（NetworkPolicy）实施

### 使用方法

1. 首先在每个结点上配置 CNI 配置文件(/etc/cni/net.d/xxnet.conf)，其中 xxnet.conf 是某一个网络配置文件的名称；
2. 安装 CNI 配置文件中所对应的二进制插件；
3. 在这个节点上创建 Pod 之后，Kubelet 就会根据 CNI 配置文件执行前两步所安装的 CNI 插件；
4. 上步执行完之后，Pod 的网络就配置完成了。

<div>
    <image src="./img/cni.png"></image>
</div>

### 通信方式

网络栈包括：**网卡（Network Interface）、回环设备（Loopback Device）、路由表（Routing Table）和 iptables 规则**。

把每一个容器看做一台主机，它们都有一套独立的“网络栈”。如果想要实现两台主机之间的通信，最直接的办法，就是把它们用一根网线连接起来；而如果想要实现多台主机之间的通信，那就需要用网线，把它们连接在一台交换机上。

在 Linux 中，能够起到虚拟交换机作用的网络设备，是网桥（Bridge）。它是一个工作在数据链路层（Data Link）的设备，主要功能是根据 MAC 地址学习来将数据包转发到网桥的不同端口（Port）上。

Docker 项目会默认在宿主机上创建一个名叫 docker0 的网桥，凡是连接在 docker0 网桥上的容器，就可以通过它来进行通信。而容器“连接”到 docker0 网桥上，需要使用一种名叫 Veth Pair 的虚拟设备。

网络插件真正要做的事情，则是通过某种方法，把不同宿主机上的特殊设备连通，从而达到容器跨主机通信的目的。

Kubernetes 是通过一个叫作 CNI 的接口，维护了一个单独的网桥来代替 docker0。这个网桥的名字就叫作：CNI 网桥，它在宿主机上的设备名称默认是：cni0

- **网络命名空间**

  Calico将每个容器或虚拟机放置在一个独立的网络命名空间中，这样可以避免不同容器之间的网络冲突。每个命名空间都有自己的IP地址和路由表。

- **BGP路由**

  Calico使用BGP（边界网关协议）来路由数据包。BGP是一种标准的路由协议，用于在自治系统之间交换路由信息。通过BGP，Calico能够在主机之间动态地学习和传播路由信息，确保跨主机的容器网络通信。

- **Felix代理**

  在每个计算节点上运行着一个名为Felix的轻量级代理。Felix负责监听ECTD（可能是指etcd，一个分布式键值存储系统）中心的存储，从中获取事件，例如用户在该节点上添加了一个IP地址或分配了一个容器等。基于这些信息，Felix会在该节点上创建容器，并配置相应的网络设置，如网卡、IP地址和MAC地址。

- **IPTables规则**

  由于TCP/IP协议栈提供了一整套的防火墙规则，因此Calico可以通过IPTables规则实现复杂的网络隔离逻辑。这意味着管理员可以定义精细的网络策略，控制容器之间的通信和数据流。

- **服务发现和负载均衡**

  Calico与Kubernetes的Service API集成，为服务提供自动的服务发现和负载均衡功能。当服务的容器分布在多个节点上时，Calico会负责将流量正确地分发到各个容器，确保服务的可用性和性能。

### 通信流程

#### 1. 容器到容器（同一Pod内）通信流程

- 在Kubernetes中，默认情况下，同一Pod内的所有容器共享相同的网络命名空间，这意味着它们可以通过localhost直接通信。
- 不涉及物理网卡和网桥，因为它们都在同一个虚拟网络环境里，可以通过进程间通信（IPC）或者网络套接字在同一网络命名空间内直接交流。

#### 2. pod之间的通信（以Calico为例）

- 当创建Pod时，Calico CNI插件会为Pod分配一个全局唯一IP地址，并将其添加到Calico创建的BGP网络中。
- Pod的数据包通过veth pair虚拟网卡进行传输，每个Pod都有一个veth对，一端连接到Pod的网络命名空间，另一端连接到主机上的Calico网桥（比如cali+随机字符串）。
- 主机上的Calico节点代理（bird/bird6）将Pod的IP地址及其所在主机的信息通过BGP协议传播到集群内的其他节点。
- 当一个Pod需要与另一个Pod通信时，数据包首先通过其veth对发送到Calico网桥，然后由Calico节点代理根据BGP路由表进行转发。
  如果目标Pod在本地节点，则直接通过内核路由到目标Pod的veth对，进而进入目标Pod的网络命名空间。
- 如果目标Pod在远程节点，则数据包通过主机的物理网卡（如eth0）发送到数据中心网络，通过交换机到达目标节点的物理网卡，然后再通过该节点上的Calico网络栈将数据包路由到目标Pod。
  

参考：

https://blog.csdn.net/xixihahalelehehe/article/details/119485267

https://blog.csdn.net/xixihahalelehehe/article/details/119535258

https://blog.csdn.net/lhq1363511234/article/details/138045261

## CNI 插件模式

<div>
    <image src="./img/cni2.png"></image>
</div>

常用 CNI 插件及工作原理
