## WSL 安装 Ubuntu

WSL 2 设置为默认版本

```powershell
wsl --set-default-version 2
#列出当前已安装版本
wsl --list
#卸载指定版本
wsl --unregister CentOS8
#列出可安装版本
wsl --list --online

#在线安装：
wsl --install -d Ubuntu-22.04
#下载安装文件：
Invoke-WebRequest -Uri https://aka.ms/wslubuntu2204 -OutFile Ubuntu.appx -UseBasicParsing
#设置默认 root 登录
./ubuntu.exe config --default-user root
```

升级内核：https://wslstorestorage.blob.core.windows.net/wslblob/wsl_update_x64.msi

## 安装Docker

```shell
#1、新软件列表和允许使用https
sudo apt-get update
sudo apt-get install \
    ca-certificates \
    curl \
    gnupg \
    lsb-release

#2、添加阿里源的GPG
curl -fsSL https://mirrors.aliyun.com/docker-ce/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg

#3、设置阿里源的docker仓库
 echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://mirrors.aliyun.com/docker-ce/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

#4.安装docker:

#4.1更新apt-get
sudo apt-get update

#4.2安装最新的docker版本
sudo apt-get install docker-ce docker-ce-cli containerd.io

#4.3启动docker
sudo service docker start

#4.4查看docker服务状态
sudo service docker status
```

执行 docker ps 报错：

```
Cannot connect to the Docker daemon at unix:///var/run/docker.sock. Is the docker daemon running?
```

查看 docker日志：

```
cat /var/log/docker.log
```

最后经过搜错发现，是因为最新版的ubuntu系统使用了iptables-nft，而WSL2不支持导致的。

需要使用如下命令修改信息：

```haskell
update-alternatives --config iptables
```

## 安装Kubectl

下载

```shell
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
```

安装

```
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

## 安装MiniKube

参考：https://zhuanlan.zhihu.com/p/543458320

```shell
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

minikube start --registry-mirror=https://registry.docker-cn.com  --image-mirror-country cn --kubernetes-version=v1.26.3

验证
$kubectl get ns
NAME              STATUS   AGE
default           Active   102s
kube-node-lease   Active   104s
kube-public       Active   104s
kube-system       Active   104s
```

启动报错：

```
 Exiting due to DRV_AS_ROOT: The "docker" driver should not be used with root privileges
```

解决方法：

```
minikube start --force --driver=docker
```

