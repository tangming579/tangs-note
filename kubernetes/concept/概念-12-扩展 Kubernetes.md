定制化的方法主要可分为 *配置（Configuration）* 和 *扩展（Extensions）* 两种。 前者主要涉及改变参数标志、本地配置文件或者 API 资源； 后者则需要额外运行一些程序或服务。 本文主要关注扩展。

## Operator 模式

Kubernetes 的 Operator 模式概念允许在不修改 Kubernetes 自身代码的情况下，通过为一个或多个自定义资源关联控制器 来扩展集群的能力。 Operator 是 Kubernetes API 的客户端，充当自定义资源 的控制器

使用 Operator 可以自动化的事情包括：

- 按需部署应用
- 获取/还原应用状态的备份
- 处理应用代码的升级以及相关改动。例如，数据库 schema 或额外的配置设置
- 发布一个 service，要求不支持 Kubernetes API 的应用也能发现它
- 模拟整个或部分集群中的故障以测试其稳定性
- 在没有内部成员选举程序的情况下，为分布式应用选择首领角色

Operator 是由 CoreOS 开发的，用来扩展 Kubernetes API，特定的应用程序控制器，它用来创建、配置和管理复杂的有状态应用，如数据库、缓存和监控系统。Operator 基于 Kubernetes 的资源和控制器概念之上构建，但同时又包含了应用程序特定的领域知识。创建Operator 的关键是CRD（自定义资源）的设计。

Operator的逻辑是先创建一个crd资源，再创建一个控制器.

### CRD & CR

在 Kubernetes 中我们使用的 Deployment， DamenSet，StatefulSet, Service，Ingress, ConfigMap, Secret 这些都是资源，而对这些资源的创建、更新、删除的动作都会被称为为事件(Event)，Kubernetes 的 Controller Manager 负责事件监听，并触发相应的动作来满足期望（Spec），这种方式也就是声明式，即用户只需要关心应用程序的最终状态。当我们在使用中发现现有的这些资源不能满足我们的需求的时候，Kubernetes 提供了自定义资源（Custom Resource）和 opertor 为应用程序提供基于 kuberntes 扩展。

CRD 则是对自定义资源的描述(Custom Resource Definition)，也就是介绍这个资源有什么属性，这些属性的类型是什么，结构是怎样的。

```yaml
apiVersion: apiextensions.k8s.io/v1beta1
kind: CustomResourceDefinition
metadata:
  name: postgresqls.acid
  labels:
    app.kubernetes.io/name: postgres-operator
  annotations:
    "helm.sh/hook": crd-install
spec:
  group: acid
  names:
    kind: postgresql
    listKind: postgresqlList
    plural: postgresqls
    singular: postgresql
...
```

CRD 是通知 Kubernetes 平台存在一种新的资源，CR 则是该 CRD 定义的具体的实例对象。

### Operator SDK

operator SDK —— operator framework，是 CoreOS 公司开发和维护的用于快速创建 operator 的工具，可以帮助我们快速构建 operator 应用，类似的工具还有：

- KUDO (Kubernetes 通用声明式 Operator)
- kubebuilder，kubernetes SIG 在维护的一个项目
- Metacontroller，可与 Webhook 结合使用，以实现自己的功能。

### Kubebuilder

和 Operator SDK 一样其实都是对 Controller Runtime（Kubernetes SIG 官方封装和抽象的开发 Operator 的公共库） 的封装，Operator-SDK 是 CoreOS 出品，Kubebuilder 则是 Kubernetes-SIG 官方团队原生打造

## 网络插件

Kubernetes 1.24 支持容器网络接口 (CNI) 集群网络插件。 你必须使用和你的集群相兼容并且满足你的需求的 CNI 插件。 在更广泛的 Kubernetes 生态系统中你可以使用不同的插件（开源和闭源）。