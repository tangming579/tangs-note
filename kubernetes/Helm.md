## helm 是什么

helm 是 kubernetes 生态系统中的一个软件包管理工具，类似 Ubuntu 的 apt, CentOS 的 yum 或 python 的 pip 一样，专门负责管理 kubernetes 应用资源；使用 helm 可以对 kubernetes 应用进行统一打包、分发、安装、升级以及回退等操作。

helm 利用Chart来封装 kubernetes 原生应用程序的一些列 yaml 文件，可以在部署应用的时候自定义应用程序的一些 Metadata ，以便于应用程序的分发。

### helm 为什么出现

利用Kubernetes部署一个应用，需要Kubernetes原生资源文件如 deployment、replicationcontroller、service 或 pod 等。这些 k8s 资源过于分散，不方便进行管理，直接通过 kubectl 来管理一个应用，非常的不方便。

helm主要作用：

- 应用程序封装
- 版本管理
- 依赖检查
- 便于应用程序分发

### Helm 架构

Helm中有三个重要概念，分别为Chart、Repository和Release。

- Chart：是一个Helm包。它包含在K8s集群内部运行应用程序，工具或服务所需的所有资源定义。可以类比成yum中的RPM。

- 仓库（Repository）：用来存放和共享Chart的地方，可以类比成Maven仓库。

- 发布（Release）：运行在K8s集群中的Chart的实例，一个Chart可以在同一个集群中安装多次。Chart就像流水线中初始化好的模板，Release就是这个“模板”所生产出来的各个产品。

Helm作为K8s的包管理软件，每次安装Charts 到K8s集群时，都会创建一个新的 release。你可以在Helm 的Repository中寻找需要的Chart。Helm对于部署过程的优化的点在于简化了原先完成配置文件编写后还需使用一串kubectl命令进行的操作、统一管理了部署时的可配置项以及方便了部署完成后的升级和维护。

## Helm 安装

### 二进制安装

helm 二进制包安装，helm 只是一个单纯的可执行程序

下载最新 release：https://github.com/helm/helm

```sh
tar -zxvf helm-v3.10.3-linux-amd64.tar.gz
cp -av linux-amd64/helm /usr/bin/
```

常用命令

```
helm create 创建一个Helm Chart初始安装包工程
helm search 在Helm仓库中查找应用
helm install 安装Helm
helm list 罗列K8s集群中的部署的Release列表
helm lint 对一个Helm Chart进行语法检查和校验
```

### 配置Helm仓库

- 微软仓库 http://mirror.azure.cn/kubernetes/charts/
- 阿里云仓库 https://kubernetes.oss-cn-hangzhou.aliyuncs.com/charts
- 官方仓库 https://hub.kubeapps.com/charts/incubator

```shell
# 添加仓库
[root@k8smaster ~]# helm repo add  aliyun https://kubernetes.oss-cn-hangzhou.aliyuncs.com/charts
"aliyun" has been added to your repositories

[root@k8smaster ~]# helm repo add stable http://mirror.azure.cn/kubernetes/charts/
"stable" has been added to your repositories

# 查看仓库
[root@k8smaster ~]# helm repo list
NAME      URL                                                   
aliyun    https://kubernetes.oss-cn-hangzhou.aliyuncs.com/charts
stable    http://mirror.azure.cn/kubernetes/charts/

helm repo update # 更新仓库
helm repo remove aliyun #删除仓库
```

### 部署应用

查找应用

```shell
# 通过helm search repo 名称
[root@centos-master home]# helm search repo weave
NAME                    CHART VERSION   APP VERSION     DESCRIPTION
aliyun/weave-cloud      0.1.2                           Weave Cloud is a add-on to Kubernetes which pro...
aliyun/weave-scope      0.9.2           1.6.5           A Helm chart for the Weave Scope cluster visual...
```

安装应用

```shell
# helm install 安装后应用名称 搜索后应用名称
[root@k8smaster ~]# helm install app-ui aliyun/weave-cloud

# 查看安装列表
[root@k8smaster ~]# helm list

# 查看安装状态
[root@k8smaster ~]# helm status app-ui
```

## 制作 Helm包

```
helm create myapp

tree myapp

├── charts                           # 这个 charts 依赖的其他 charts，无依赖可删除
├── Chart.yaml                       # 描述这个 Chart 的相关信息、包括名字、描述信息、版本等
├── templates                        # 模板目录
│   ├── deployment.yaml              # deployment 控制器的 Go 模板文件
│   ├── _helpers.tpl                 # 以 _ 开头的文件不会部署到 k8s 上，可用于定制通用信息
│   ├── hpa.yaml                     # hpa 的模板文件
│   ├── ingress.yaml                 # ingress 的模板文件
│   ├── NOTES.txt                    # Chart 部署到集群后的一些信息，例如：如何使用、列出缺省值
│   ├── serviceaccount.yaml          # serviceaccount 的 Go 模板文件
│   ├── service.yaml                 # service 的 Go 模板文件
│   └── tests                        # 测试pod目录
│       └── test-connection.yaml     # 测试pod的deployment文件
└── values.yaml                      # 模板的值文件，这些值会在安装时应用到 GO 模板生成部署文件
```

执行以下命令打包helm chart，这将在当前目录下生成一个名为myapp-x.x.x.tgz的tar包，其中x.x.x为当前版本号。

```text
helm package myapp
```

发布helm chart，这将把myapp-x.x.x.tgz包推送到名为myrepo的私有helm仓库中。

```
helm push myapp-x.x.x.tgz myrepo/
```

helm install命令来在Kubernetes上部署

```
helm install myapp myrepo/myapp-x.x.x.tgz
```

