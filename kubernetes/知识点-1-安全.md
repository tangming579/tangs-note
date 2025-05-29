## Pod Security Policy

Pod Security Policy 定义了一组 Pod 运行时必须遵循的条件及相关字段的默认值，Pod 必须满足这些条件才能被成功创建，例如禁止采用 root权限、防止容器逃逸等等。

Pod Security Policy 是集群级别的资源，使用流程：

1. 启用 PSP 准入控制
2. 创建 PSP
3. 创建 ClusterRole/Role 授权 PSP 使用
4. 创建 ClusterRoleBinding/RoleBinding

### 如何开启、关闭PSP

PSP 默认没有开启，如果开启PSP之后，没有写PSP规则的话，任何用户都创建不了POD，包括管理员。

1. 进入k8s集群的master节点（如果是多集群，那master节点都要修改）
2. 编辑“/etc/kubernetes/manifests/kube-apiserver.yaml”文件
3. 修改行：- --enable-admission-plugins=xxx，增加或去掉“PodSecurityPolicy”，例如：“- --enable-admission-plugins=NodeRestriction,PodSecurityPolicy,Priority”，修改为“- --enable-admission-plugins=NodeRestriction,Priority”
4. 等待apiserver重启即可

**注意**：一定要等k8s正常工作之后再启动PSP，如果etcd,api都没启动好就启动PSP，则相关pod都启动不了了 。

启用PSP之后，创建pod必须先访问PSP规则，通过了PSP规则才能创建POD，否则创建pod失败。

### 如何验证psp策略有没有生效

（1）创建一条PSP策略，在项目中开启。

​    psp策略yaml文件如下：（限制容器的host端口范围为8000-8080）

```
apiVersion: extensions/v1beta1
kind: PodSecurityPolicy
metadata:
  name: permissive
spec:
  seLinux:
    rule: RunAsAny
  supplementalGroups:
    rule: RunAsAny
  runAsUser:
    rule: RunAsAny
  fsGroup:
    rule: RunAsAny
  hostPorts:
  - min: 8000
    max: 8080
  volumes:
  - '*'
```

（2）创建deployment，指定容器pod的hostpart为（**8000-8080**）以外的数字。

（3）如果psp策略生效了，则pod起不来。

## Pod Security Admission

PSP 缺陷：没有 dry-run 审计模式、不方便开启和关闭等，并且使用起来也不那么清晰。

| 策略级别（level） | 描述                                                         |
| :---------------- | :----------------------------------------------------------- |
| privileged        | 不受限制，通常适用于特权较高、受信任的用户所管理的系统级或基础设施级负载，例如CNI、存储驱动等。 |
| baseline          | 限制较弱但防止已知的特权提升（Privilege Escalation），通常适用于部署常用的非关键性应用负载，该策略将禁止使用hostNetwork、hostPID等能力。 |
| restricted        | 严格限制，遵循Pod防护的最佳实践。                            |

Pod Security Admission配置是命名空间级别的，控制器将会对该命名空间下Pod或容器中的安全上下文（Security Context）以及其他参数进行限制。其中，privileged策略将不会对Pod和Container配置中的securityContext字段有任何校验，而Baseline和Restricted则会对securityContext字段有不同的取值要求