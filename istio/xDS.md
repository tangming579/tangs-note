## Pilot

https://layer5.io/resources/service-mesh/istio-pilot

Istio Pilot的Debug API接口通常用于获取有关Pilot内部状态的信息，以帮助诊断问题。这些接口通常不用于生产环境，因为它们可能暴露敏感信息，并且可能会对Pilot性能产生不利影响。

以下是一些常见的Pilot Debug API接口：

1. `/debug/adscerts`：显示ADS API使用的证书信息。
2. `/debug/configz`：以JSON格式返回内部配置信息。
3. `/debug/edsz`：显示有关Envoy动态服务发现信息的统计数据。
4. `/debug/endpointz`：显示Pilot认为的服务端点信息。
5. `/debug/push`：触发Pilot向Envoy代理推送配置更新。
6. `/debug/push/shardz`：显示有关配置分片的信息。
7. `/debug/registrationz`：显示有关服务注册信息的调试信息。
8. `/debug/sdsz`：显示有关服务发现信息的统计数据。
9. `/debug/workloadz`：显示有关工作负载认证信息的调试信息。

要使用这些接口，你可以使用`curl`或类似工具从命令行访问它们。例如：

```
bashcurl http://localhost:15000/debug/configz
```

## Envoy

https://www.envoyproxy.io/docs/envoy/latest/operations/admin

通过 HTTP GET 请求访问 Envoy 监听的 admin 地址（通常是 `localhost:15000`），并带上 `/config_dump` 路径，可以获取当前 Envoy 从各种组件加载的配置信息，并转储为 JSON 进行输出。在命令行中，你可以使用 `curl` 工具来实现这一操作，如下：

```
bashcurl http://localhost:15000/config_dump
```

这个命令会返回大量的 JSON 数据，其中包含了 Envoy 的路由、监听器、集群等配置信息。这些信息对于调试和诊断 Envoy 的行为非常有用。

另外，Envoy 还提供了其他 Admin API 接口，如 `/routes` 用于获取路由配置，`/clusters` 用于获取集群配置等。这些接口都可以通过类似的 HTTP GET 请求来访问123。