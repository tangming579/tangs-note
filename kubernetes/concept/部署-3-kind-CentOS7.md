官网：https://kind.sigs.k8s.io/

Kind 是 Kubernetes in Docker 的简写，是一个使用 Docker 容器作为 Node 节点，在本地创建和运行Kubernetes 集群的工具。适用于在本机创建 Kubernetes 集群环境进行开发和测试。使用 Kind 搭建的集群无法在生产中使用

Kind 内部也是使用 Kubeadm 创建和启动集群节点，并使用 Containerd 作为容器运行时，所以弃用 dockershim对 Kind 没有什么影响。

Kind 将 Docker 容器作为 Kubernetes 的 Node 节点，并在该 Node 中安装 Kubernetes组件，包括一个或者多个 Control Plane 和一个或者多个 Work Nodes。这就解决了在本机运行多个 Node 的问题，而不需要虚拟化。
