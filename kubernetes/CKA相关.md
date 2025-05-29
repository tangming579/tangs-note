# 题

1. **RBAC鉴权**（4分）

   - 创建一个名为 deployment-clusterrole 的 ClusterRole, 并且对该 ClusterRole 只绑定对 Deployment，Daemonset, Statefulset 的创建权限
   - 在指定 namespace app-team1 创建一个名为 cicd-token 的 ServiceAccount，并且将上一步创建 ClusterRole 和该 ServiceAccount 绑定
   - 限定在 app-team1 namespace 下。

2. **设置 Node 不可调度**（4分）

   将ek8s-node-1节点设置为不可用，然后重新调度该节点上的所有Pod

3. **升级集群节点**

   现有的 Kubernetes 集权正在运行的版本是 1.21.0，仅将主节点上的所有 kubernetes 控制面板和组件升级到版本 1.22.0 另外，在主节点上升级 kubelet 和 kubectl

   注意：

   - drain 升级主节点之前，uncordon 在升级后

   - 不要升级工作节点、etcd、容器管理器、CNI插件、DNS服务或任何其他插件

4. **创建NetworkPolicy**（7分）

   创建 NetworkPolicy 名称为 allow-port-from-namespace ，允许 internal namespace 下的 pod 访问同 namespace 下 pod 的 9000 端口。不允许其它 namespace 下 Pod 连接，不允许非 9000 端口的 pod 访问。

5. **Pod 部署到指定节点**（4分）

   创建一个Pod，名字为nginx-kusc00401，镜像地址是nginx，调度到具有disk=spinning标签的节点上

6. **获取 Pod 节点健康数**（4分）

   检查集群中有多少节点为Ready状态，并且去除包含NoSchedule污点的节点。之后将数字写到/opt/KUSC00402/kusc00402.txt

7. **创建多个 container 的 pod**（4分）

   创建一个Pod，名字为kucc1，这个Pod可能包含1-4容器，该题为四个：nginx+redis+memcached+consul

8. **创建 Persistent Volume**

   创建一个pv，名字为app-config，大小为2Gi，访问权限为ReadWriteMany。Volume的类型为hostPath，路径为/srv/app-config

9. **创建 Persistent Volume Claim**

   创建一个名字为pv-volume的pvc，指定storageClass为csi-hostpath-sc，大小为10Mi
   然后创建一个Pod，名字为web-server，镜像为nginx，并且挂载该PVC至/usr/share/nginx/html，挂载的权限为ReadWriteOnce。之后通过kubectl edit或者kubectl path将pvc改成70Mi，并且记录修改记录。

10. **过滤Pod 日志**（5分）

    监控名为foobar的Pod的日志，并过滤出具有unable-access-website 信息的行，然后将写入到 /opt/KUTR00101/foobar

11. **添加 sidecar container**（7分）

    添加一个名为busybox且镜像为busybox的sidecar到一个已经存在的名为legacy-app的Pod上，这个sidecar的启动命令为/bin/sh, -c, 'tail -n+1 -f /var/log/legacy-app.log'。
    并且这个sidecar和原有的镜像挂载一个名为logs的volume，挂载的目录为/var/log/

12. **CPU 使用率最高的 Pod**（5分）

    找出具有name=cpu-user的Pod，并过滤出使用CPU最高的Pod，然后把它的名字写在已经存在的/opt/KUTR00401/KUTR00401.txt文件里（注意他没有说指定namespace。所以需要使用-A指定所有namespace）

13. **集群故障排查**（13分）

    情况1：一个名为wk8s-node-0的节点状态为NotReady，让其他恢复至正常状态，并确认所有的更改开机自动完成

    情况2：Pod 不能创建出来，检查 k8s 静态资源是否存在

14. **备份还原** etcd

    1. 为运行在 https://127.0.0.1:2379 上的现有 etcd 实例创建快照并且将快照保存到 /etc/data/etcd-snapshot.db

    2. 还原  /var/lib/backup/etcd-snapshot-previoys.db 的先前存在的快照

       提供了以下TLS证书和密钥，用于使用 etcdctl 连接到服务器：

       - ca证书：/opt/KUIN000601/ca.crt
       - 客户端证书：/opt/KUIN000601/etcd-client.crt
       - 客户端密钥：/opt/KUIN000601/etcd-client.key

