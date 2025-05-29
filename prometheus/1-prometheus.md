## consul安装

1. 安装

   ```
   wget https://releases.hashicorp.com/consul/1.17.1/consul_1.17.1_linux_amd64.zip
   unzip consul_1.17.1_linux_amd64.zip -C /usr/local/consul/
   ```

2. 启动服务

   ```
   ./consul agent -server -bootstrap-expect 1 -data-dir=/usr/local/consul/data -config-dir=/etc/consul/ -node=n1 -bind=127.0.0.1 -client=0.0.0.0 -ui
   ```

3. 新增acl配置在/etc/consul下

   ```
   {
     "acl" : {
       "enabled" : true,
       "default_policy" : "deny",
       "down_policy" : "extend-cache"
     }
   }
   ```

4. 创建acl token（日志就会输出 `consul.acl: ACL bootstrap completed`）

   ```
   $ ./consul acl bootstrap
   AccessorID:       26c008ee-ca29-16b2-1c5c-c6b8b515fc82
   SecretID:         a689274a-8f8b-214e-d8d8-3d79982b39cc
   Description:      Bootstrap Token (Global Management)
   Local:            false
   Create Time:      2023-12-24 20:52:45.8803759 +0800 CST
   Policies:
      00000000-0000-0000-0000-000000000001 - global-management
   ```

5. 节点增加token信息

   ```
   {
     "acl": {
       "enabled": true,
       "default_policy": "deny",
       "enable_token_persistence": true,
       "tokens": {
           "master": "a689274a-8f8b-214e-d8d8-3d79982b39cc"
       }
     }
   }
   ```

## prometheus

1. 下载并解压服务

   ```
   tar xvfz prometheus-*.tar.gz
   cd prometheus-*
   ```

2. 配置prometheus

   https://www.python100.com/html/120606.html

   https://blog.csdn.net/w2009211777/article/details/124005822

3. 启动prometheus

   ```
   ./prometheus --config.file=prometheus.yml
   ```

### 配置说明

参考：

- https://prometheus.io/docs/prometheus/latest/configuration/configuration/
- https://www.cnblogs.com/zhoujinyi/p/11944176.html

#### global

```
global:
  # 默认情况下抓取目标的频率.
  [ scrape_interval: <duration> | default = 1m ]
  # 抓取超时时间.
  [ scrape_timeout: <duration> | default = 10s ]
  # 评估规则的频率.
  [ evaluation_interval: <duration> | default = 1m ]
  # 与外部系统通信时添加到任何时间序列或警报的标签（联合，远程存储，Alertmanager）.即添加到拉取的数据并存到数据库中
  external_labels:
    [ <labelname>: <labelvalue> ... ]
# 规则文件指定了一个globs列表. 
# 从所有匹配的文件中读取规则和警报.
rule_files:
  [ - <filepath_glob> ... ]
# 抓取配置列表.
scrape_configs:
  [ - <scrape_config> ... ]
# 警报指定与Alertmanager相关的设置.
alerting:
  alert_relabel_configs:
    [ - <relabel_config> ... ]
  alertmanagers:
    [ - <alertmanager_config> ... ]
# 与远程写入功能相关的设置.
remote_write:
  [ - <remote_write> ... ]
# 与远程读取功能相关的设置.
remote_read:
  [ - <remote_read> ... ]
```

#### scrape_config

<scrape_config>指定一组描述如何抓取的目标和参数。 一般一个scrape指定单个作业。目标可以通过<static_configs>参数静态配置，也可以使用其中一种支持的服务发现机制动态发现。此外，<relabel_configs>允许在抓取之前对任何目标及其标签进行高级修改。

