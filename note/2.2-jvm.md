## 1、JVM运行参数

### 1.1、三种参数类型

- 标准参数
  - -help
  - -version
- -X参数（非标准参数）
  - -Xint
  - -Xcomp
- -XX参数（使用率较高）
  - -XX:newSize
  - -XX:+UseSerialGC

### 2.2、标准参数

jvm的标准参数，一般都很稳定，在未来的JVM版本中不会改变，可以使用 java -help 检索出所有标准参数

```shell
C:\Users\tangm>java -help
用法: java [-options] class [args...]
           (执行类)
   或  java [-options] -jar jarfile [args...]
           (执行 jar 文件)
其中选项包括:
    -client       选择 "client" VM
    -server       选择 "server" VM
                  默认 VM 是 client.

    -cp <目录和 zip/jar 文件的类搜索路径>
    -classpath <目录和 zip/jar 文件的类搜索路径>
                  用 ; 分隔的目录, JAR 档案
                  和 ZIP 档案列表, 用于搜索类文件。
    -D<名称>=<值>
                  设置系统属性
    -verbose:[class|gc|jni]
                  启用详细输出
    -version      输出产品版本并退出
    -showversion  输出产品版本并继续
    -? -help      输出此帮助消息
    -X            输出非标准选项的帮助
    -ea[:<packagename>...|:<classname>]
    -enableassertions[:<packagename>...|:<classname>]
                  按指定的粒度启用断言
    -da[:<packagename>...|:<classname>]
    -disableassertions[:<packagename>...|:<classname>]
                  禁用具有指定粒度的断言
    -esa | -enablesystemassertions
                  启用系统断言
    -dsa | -disablesystemassertions
                  禁用系统断言
    -agentlib:<libname>[=<选项>]
                  加载本机代理库 <libname>, 例如 -agentlib:hprof
                  另请参阅 -agentlib:jdwp=help 和 -agentlib:hprof=help
    -agentpath:<pathname>[=<选项>]
                  按完整路径名加载本机代理库
    -javaagent:<jarpath>[=<选项>]
                  加载 Java 编程语言代理, 请参阅 java.lang.instrument
    -splash:<imagepath>
                  使用指定的图像显示启动屏幕
```

### 2.3、-X参数

### 2.4、-XX参数

### 2.5、-Xms与-Xmx参数

### 2.6、查看jvm的运行参数

## 2、jvm内存模型

## 3、内存分析工具

### 3.1、jmap

### 3.2、arthas

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

