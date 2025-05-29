## 虚拟机规划

|         | 主机名      | IP             |
| :------ | :---------- | :------------- |
| 主节点  | master-node | 192.168.56.102 |
| 从节点1 | work-node1  | 192.168.56.103 |
| 从节点2 | work-node2  | 192.168.56.104 |

## 必要的准备

### 关闭防火墙

防火墙一定要提前关闭，否则在后续安装K8S集群的时候是个trouble maker。执行下面语句关闭，并禁用开机启动：

```bash
systemctl stop firewalld & systemctl disable firewalld
```

### 关闭Swap

类似ElasticSearch集群，在安装K8S集群时，Linux的Swap内存交换机制是一定要关闭的，否则会因为内存交换而影响性能以及稳定性。这里，我们可以提前进行设置：

执行swapoff -a可临时关闭，但系统重启后恢复
编辑/etc/fstab，注释掉包含swap的那一行即可，重启后可永久关闭，如下所示：

```
/dev/mapper/centos-root /                       xfs     defaults        0 0
UUID=c32383b8-5912-4536-8cd5-4d6ab99d8c45 /boot                   xfs     defaults        0 0
#/dev/mapper/centos-swap swap                    swap    defaults        0 0
```

### 关闭SeLinux

临时关闭：

```bash
setenforce 0
```

要永久禁用SELinux，使用编辑器打开/etc/sysconfig/selinux文件，如下所示：

```
vi /etc/sysconfig/selinux
```

然后将配置SELinux=enforcing改为SELinux=disabled，如下图所示。

```
SELINUX=disabled
```

然后，保存并退出文件，为了使配置生效，需要重新启动系统，然后使用sestatus命令检查SELinux的状态，如下所示：

```
sestatus
```

### 配置yum源

下载centos基础yum源配置（这里用的是阿里云的镜像）

```
curl -o CentOS-Base.repo http://mirrors.aliyun.com/repo/Centos-7.repo
```


下载docker的yum源配置

```
curl -o docker-ce.repo https://download.docker.com/linux/centos/docker-ce.repo
```


配置kubernetes的yum源

```
cat <<EOF > /etc/yum.repos.d/kubernetes.repo
[kubernetes]
name=Kubernetes
baseurl=http://mirrors.aliyun.com/kubernetes/yum/repos/kubernetes-el7-x86_64
enabled=1
gpgcheck=0
repo_gpgcheck=0
gpgkey=http://mirrors.aliyun.com/kubernetes/yum/doc/yum-key.gpg
        http://mirrors.aliyun.com/kubernetes/yum/doc/rpm-package-key.gpg
EOF
```


执行下列命令刷新yum源缓存

```
yum clean all
yum makecache
yum repolist
```

安装docker

```
yum install -y docker-ce
```

## 设置三个虚拟机网络

对master-node做如下设置

编辑/etc/hostname，将hostname修改为master-node
编辑/etc/hosts，追加内容 【192.168.56.109 master-node】【192.168.56.110 work-node1】【192.168.56.108 work-node2】
过程展示如下：

```
vi /etc/hostname 
vi /etc/hosts
cat /etc/hostname 
master-node

cat /etc/hosts
127.0.0.1   localhost localhost.localdomain localhost4 localhost4.localdomain4
::1         localhost localhost.localdomain localhost6 localhost6.localdomain6
192.168.56.102   master-node
192.168.56.104   work-node1
192.168.56.103   work-node2
```

 设置work-node1和work-node2。方法同上。

## kubeadm安装k8s

由于之前已经设置好了kubernetes的yum源，只要执行

> k8s 已经弃用了docker了，如果安装 V1.24就会出现错误，安装的时候指定一下1.23版本，就可以解决了

```bash
yum install -y kubelet-1.23.6 kubeadm-1.23.6 kubectl-1.23.6
```

## 主节点初始化K8S

主节点就是本文提到的“master-node”虚拟机。 执行下列代码，开始master节点的初始化工作：

```
kubeadm init --apiserver-advertise-address=192.168.56.102 --image-repository registry.cn-hangzhou.aliyuncs.com/google_containers --kubernetes-version v1.23.6 --service-cidr=10.1.0.0/16 --pod-network-cidr=10.244.0.0/16
```

- 提示：/proc/sys/net/bridge/bridge-nf-call-iptables contents are not set to 1

  解决方法：

  ```
  echo "1" >/proc/sys/net/bridge/bridge-nf-call-iptables
  ```

- 提示：unknown service runtime.v1alpha2.RuntimeService

  解决方法：

  参考：https://www.cnblogs.com/immaxfang/p/16721407.html

  删除 /etc/containerd/config.toml 文件并重启 containerd 即可。

  ```
  mv /etc/containerd/config.toml /root/config.toml.bak
  
  systemctl restart containerd
  ```

- 提示：The HTTP call equal to ‘curl -sSL http://localhost:10248/healthz’ failed

  解决方法：https://blog.csdn.net/qq_43762191/article/details/125567365

  ```
  vim /etc/docker/daemon.json
  
  {
    "exec-opts": ["native.cgroupdriver=systemd"]
  }
  ```

  ```
  # 重启docker
  systemctl restart docker
  ```

  ```
  # 重新初始化
  kubeadm reset # 先重置
  
  kubeadm init……
  ```

- 提示：The connection to the server localhost:8080 was refused - did you specify the right host or port

  ```sh
  #设置环境变量
  #（worker节点没有admin.conf，要从master的 /etc/kubernetes/中拷贝
  echo "export KUBECONFIG=/etc/kubernetes/admin.conf" >> /etc/profile
  #使设置生效
  source /etc/profile
```
  
  

