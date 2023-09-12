

```
spring.datasource.type = com.zaxxer.hikari.HikariDataSource
spring.datasource.hikari.minimum-idle = 20
spring.datasource.hikari.maximum-pool-size = 200
spring.datasource.hikari.max-lifetime = 1500000
spring.datasource.hikari.idle-timeout = 600000
spring.datasource.hikari.connection-timeout = 20000
spring.datasource.platform = postgresql
spring.datasource.sql-script-encoding = utf-8
```



```
-Xms3g -Xmx3g -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=256m -XX:+UseG1GC -XX:MaxGCPauseMillis=40 -XX:+UseStringDeduplication -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -Djava.security.egd=file:/dev/./urandom
```