```
# 默认分配给已抓取指标的job名称。
job_name: <job_name>
# 从目标获取指标的HTTP资源路径.
[ metrics_path: <path> | default = /metrics ]
# honor_labels控制Prometheus如何处理已经存在于已抓取数据中的标签与Prometheus将附加服务器端的标签之间的冲突（"job"和"instance"标签，手动配置的目标标签以及服务发现实现生成的标签）。
# 如果honor_labels设置为"true"，则通过保留已抓取数据的标签值并忽略冲突的服务器端标签来解决标签冲突。
# 如果honor_labels设置为"false"，则通过将已抓取数据中的冲突标签重命名为"exported_ <original-label>"（例如"exported_instance"，"exported_job"）然后附加服务器端标签来解决标签冲突。 这对于联合等用例很有用，其中应保留目标中指定的所有标签。
# 请注意，任何全局配置的"external_labels"都不受此设置的影响。 在与外部系统通信时，它们始终仅在时间序列尚未具有给定标签时应用，否则将被忽略。
[ honor_labels: <boolean> | default = false ]

# 配置用于请求的协议方案.
[ scheme: <scheme> | default = http ]

# 可选的HTTP URL参数.
params:
  [ <string>: [<string>, ...] ]

# 使用配置的用户名和密码在每个scrape请求上设置`Authorization`标头。 password和password_file是互斥的。
basic_auth:
  [ username: <string> ]
  [ password: <secret> ]
  [ password_file: <string> ]

# 使用配置的承载令牌在每个scrape请求上设置`Authorization`标头。 它`bearer_token_file`和是互斥的。
[ bearer_token: <secret> ]

# 使用配置的承载令牌在每个scrape请求上设置`Authorization`标头。 它`bearer_token`和是互斥的。
[ bearer_token_file: /path/to/bearer/token/file ]

# 配置scrape请求的TLS设置.
tls_config:
  [ <tls_config> ]

# 可选的代理URL.
[ proxy_url: <string> ]

# Azure服务发现配置列表.
azure_sd_configs:
  [ - <azure_sd_config> ... ]

# Consul服务发现配置列表.
consul_sd_configs:
  [ - <consul_sd_config> ... ]

# DNS服务发现配置列表。
dns_sd_configs:
  [ - <dns_sd_config> ... ]

# 文件服务发现配置列表。
file_sd_configs:
  [ - <file_sd_config> ... ]

# Kubernetes服务发现配置列表。
kubernetes_sd_configs:
  [ - <kubernetes_sd_config> ... ]

# 此job的标记静态配置目标列表。
static_configs:
  [ - <static_config> ... ]

# 目标重新标记配置列表。
relabel_configs:
  [ - <relabel_config> ... ]

# 度量标准重新配置列表。
metric_relabel_configs:
  [ - <relabel_config> ... ]

# 对每个将被接受的样本数量的每次抓取限制。
# 如果在度量重新标记后存在超过此数量的样本，则整个抓取将被视为失败。 0表示没有限制。
[ sample_limit: <int> | default = 0 ]
```

#### consul_sd_config

```
# 根据文档要求定义consul api信息
[ server: <host> | default = "localhost:8500" ]
[ token: <secret> ]
[ datacenter: <string> ]
[ scheme: <string> | default = "http" ]
[ username: <string> ]
[ password: <secret> ]

tls_config:
  [ <tls_config> ]

# 检索目标的服务列表，默认所有服务器
services:
  [ - <string> ]

#标签的可选列表，用于过滤给定服务的节点。 服务必须包含列表中的所有标签。
tags:
  [ - <string> ]

# 节点元数据，用于过滤给定服务的节点。
[ node_meta:
  [ <name>: <value> ... ] ]

# Consul标签通过其连接到标签标签中的字符串
[ tag_separator: <string> | default = , ]

# Allow stale Consul results (see https://www.consul.io/api/features/consistency.html). Will reduce load on Consul.
[ allow_stale: <bool> ]

# 刷新提供的名称之后的时间，在大型设置中，增加此值可能是个好主意，因为目录会一直更改。
[ refresh_interval: <duration> | default = 30s ]
```

### PromQL



## alertmanager

https://blog.csdn.net/qq_37843943/article/details/120665690

https://blog.csdn.net/sinat_32582203/article/details/122617740

https://www.cnblogs.com/zydev/p/16850401.html

参数

```
group_wait: 10s
group_interval: 30m
repeat_interval: 50m
```

告警过程

```
1. alertmanager收到告警后，等待group_wait（10s），发送第一次通知
2. 未达到group_interval（30m 10s），休眠
3. 达到group_interval（30m 10s）时，小于repeat_interval（50m 10s），休眠
4. 到下一个group_interval（60m 10s），大于repeat_interval（50m 10s），发送第二次通知

Firing（0s） - 第一次通知（10s） - 第二次通知（60m 10s）
```



https://www.cnblogs.com/zhaojiedi1992/p/zhaojiedi_liunx_65_prometheus_alertmanager_conf.html

https://prometheus.io/docs/alerting/latest/configuration/#inhibit_rule
