容器部署java服务时，当启动参数中 -Xms -Xmx 值小于容器 limit 设定时，就会 oom，推荐改为：

```
java -XX:+UseContainerSupport -XX:MinRAMPercentage=25.0 -XX:MaxRAMPercentage=85.0 ........ -jar /app/app.jar 
```

