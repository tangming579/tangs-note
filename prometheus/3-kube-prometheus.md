GitHub：https://github.com/prometheus-operator/kube-prometheus

### 包含的组件

- Prometheus Operator：以Deployment的方式运行于Kubernetes集群上

- 高可用 Prometheus：Operator会观察集群内的Prometheus CRD来创建一个合适的statefulset在

- 高可用 Alertmanager

- node-exporter：收集主机的指标数据，如CPU、内存、磁盘、网络等等多个维度的指标数据

- blackbox-exporter

- Prometheus Adapter：用于将Prometheus中的监控数据转换为Kubernetes自定义指标。使得Kubernetes可以基于这些自定义指标进行自动扩展（HPA）和其他自定义操作

- kube-state-metrics：通过监听 API Server 生成有关资源对象的状态指标，比如 Deployment、Node、Pod

  二者的主要区别如下：

  - kube-state-metrics 主要关注的是业务相关的一些元数据，比如 Deployment、Pod、副本状态等
  - metrics-server 主要关注的是[资源度量 API](https://github.com/kubernetes/community/blob/master/contributors/design-proposals/instrumentation/resource-metrics-api.md) 的实现，比如 CPU、文件描述符、内存、请求延时等指标

- Grafana

### 监控组件类型

- 监控代理程序：如node_exporter：收集主机的指标数据，如CPU、内存、磁盘、网络等等多个维度的指标数据。
- kubelet（cAdvisor）：收集容器指标数据，也是K8S的核心指标收集，每个容器的相关指标数据包括：CPU使用率、限额、文件系统读写限额、内存使用率和限额、网络报文发送、接收、丢弃速率等等。
- API Server：收集API Server的性能指标数据，包括控制队列的性能、请求速率和延迟时长等等
- etcd：收集etcd存储集群的相关指标数据
- kube-state-metrics：该组件可以派生出k8s相关的多个指标数据，主要是资源类型相关的计数器和元数据信息，包括制定类型的对象总数、资源限额、容器状态以及Pod资源标签系列等。

Prometheus 能够 直接 把 Kubernetes API Server 作为 服务发现对象。 需要特别说明的是， Pod 资源 需要 添加 下列 注解信息才能被 Prometheus 系统自动发现并抓取其内建的指标数据。

- 1） prometheus. io/ scrape： 用于 标识 是否 需要 被 采集 指标 数据， 布尔 型 值， true 或 false。
- 2） prometheus. io/ path： 抓取 指标 数据 时 使用 的 URL 路径， 一般 为/ metrics。
- 3） prometheus. io/ port： 抓取 指标 数据 时 使 用的 套 接 字 端口， 如 8080。

