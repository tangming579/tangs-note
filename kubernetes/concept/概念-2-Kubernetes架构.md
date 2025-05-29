# Kubernetes架构

## Nodes

Kubernetes 通过将容器放入在节点（Node）上运行的 Pod 中来执行你的工作负载。 节点可以是一个虚拟机或者物理机器，取决于所在的集群配置。 每个节点包含运行 Pods 所需的服务； 这些节点由 control-plane 负责管理。

节点上的组件包括 kubelet、container-runtimes 以及 kube-proxy。

### 管理

向 API 服务器 添加节点的方式主要有两种：

1. 节点上的 `kubelet` 向 control plane 执行自注册；
2. 手动添加一个 Node 对象。

> Kubernetes 会一直保存着非法节点对应的对象，并持续检查该节点是否已经变得健康。 必须显式地删除该 Node 对象以停止健康检查操作。

**Node 对象的名称约束**

- 不能超过 253 个字符
- 只能包含小写字母、数字，以及 '-' 和 '.'
- 必须以字母数字开头
- 必须以字母数字结尾

### 节点自注册

当 kubelet 标志 `--register-node` 为 true（默认）时，它会尝试向 API 服务注册自己。 这是首选模式。

自注册模式，kubelet 使用下列参数启动：

- `--kubeconfig` - 身份认证所用的凭据的路径。
- `--cloud-provider` - 与云驱动通信。
- `--register-node` - 自动向 API 服务注册。
- `--register-with-taints` - 使用所给的[污点](https://kubernetes.io/zh/docs/concepts/scheduling-eviction/taint-and-toleration/)列表。当 `register-node` 为 false 时无效。
- `--node-ip` - 节点 IP 地址。
- `--node-labels` - 在集群中注册节点时要添加的 Labels
- `--node-status-update-frequency` - 指定 kubelet 向控制面发送状态的频率。

> 当 Node 的配置需要被更新时， 一种好的做法是重新向 API 服务器注册该节点。

### 手动节点管理

设置 kubelet 标志 `--register-node=false`。

可以结合使用 Node 上的标签和 Pod 上的选择算符来控制调度

一个节点的状态包含以下信息:

- 地址（Addresses）：用法取决于你的云服务商或者物理机配置
- 状况（Condition)：字段描述了所有 `Running` 节点的状况
- 容量与可分配（Capacity）：描述节点上的可用资源：CPU、内存和可以调度到节点上的 Pod 的个数上限
- 信息（Info）： 指的是节点的一般信息，如内核版本、Kubernetes 版本等

可以使用 `kubectl` 来查看节点状态和其他细节信息：

```shell
kubectl describe node <节点名称>
```

| 节点状况             | 描述                                                         |
| -------------------- | ------------------------------------------------------------ |
| `Ready`              | 如节点是健康的并已经准备好接收 Pod 则为 `True`；`False` 表示节点不健康而且不能接收 Pod；`Unknown` 表示节点控制器在最近 `node-monitor-grace-period` 期间（默认 40 秒）没有收到节点的消息 |
| `DiskPressure`       | `True` 表示节点存在磁盘空间压力，即磁盘可用量低, 否则为 `False` |
| `MemoryPressure`     | `True` 表示节点存在内存压力，即节点内存可用量低，否则为 `False` |
| `PIDPressure`        | `True` 表示节点存在进程压力，即节点上进程过多；否则为 `False` |
| `NetworkUnavailable` | `True` 表示节点网络配置不正确；否则为 `False`                |

### 心跳 

Kubernetes 节点发送的心跳帮助集群确定每个节点的可用性，并在检测到故障时采取行动。

对于 Nodes 节点，有两种形式的心跳:

- 更新节点的 `.status`
- `kube-node-lease` namespaces 中的 Lease（租约）对象。 每个节点都有一个关联的 Lease 对象。

与 Node 的 `.status` 更新相比，Lease 是一种轻量级资源。 使用 Lease 来表达心跳在大型集群中可以减少这些更新对性能的影响。

kubelet 负责创建和更新节点的 `.status`，以及更新它们对应的 Lease。

### 节点控制器

节点控制器在节点的生命周期中扮演多个角色：

1. 当节点注册时为它分配一个 CIDR 区段（如果启用了 CIDR 分配）。
2. 保持节点控制器内的节点列表与云服务商所提供的可用机器列表同步
3. 监控节点的健康状况

### 节点拓扑

如果启用了 TopologyManager 特性门控， kubelet 可以在作出资源分配决策时使用拓扑提示。 

## 控制面到节点通信

### 节点到控制面

Kubernetes 采用的是中心辐射型（Hub-and-Spoke）API 模式。 所有从集群（或所运行的 Pods）发出的 API 调用都终止于 API 服务器。 其它控制面组件都没有被设计为可暴露远程服务。

### 控制面到节点

从控制面（API 服务器）到节点有两种主要的通信路径：

1. 从 API 服务器到集群中每个节点上运行的 kubelet 进程，作用：
   - 获取 Pod 日志
   - 挂接（通过 kubectl）到运行中的 Pod
   - 提供 kubelet 的端口转发功能。 
2. 从 API 服务器通过它的代理功能连接到任何节点、Pod 或者服务。
   - 连接默认为纯 HTTP 方式，因此既没有认证，也没有加密

## 控制器 Controller

在 Kubernetes 中，控制器通过监控集群 的公共状态，并致力于将当前状态转变为期望的状态

**控制器模式**

`spec` 字段代表期望状态。 该资源的控制器负责确保其当前状态接近期望状态。

- 通过 API 服务器来控制：Job 控制器是一个 Kubernetes 内置控制器的例子。 内置控制器通过和集群 API 服务器交互来管理状态。
- 直接控制：相比 Job 控制器，有些控制器需要对集群外的一些东西进行修改。

Kubernetes 内置一组控制器，运行在 kube-controller-manager 内。 这些内置的控制器提供了重要的核心功能。

Deployment 控制器和 Job 控制器是 Kubernetes 内置控制器的典型例子。

## 垃圾收集器

垃圾收集是 Kubernetes 用于清理集群资源的各种机制的统称。 垃圾收集允许系统清理如下资源：

- 失败的 Pod
- 已完成的 Job
- 不再存在属主引用的对象
- 未使用的容器和容器镜像
- 动态制备的、StorageClass 回收策略为 Delete 的 PV 卷
- 阻滞或者过期的 CertificateSigningRequest (CSRs)
- 在以下情形中删除了的节点对象：
  - 当集群使用云控制器管理器运行于云端时；
  - 当集群使用类似于云控制器管理器的插件运行在本地环境中时。

- 节点租约对象

**属主与依赖**

Kubernetes 中很多对象通过属主引用 链接到彼此。属主引用（Owner Reference）可以告诉控制面哪些对象依赖于其他对象

**级联删除**

Kubernetes 会检查并删除那些不再拥有属主引用的对象，例如删除了 ReplicaSet 之后留下来的 Pod。可以控制 Kubernetes 是否去自动删除该对象的依赖对象， 这个过程称为 **级联删除（Cascading Deletion）**

- 前台级联删除：当属主对象进入删除过程中状态后，控制器删除其依赖对象。控制器在删除完所有依赖对象之后， 删除属主对象
- 后台级联删除：Kubernetes 服务器立即删除属主对象，控制器在后台清理所有依赖对象。 默认情况下，Kubernetes 使用后台级联删除方案

## 容器运行时接口（CRI）

CRI 是一个插件接口，它使 kubelet 能够使用各种容器运行时，无需重新编译集群组件。

CRI 是 kubelet 和容器运行时之间通信的主要协议（主要使用gRPC）。