15. **创建 Service**（7分）

    重新配置现有的 deployment front-end，并添加一个名为 http 的 port 规范，公开现有容器 **nginx** 的端口 80/tcp

    创建 service 名称为 front-end-svc 暴露窗口的 http 端口，通过高度节点的 NodePort 访问 Pod

16. **创建 Ingress**（8分）

    在**ing-internal** 命名空间下创建一个ingress，名字为pong，代理的service hi，端口为5678，配置路径/hi。

    验证：访问curl -kL <INTERNAL_IP>/hi会返回hi

17. 扩容 Deployment

    ```
    扩容名字为loadbalancer的deployment的副本数为6
    ```

# 1. 使用 RBAC 鉴权

https://kubernetes.io/zh-cn/docs/reference/access-authn-authz/rbac/

基于角色（Role）的访问控制（RBAC）是一种基于组织中用户的角色来调节控制对 计算机或网络资源的访问的方法。

RBAC 鉴权机制使用 `rbac.authorization.k8s.io` 来驱动鉴权决定，允许通过 Kubernetes API 动态配置策略。

RBAC API 声明了四种 Kubernetes 对象：

- Role：Namespace 内设置访问权限
- ClusterRole：集群内设置访问权限
- RoleBinding
- ClusterRoleBinding

> RoleBinding 也可以引用 ClusterRole，这种引用使得你可以跨整个集群定义一组通用的角色， 之后在多个名字空间中复用。



> 创建了绑定之后，不能再修改绑定对象所引用的 Role 或 ClusterRole。 试图改变绑定对象的 `roleRef` 将导致合法性检查错误。 如果你想要改变现有绑定对象中 `roleRef` 字段的内容，必须删除重新创建绑定对象。



```shell
kubectl create role pod-reader --verb=get,list,watch --resource=pods --namespace=acme

kubectl create clusterrole pod-reader --verb=get,list,watch --resource=pods

kubectl create rolebinding bob-admin-binding --clusterrole=admin --user=bob --namespace=acme

kubectl create clusterrolebinding root-cluster-admin-binding --clusterrole=cluster-admin --user=root
```

**CKA易错点：创建rolebinding的时候未指定namespace，把rolebinding创建在了default空间下；绑定指定的是clusterrole，不是role**

# 2. 节点控制（cordon、drain）

https://kubernetes.io/docs/reference/generated/kubectl/kubectl-commands#cordon

```
kubectl cordon NODE
kubectl drain NODE
kubectl uncordon NODE
```

在1.2之前，由于没有相应的命令支持，如果要维护一个节点，只能stop该节点上的kubelet将该节点退出集群，是集群不在将新的pod调度到该节点上。如果该节点上本身就没有pod在运行，则不会对业务有任何影响。如果该节点上有pod正在运行，kubelet停止后，master会发现该节点不可达，而将该节点标记为notReady状态，不会将新的节点调度到该节点上。同时，会在其他节点上创建新的pod替换该节点上的pod。

如此虽能够保证集群的健壮性，但可能还有点问题，如果业务只有一个副本，而且该副本正好运行在被维护节点上的话，可能仍然会造成业务的短暂中断。

- cordon：影响最小，只会将node调为SchedulingDisabled，之后再发创建pod，不会被调度到该节点
- drain：驱逐node上的pod，其他节点重新创建，接着，将节点调为 SchedulingDisabled

# 3. 升级集群

https://kubernetes.io/zh-cn/docs/tasks/administer-cluster/kubeadm/kubeadm-upgrade/

