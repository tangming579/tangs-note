## 概念

官网：https://istio.io/latest/zh/docs

Istio的核心思想就是将服务治理的功能从业务服务中独立出来，作为一个sidecar容器，解耦的同时也能够兼容不同语言，无需和业务服务使用同一套语言。公共的治理能力独立后，所有组件都可以接入，而且采用sidecar的方式让业务无需任何修改即可接入。

Istio主要提供四个特性：

- 流量管理：在实现服务连接的基础上，通过控制服务间的流量和调用，实现请求路由、负载均衡、超时、重试、熔断、故障注入、重定向等功能
- 安全：提供证书配置管理，以及服务访问认证、授权等安全能力
- 策略控制：提供访问速率限制能力。
- 观测：获取服务运行指标和输出，提供调用链追踪和日志收集能力。

## 安装 istio

### 下载安装包

手动下载：

```
wget https://github.com/istio/istio/releases/download/1.16.2/istio-1.16.2-linux-amd64.tar.gz
tar -xf istio-1.16.2-linux-amd64.tar.gz 
cd istio-1.16.2/
cp bin/istioctl /usr/local/bin/
```

脚本下载：

```
curl -L https://istio.io/downloadIstio | ISTIO_VERSION=1.16.2 TARGET_ARCH=x86_64 sh -
cd istio-1.16.1
export PATH=$PWD/bin:$PATH
```

### 通过 istioctl 安装 istio

1. 安装 demo 配置的 istio

```
$ istioctl install --set profile=demo -y
✔ Istio core installed
✔ Istiod installed
✔ Egress gateways installed
✔ Ingress gateways installed
✔ Installation complete
```

2. 给命名空间添加标签，指示 Istio 在部署应用的时候，自动注入 Envoy SideCar 代理：

```
kubectl label namespace default istio-injection=enabled
```

- 错误提示：Istiod encountered an error: failed to wait for resource: resources not ready

  解决方法：如果k8s是只有master节点的话，要开启允许master节点调度

  ```
  # 查看node
  kubectl get nodes 
  
  # 查看污点
  kubectl describe node k8s-master |grep Taints
  Taints:    node-role.kubernetes.io/master:NoSchedule
  
  # 删除污点
  kubectl taint nodes --all node-role.kubernetes.io/master-
  
  # 让master节点参与调度，#如果想删除，把=换成-
  kubectl label nodes k8s-master node-role.kubernetes.io/worker=
  ```

### istio 配置组件

| 核心组件               | default | demo | minimal | remote | empty | preview |
| ---------------------- | ------- | ---- | ------- | ------ | ----- | ------- |
| `istio-egressgateway`  |         | ✔    |         |        |       |         |
| `istio-ingressgateway` | ✔       | ✔    |         |        |       | ✔       |
| `istiod`               | ✔       | ✔    | ✔       |        |       | ✔       |

## 部署示例应用

部署 `Bookinfo` 示例应用

```
$ kubectl apply -f samples/bookinfo/platform/kube/bookinfo.yaml
service/details created
serviceaccount/bookinfo-details created
deployment.apps/details-v1 created
service/ratings created
serviceaccount/bookinfo-ratings created
deployment.apps/ratings-v1 created
service/reviews created
serviceaccount/bookinfo-reviews created
deployment.apps/reviews-v1 created
deployment.apps/reviews-v2 created
deployment.apps/reviews-v3 created
service/productpage created
serviceaccount/bookinfo-productpage created
deployment.apps/productpage-v1 created
```

查看 Pod 是否已经就绪：

```
$ kubectl get pods
NAME                              READY   STATUS    RESTARTS   AGE
details-v1-558b8b4b76-2llld       2/2     Running   0          2m41s
productpage-v1-6987489c74-lpkgl   2/2     Running   0          2m40s
ratings-v1-7dc98c7588-vzftc       2/2     Running   0          2m41s
reviews-v1-7f99cc4496-gdxfn       2/2     Running   0          2m41s
reviews-v2-7d79d5bd5d-8zzqd       2/2     Running   0          2m41s
reviews-v3-7dbcdcbc56-m8dph       2/2     Running   0          2m41s
```

检查服务是否已经启动：

```
$ kubectl exec "$(kubectl get pod -l app=ratings -o jsonpath='{.items[0].metadata.name}')" -c ratings -- curl -sS productpage:9080/productpage | grep -o "<title>.*</title>"
<title>Simple Bookstore App</title>
```

### 对外开放应用程序

服务部署后，还需要添加路由规则，将请求路由到对应的服务

```
$ kubectl apply -f samples/bookinfo/networking/bookinfo-gateway.yaml
gateway.networking.istio.io/bookinfo-gateway created
virtualservice.networking.istio.io/bookinfo created
```

获取port，也就是80端口映射的目的端口，即31114

```
$ kubectl get svc istio-ingressgateway -n istio-system
NAME                   TYPE           CLUSTER-IP     EXTERNAL-IP   PORT(S)   AGE
istio-ingressgateway   LoadBalancer   10.1.251.189   <pending>     15021:30475/TCP,80:31114/TCP,443:32409/TCP,31400:31098/TCP,15443:30712/TCP   37h

```

访问：http://192.168.56.102:31114/productpage

## 仪表板

1. 安装插件

   ```
   $ kubectl apply -f samples/addons
   $ kubectl rollout status deployment/kiali -n istio-system
   Waiting for deployment "kiali" rollout to finish: 0 of 1 updated replicas are available...
   deployment "kiali" successfully rolled out
   ```

2. 访问 Kiali 仪表板。

   ```
   $ istioctl dashboard kiali
   http://localhost:20001/kiali
   ```

3. 查看 kiali服务

   ```
   $ k get svc -n istio-system
   NAME                   TYPE           CLUSTER-IP     EXTERNAL-IP   PORT(S)                   AGE
   kiali                  ClusterIP      10.1.251.8     <none>        20001/TCP,9090/TCP        13m
   prometheus             ClusterIP      10.1.191.231   <none>        9090/TCP                  13m
   tracing                ClusterIP      10.1.170.227   <none>        80/TCP,16685/TCP          13m
   zipkin                 ClusterIP      10.1.158.175   <none>        9411/TCP
   ```

4. 外部浏览器可以直接访问，需要将 service 的服务类型设置为 nodeport，执行命令如下：

   ```
   kubectl patch svc -n istio-system kiali -p '{"spec": {"type": "NodePort"}}'
   ```

5. 再次查看

   ```
   [root@centos-master ~]# k get svc -n istio-system
   NAME                   TYPE           CLUSTER-IP     EXTERNAL-IP   PORT(S)                          AGE
   kiali                  NodePort       10.1.251.8     <none>        20001:32279/TCP,9090:30537/TCP   16m
   prometheus             ClusterIP      10.1.191.231   <none>        9090/TCP                         16m
   tracing                ClusterIP      10.1.170.227   <none>        80/TCP,16685/TCP                 16m
   zipkin                 ClusterIP      10.1.158.175   <none>        9411/TCP                         16m
   ```

6. 访问：http://192.168.56.102:32279/kiali/