## 概念

CoreDNS是一种新的DNS服务器，它开发的初衷主要是用于Linux和docker的配合使用，自kubernetes 1.11版本开始，CoreDNS取代原来的 KubeDNS 成为k8s中默认的DNS组件。

在k8s安装完成后，我们可以通过命令“`kubectl get pod -n kube-system`”查看到 CoreDNS 的pod

DNS 服务器支持正向查找（A 和 AAAA 记录）、端口发现（SRV 记录）、反向 IP 地址发现（PTR 记录）等。

如果 Pod 的 `dnsPolicy` 设置为 `default`，则它将从 Pod 运行所在节点继承名称解析配置。 Pod 的 DNS 解析行为应该与节点相同。

## Pod 的 DNS 策略

- **ClusterFirst**：这是默认的DNS策略，意味着当Pod需要进行域名解析时，首先会查询集群内部的CoreDNS服务。通过CoreDNS来做域名解析，表示Pod的/etc/resolv.conf文件被自动配置指向kube-dns服务地址。
- **None**：使用该策略，Kubernetes会忽略集群的DNS策略。需要您提供**dnsConfig**字段来指定DNS配置信息，否则Pod可能无法正确解析任何域名。
- **Default**：Pod直接继承集群节点的域名解析配置。
- **ClusterFirstWithHostNet**：强制在hostNetwork网络模式下使用ClusterFirst策略（默认使用Default策略）。

## coredns ConfigMap

在k8s集群配置完成后，我们可以通过命令“`kubectl edit configmap coredns -n kube-system`”查看到相应的配置文件

```
Corefile: |
  .:53 {
      errors  # 输出错误信息，若需调试请设置为debug
      log     # 输出客户端请求解析信息
      health { # 健康检查配置
        lameduck 15s # 关闭延迟时间
      }
      ready # CoreDNS 插件，一般用来做可读性检查，可以通过 http://localhost:8181/ready 读取。
      # CoreDNS Kubernetes 插件，提供集群内服务解析能力。
      kubernetes {{.ClusterDomain}} in-addr.arpa ip6.arpa {
        pods verified
        fallthrough in-addr.arpa ip6.arpa
      }
      prometheus :9153 # CoreDNS 自身 metrics 数据接口。
      # 当域名不在 Kubernetes 域时，将请求转发到预定义的解析器。
      forward . /etc/resolv.conf { 
        max_concurrent 1000
      }
      cache 30 # DNS 查询缓存。
      loop  #环路检测，如果检测到环路，则停止 CoreDNS。
      reload #允许自动重新加载已更改的 Corefile, 编辑 ConfigMap 配置后，请等待两分钟以使更改生效。
      loadbalance #循环 DNS 负载均衡器，可以在答案中随机 A、AAAA、MX 记录的顺序。
  }
```

### 1. 自定义hosts解析特定域名

```plaintext
hosts {
  192.168.80.135 www.tm.com
  fallthrough
}
```

作用

- 功能：直接将域名 `www.tm.com` 静态映射到 IP `192.168.80.135`，类似本地 `hosts` 文件。
- 匹配规则：严格匹配 `www.tm.com`（精确域名），不处理子域名（如 `api.tm.com`）

------

### 2. 特定域名的 DNS 服务器转发

```plaintext
test.com:53 {
  errors
  cache 30
  forward . 192.168.80.135
}
```

**作用**

- 功能：将所有以 `test.com` 结尾的域名（包括子域名）转发到指定的 DNS 服务器 `192.168.80.135` 进行解析。
- 匹配规则：通配所有 `test.com` 及其子域（如 `www.test.com`、`api.test.com`）。