```sh
# 停止调度
kubectl cordon k8s-master 
# master 进行 pod 驱逐
kubectl drain k8s-master --ignore-daemonsets --delete-emptydir-data --force
# 登录到 master 节点
ssh k8s-master 
# 切换到 root 用户
# 更新
 apt-mark unhold kubeadm && \
 apt-get update && apt-get install -y kubeadm=1.19.0-00 && \
 apt-mark hold kubeadm
 
# 查看 kubeadm 版本
kubeadm version
# 验证集群是否可以更新
kubeadm upgrade plan
# 指定升级版本升级
kubeadm upgrade apply v1.19.0 --etcd-upgrade=false

# 升级 kubelet kubectl 
apt-get install kubelet=1.19.0-00 kubectl=1.19.0-00
# 查看升级后版本
kubectl version
# 重新运行 kubelet 
sudo systemctl daemon-reload
sudo systemctl restart kubelet

# uncordon node 恢复节点的调度
kubectl uncordon k8s-master
```

**CKA易错点：一定要先drain，最后uncordon，升级时要加 --etcd-upgrade=false**

# 4. NetworkPolicy

在 IP 地址或端口层面（OSI 第 3 层或第 4 层）控制网络流量

NetworkPolicies 适用于一端或两端与 Pod 的连接，与其他连接无关。 

Pod 可以通信的 Pod 是通过如下三个标识符的组合来辩识的：

1. 其他被允许的 Pods（例外：Pod 无法阻塞对自身的访问）
2. 被允许的 Namespace
3. IP 组块（例外：与 Pod 运行所在的节点的通信总是被允许的， 无论 Pod 或节点的 IP 地址）

**Pod 隔离的两种类型**

Pod 有两种隔离: 出口的隔离和入口的隔。这两种隔离（或不隔离）是独立声明的， 并且都与从一个 Pod 到另一个 Pod 的连接有关。

默认情况下，一个 Pod 的出口是非隔离的，即所有外向连接都是被允许的。如果有任何的 NetworkPolicy 选择该 Pod 并在其 `policyTypes` 中包含 “Egress”，则该 Pod 是出口隔离的

默认情况下，一个 Pod 对入口是非隔离的，即所有入站连接都是被允许的。如果有任何的 NetworkPolicy 选择该 Pod 并在其 `policyTypes` 中包含 “Ingress”，则该 Pod 被隔离入口， 我们称这种策略适用于该 Pod 的入口

网络策略是相加的，所以不会产生冲突

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: test-network-policy
  namespace: default
spec:
  podSelector:
    matchLabels:
      role: db
  policyTypes:
    - Ingress
    - Egress
  ingress:
    - from:
        - ipBlock:
            cidr: 172.17.0.0/16
            except:
              - 172.17.1.0/24
        - namespaceSelector:
            matchLabels:
              project: myproject
        - podSelector:
            matchLabels:
              role: frontend
      ports:
        - protocol: TCP
          port: 6379
  egress:
    - to:
        - ipBlock:
            cidr: 10.0.0.0/24
      ports:
        - protocol: TCP
          port: 5978
```

# 5. Pod 部署到指定节点

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: nginx-kusc00401
  labels:
    env: test
spec:
  containers:
  - name: nginx
    image: nginx
    imagePullPolicy: IfNotPresent
  nodeSelector:
    disk: spinning
```

# 6. 获取 Pod 节点健康数

获取健康节点数，不包含标记污点的节点 （NoSchedule）

```
# grep -v -i noschedule 过滤掉不可调度的节点(根据题意要求)
# grep -v -i noexecute 过滤掉 NotReady 的节点
kubectl describe node | grep -i taints | grep -v -i noschedule | grep -v -i noexecute
echo $num > /opt/KUSC00402/kusc00402.txt # 节点健康数量写入到文件

----------------------------------------------------------
kubectl get node | grep NotReady # 查看 NotReady 的节点
```

# 7. 创建多个 container 的 Pod

创建一个Pod，名字为kucc1，这个Pod可能包含1-4容器，该题为四个：nginx+redis+memcached+consul

```
# 快速获取 Pod yaml
kubectl run kucc1 --restart=Never --image=nginx --dry-run=client -oyaml > multi-container.yaml
# 编辑 yaml 增加其它容器
vi  multi-container.yaml
# 创建 pod
kubectl apply -f  multi-container.yaml
```

