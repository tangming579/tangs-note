## 基本概念

**Containerd** 是一个开源的**容器运行时**（Container Runtime），由 Docker 公司捐赠给云原生计算基金会（CNCF）。它是现代容器生态系统的核心组件之一，专注于管理容器的生命周期，提供容器运行环境

Containerd 以 Daemon 的形式运行在系统上，通过暴露底层的gRPC API，上层系统可以通过这些API管理机器上的容器。

### OCI

Open Container Initiative

- **目标**：定义容器镜像和运行时的**开放标准**（中立规范，不绑定具体实现）。
- **发起者**：由 Docker、Google、CoreOS 等公司发起，现由 Linux 基金会管理。

**两大核心规范**

| 规范名称             | 作用                                                         |
| -------------------- | ------------------------------------------------------------ |
| **OCI Image Spec**   | 定义容器镜像格式（如层（layers）、配置、manifest等），兼容 Docker 镜像格式。 |
| **OCI Runtime Spec** | 定义如何运行容器的标准（如根文件系统、namespaces、cgroups 配置等）。 |

**常见 OCI 运行时实现**

- **`runc`**（默认实现，被 containerd、CRI-O 使用）
- **`crun`**（Red Hat 开发的轻量实现）
- **`Kata Containers`**（基于虚拟机的运行时）
- **`gVisor`**（用户态内核隔离）

### CRI

Container Runtime Interface

- **目标**：定义 Kubernetes **kubelet** 与**容器运行时**之间的通信接口（API 规范）。

- **发起者**：由 Kubernetes 社区提出，用于解耦 kubelet 与具体运行时。

- **核心功能**：

  - 标准化 kubelet 对容器的操作（如创建/删除 Pod、执行命令、获取日志等）。

  - 支持多种运行时通过插件方式接入 Kubernetes（如 containerd、CRI-O、Docker 等）。

**kubelet 如何与 containerd 交互**

路径：kubelet → CRI Plugin（containerd 内部）→ containerd Core

<div>
    <image src="./img/cri.png"></image>
</div>

1. **kubelet** 通过 CRI 接口发起请求（如创建/删除容器）。
2. **containerd** 通过内置的 `cri` 插件（默认启用）接收 gRPC 请求。
3. `cri` 插件调用 containerd 的核心模块（如容器管理、镜像拉取等）完成操作。
4. 底层通过 **OCI 运行时**（如 `runc`）实际启动容器。

## 配置

### 配置文件

| **功能**       | **Docker**                | **Containerd**                |
| -------------- | ------------------------- | ----------------------------- |
| **主配置文件** | `/etc/docker/daemon.json` | `/etc/containerd/config.toml` |
| **日志配置**   | 在 `daemon.json` 中调整   | 在 `config.toml` 中调整       |

工具对比

| 工具          | 开发者     | 主要用途                                                     | 适用场景                          |
| ------------- | ---------- | ------------------------------------------------------------ | --------------------------------- |
| **`ctr`**     | Containerd | Containerd 的**原生命令行工具**，直接操作 containerd 的底层功能。 | 调试 containerd，低级别容器操作。 |
| **`crictl`**  | Kubernetes | 专为 Kubernetes CRI 设计的**调试工具**，命令风格类似 Docker。 | Kubernetes 节点调试，兼容 CRI。   |
| **`nerdctl`** | containerd | 为 Containerd 设计的**用户友好型工具**，完全兼容 Docker CLI 命令格式。 | 取代 Docker CLI，开发/生产环境。  |

### nerdctl

#### 安装

##### 1、nerdctl 安装

```sh
#因为nerdctl运行容器需要使用cni配置容器网络，所以先安装cni
wget https://github.com/containernetworking/plugins/releases/download/v1.6.2/cni-plugins-linux-amd64-v1.6.2.tgz
mkdir -p /opt/cni/bin
tar xvf cni-plugins-linux-amd64-v1.6.2.tgz -C /opt/cni/bin/

wget https://github.com/containerd/nerdctl/releases/download/v2.0.4/nerdctl-2.0.4-linux-amd64.tar.gz
tar xvf nerdctl-2.0.4-linux-amd64.tar.gz
cp nerdctl /usr/bin/
nerdctl version
```

##### 2、nerdctl命令补全设置

```
echo "source <(nerdctl completion bash)" >/etc/profile
source /etc/profile
12
```

##### 3、nerdctl访问https仓库

```
nerdctl --insecure-registry login harbor-server.linux.io
nerdctl tag ubuntu:20.04 harbor-server.linux.io/base-images/ubuntu:20.04
nerdctl --insecure-registry push harbor-server.linux.io/base-images/ubuntu:20.04	#推送镜像测试
```

#### insecure registry

参考：

https://github.com/containerd/containerd/blob/main/docs/hosts.md

 https://github.com/containerd/nerdctl/blob/main/docs/registry.md

```sh
mkdir -p /etc/containerd/certs.d/registry-dev.test.com
cat > /etc/containerd/certs.d/registry-dev.test.com/hosts.toml <<EOF
server = "http://registry-dev.test.com"
[host."http://registry-dev.test.com"]
  capabilities = ["pull", "resolve","push"]
  skip_verify = true
EOF
```

## 命令

### 镜像管理

| 操作         | Docker              | ctr                                              | crictl              | nerdctl              |
| ------------ | ------------------- | ------------------------------------------------ | ------------------- | -------------------- |
| **拉取镜像** | `docker pull nginx` | `ctr images pull docker.io/library/nginx:latest` | `crictl pull nginx` | `nerdctl pull nginx` |
| **列出镜像** | `docker images`     | `ctr images ls`                                  | `crictl images`     | `nerdctl images`     |
| **删除镜像** | `docker rmi nginx`  | `ctr images rm docker.io/library/nginx:latest`   | `crictl rmi nginx`  | `nerdctl rmi nginx`  |

### 容器管理

| 操作         | Docker                | ctr (复杂,需手动步骤)       | crictl (Pod/Container分离)      | nerdctl                |
| ------------ | --------------------- | --------------------------- | ------------------------------- | ---------------------- |
| **运行容器** | `docker run -d nginx` | 需先创建容器再启动（见注1） | `crictl runp` + `crictl create` | `nerdctl run -d nginx` |
| **列出容器** | `docker ps`           | `ctr containers ls`         | `crictl ps`                     | `nerdctl ps`           |
| **进入容器** | `docker exec -it sh`  | 不支持                      | `crictl exec -it sh`            | `nerdctl exec -it sh`  |
| **查看日志** | `docker logs`         | 不支持                      | `crictl logs`                   | `nerdctl logs`         |

### 资源监控

| **Docker**       | **Containerd**   |
| ---------------- | ---------------- |
| `docker stats`   | `crictl stats`   |
| `docker inspect` | `crictl inspect` |