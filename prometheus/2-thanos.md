## 组件介绍

- **Thanos Sidecar**：和Pormetheus 实例一起部署
  1. 代理Querier 组件对本地Prometheus数据读取
  2. 将Prometheus 本地监控数据通过对象存储接口上传到对象存储中
  3. 监视Prometheus的本地存储，若发现有新的监控数据保存到磁盘，会将这些监控数据上传至对象存储。
- **Thanos Query**：查询组件，prometheus v1 API的重新实现，跟prometheus的UI具有基本相当的功能。
- **Thanos Store**：在对象存储上构建api，使query组件可以查询到历史数据。
- **Thanos Ruler**：
  1. 不断计算和评估是否达到告警阀值，通知 AlertManager 触发告警
  2. 根据配置不断计算出新指标数据提供给 Thanos Query 查询并且/或者上传到对象存储。
- **Thanos Compact**：负责数据压缩与降采样，优化查询速度。

## 安装

thanos各组件使用的是同一个二进制包 只是启动参数不同

1. Thanos Sidecar

   ```
   sidecar --prometheus.url=http://192.168.1.202:9090 --tsdb.path=/alidata1/admin/data/prometheus --objstore.config-file=/alidata1/admin/tools/thanos-0.30.1/conf/store.yaml --grpc-address=192.168.1.202:10901 --http-address=192.168.1.202:10902 --log.level=error
   ```

   