yaml：

```
apiVersion: v1
kind: Pod
metadata:
  name: multi-container
spec:
  containers:
  - image: nginx
    name: nginx
  - image: redis
    name: redis
  - image: memcached
    name: memcached
  - image: consul
    name: consul
```

# 8. 创建 PV (Persistent Volume)

在Kubenetes中，为了能够屏蔽底层存储实现的细节，让用户方便使用及管理员方便管理，Kubernetes从1.0版本就已经引入了Persistent Volume(PV)和Persistent Volume Claim(PVC)

- PV(持久卷)：是对存储资源的抽象，将存储定义为一种容器应用可以使用的资源，PV由管理员创建和配置，它与存储提供商的具体实现直接相关，例如：GlusterFS、iSCSI、RBD或GCE和AWS公有云提供的共享存储，通过插件式的机制进行管理，供应用访问和使用。除EmptyDir类型存储卷，PV的生命周期独立使用它的Pod。
- PVC是用户对存储资源的一个申请。就像Pod消耗Node的资源一样，PVC消耗PV的资源。PVC可以申请存储空间的大小(Size)和访问模式(例如ReadWriteOnce、ReadOnlyMany或ReadWriteMany)。

 有了PV,为什么又设计了PVC？

1. 职责分离，PVC中只用声明自己需要的存储size、access mode（单node独占还是多node共享？只读还是读写访问？）等业务真正关心的存储需求（不用关心存储实现细节），PV和其对应的后端存储信息则由交给cluster admin统一运维和管控，安全访问策略更容易控制。
2. PVC简化了User对存储的需求，PV才是存储的实际信息的承载体，通过kube-controller-manager中的persistentVolumeController将PVC与合适的PV bound到一起，从而满足User 对存储的实际需求。
3. PVC像是面向对对象编程中抽象出来的接口，PV是接口对应的实现。

创建一个pv，名字为app-config，大小为2Gi，访问权限为ReadWriteMany。Volume的类型为hostPath，路径为/srv/app-config

```yaml
apiVersion: v1
kind: PersistentVolume
metadata:
  name: app-config
  labels:
    type: local
spec:
  storageClassName: manual  #可以写也可以不写
  capacity:
    storage: 2Gi
  accessModes:
    - ReadWriteMany
  hostPath:
    path: "/srv/app-config"
```

# 9. PersistentVolumeClaim

1. 创建一个名字为pv-volume的pvc，指定storageClass为csi-hostpath-sc，大小为10Mi
2. 然后创建一个Pod，名字为web-server，镜像为nginx，并且挂载该PVC至/usr/share/nginx/html，挂载的权限为ReadWriteOnce
3. 之后通过kubectl edit或者kubectl path将pvc改成70Mi，并且记录修改记录。

``` yaml
# 创建 pvc
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: pvc-volume
spec:
  storageClassName: csi-hostpath-sc
  accessModes:
  - ReadWriteMany
  resources:
    requests:
      storage: 10Mi
---
# 创建 Pod
apiVersion: v1
kind: Pod
metadata:
  name: web-server
spec: 
  volumes:
  - name: pv-volume
    persistentVolumeClaim:
      claimName: pvc-volume
  containers:
  - name: nginx
    image: nginx
    volumeMounts:
    - mountPath: "/usr/share/nginx/html"
      name: pv-volume
---
# 编辑 pvc 容量，改为 70Mi
kubectl edit pvc pv-volume  --record
```

# 10. 过滤 Pod 日志

``` sh
kubectl logs foobar | grep unable-to-access-website > /opt/KUTR00101/foobar
```

# 11. 添加 sidecar container

Sidecar 模式就是指在原来的业务逻辑上再新加一个抽象层，Sidecar作为一种模式并不是Kubernetes的正式约定。

``` sh
kubectl get po legacy-app -oyaml > c-sidecar.yaml
```

# 12. CPU 使用率最高的 Pod

找出具有name=cpu-user的Pod，并过滤出使用CPU最高的Pod，然后把它的名字写在已经存在的/opt/KUTR00401/KUTR00401.txt文件里（注意他没有说指定namespace。所以需要使用-A指定所以namespace）

```sh
$ kubectl config use-context k8s
$ kubectl kubectl top pod -A -l name=cpu-user --sort-by=cpu
NAMESPACE     NAME                       CPU(cores)   MEMORY(bytes)   
kube-system   coredns-54d67798b7-hl8xc   7m           8Mi             
kube-system   coredns-54d67798b7-m4m2q   6m           8Mi
# 注意这里的pod名字以实际名字为准，按照CPU那一列进行选择一个最大的Pod，另外如果CPU的数值是1 2 3这样的。是大于带m这样的，因为1颗CPU等于1000m，注意要用>>而不是>

$ echo "coredns-54d67798b7-hl8xc" >> /opt/KUTR00401/KUTR00401.txt
```

# 13. 集群故障排查

一个名为wk8s-node-0的节点状态为NotReady，让其他恢复至正常状态，并确认所有的更改开机自动完成

```  sh
# 连接到节点
ssh wk8s-node-0
# 获取 root 权限
sudo -i
# 查看 kubelet 运行状态
systemctl status kubelet
# 非正常 running 状态，启动
systemctl start kubelet
# 设置开机自启
systemctl enable kubelet
```

# 14. 备份还原 etcd

1. 为运行在 https://127.0.0.1:2379 上的现有 etcd 实例创建快照并且将快照保存到 /etc/data/etcd-snapshot.db

2. 还原  /var/lib/backup/etcd-snapshot-previoys.db 的先前存在的快照

   提供了以下TLS证书和密钥，用于使用 etcdctl 连接到服务器：

   - ca证书：/opt/KUIN000601/ca.crt
   - 客户端证书：/opt/KUIN000601/etcd-client.crt
   - 客户端密钥：/opt/KUIN000601/etcd-client.key

``` sh
#备份：
ETCDCTL_API=3 etcdctl --endpoints=https://127.0.0.1:2379 \
  --cacert=<trusted-ca-file> --cert=<cert-file> --key=<key-file> \
  snapshot save <backup-file-location>
#恢复：  
ETCDCTL_API=3 etcdctl --data-dir <data-dir-location> snapshot restore snapshotdb

# 切换 etcd 目录，先停 etcd
cd /etc/kubernetes
mv manifests/etcd.yaml ./ # 移出 etcd.yaml etcd 会自动停止
# 修改 etcd 数据目录
vi etcd.yaml
# 查看 etcd 是否停止
docker ps -a | grep etcd
# 移回配置
mv etcd.yaml
# 查看 etcd 是否正常启动(可能会有点慢)
docker ps  | grep etcd
docker logs xxx
# 验证 api server 是否正常
kubectl get node 
```

# 15. 创建service

1. 重新配置一个已经存在的deployment front-end，在名字为nginx的容器里面添加一个端口配置，名字为http，暴露端口号为80

2. 创建一个service，名字为front-end-svc，暴露该deployment的http端口，并且service的类型为NodePort。

``` 
kubectl edit deployment front-end 
# port 配置存在不做修改，不存在则加入 port 规则； 一个 service 可以增加多个 port
    ports:
    - containerPort: 8080
      name: http
      protocol: TCP
# 创建 svc
kubectl expose deployment front-end --name=front-end-svc --port=80 --target-port=80 --type=NodePort

```

**易错点：service 别忘了指定名字**

# 16. 创建Ingress

创建 Ingress 资源，名称 pong , Namespace ing-internal，公开 hi 服务在 path 为 /hi ，使用服务的 5678 端口

```
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: pong
  namespace: ing-internal
spec:
  rules:
  - http:
      paths:
      - path: /hi
        pathType: Prefix
        backend:
          service:
            name: hi
            port:
              number: 5678
```

# 17. 扩容 Deployment

扩容 deployment 到 6 个 pod

```sh
kubectl scale deploy loadbalancer --replicas=6 
```

