## 1. Spring

## 2. Spring Boot

### 对Spring Boot理解

SpringBoot是一个快速开发框架，快速的将一些常用的第三方依赖整合（原理：通过Maven子父工程的方式），简化XML配置，全部采用注解形式，内置Http服务器（Jetty和Tomcat），最终以java应用程序进行执行，它是为了简化Spring应用的创建、运行、调试、部署等而出现的，使用它可以做到专注于Spring应用的开发，而无需过多关注XML的配置。

### Spring Boot 启动流程

```java
public ConfigurableApplicationContext run(String... args) {
    StopWatch stopWatch = new StopWatch();
    stopWatch.start();
    ConfigurableApplicationContext context = null;
    Collection<springbootexceptionreporter> exceptionReporters = new ArrayList&lt;&gt;();
    //设置系统属性『java.awt.headless』，为true则启用headless模式支持
    configureHeadlessProperty();
    //通过*SpringFactoriesLoader*检索*META-INF/spring.factories*，
       //找到声明的所有SpringApplicationRunListener的实现类并将其实例化，
       //之后逐个调用其started()方法，广播SpringBoot要开始执行了
    SpringApplicationRunListeners listeners = getRunListeners(args);
    //发布应用开始启动事件
    listeners.starting();
    try {
    //初始化参数
      ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
      //创建并配置当前SpringBoot应用将要使用的Environment（包括配置要使用的PropertySource以及Profile）,
        //并遍历调用所有的SpringApplicationRunListener的environmentPrepared()方法，广播Environment准备完毕。
      ConfigurableEnvironment environment = prepareEnvironment(listeners, applicationArguments);
      configureIgnoreBeanInfo(environment);
      //打印banner
      Banner printedBanner = printBanner(environment);
      //创建应用上下文
      context = createApplicationContext();
      //通过*SpringFactoriesLoader*检索*META-INF/spring.factories*，获取并实例化异常分析器
      exceptionReporters = getSpringFactoriesInstances(SpringBootExceptionReporter.class,
          new Class[] { ConfigurableApplicationContext.class }, context);
      //为ApplicationContext加载environment，之后逐个执行ApplicationContextInitializer的initialize()方法来进一步封装ApplicationContext，
        //并调用所有的SpringApplicationRunListener的contextPrepared()方法，【EventPublishingRunListener只提供了一个空的contextPrepared()方法】，
        //之后初始化IoC容器，并调用SpringApplicationRunListener的contextLoaded()方法，广播ApplicationContext的IoC加载完成，
        //这里就包括通过**@EnableAutoConfiguration**导入的各种自动配置类。
      prepareContext(context, environment, listeners, applicationArguments, printedBanner);
      //刷新上下文
      refreshContext(context);
      //再一次刷新上下文,其实是空方法，可能是为了后续扩展。
      afterRefresh(context, applicationArguments);
      stopWatch.stop();
      if (this.logStartupInfo) {
        new StartupInfoLogger(this.mainApplicationClass).logStarted(getApplicationLog(), stopWatch);
      }
      //发布应用已经启动的事件
      listeners.started(context);
      //遍历所有注册的ApplicationRunner和CommandLineRunner，并执行其run()方法。
        //我们可以实现自己的ApplicationRunner或者CommandLineRunner，来对SpringBoot的启动过程进行扩展。
      callRunners(context, applicationArguments);
    }
    catch (Throwable ex) {
      handleRunFailure(context, ex, exceptionReporters, listeners);
      throw new IllegalStateException(ex);
    }

    try {
    //应用已经启动完成的监听事件
      listeners.running(context);
    }
    catch (Throwable ex) {
      handleRunFailure(context, ex, exceptionReporters, null);
      throw new IllegalStateException(ex);
    }
    return context;
  }
```

1. 配置属性 > 2. 获取监听器，发布应用开始启动事件 > 3. 初始化输入参数 > 4. 配置环境，输出 banner > 5. 创建上下文 > 6. 预处理上下文 > 7. 刷新上下文 > 8. 再刷新上下文 > 9. 发布应用已经启动事件 > 10. 发布应用启动完成事件

1. 配置Headless属性：Headless模式是在缺少显示屏、键盘或者鼠标时候的系统配置
2. 获取监听器：在文件META-INF\spring.factories中获取SpringApplicationRunListener接口的实现类EventPublishingRunListener，主要发布SpringApplicationEvent
3. 准备环境变量，包括系统变量，环境变量，命令行参数，默认变量，servlet相关配置变量，随机值及配置文件（比如application.properties）等
4. 控制台打印springboot的bannner标志
5. 根据不同类型环境创建不同类型的applicationcontext容器，如果是servlet环境，创建的就是AnnotationConfigServletWebServerApplicatonContext容器对象
6. 从spring.factories配置文件中加载FailureAnalyZers对象，用来报告springboot启动过程中的异常
7. 为刚创建的容器对象做一些初始化工作，准备一些容器属性值等，对ApplicationContext应用一些相关的后置处理和调用各个ApplicationContextInitializer的初始化方法来执行一些初始逻辑等
8. 刷新容器，这一步至关重要。比如调用bean factory的后置处理器，注册BeanPostProcessor后置处理器，初始化事件广播器且广播事件，初始化剩下的单例bean和springboot创建内嵌的Tomcat服务器等重要且复杂的逻辑都在这里实现
9. 执行刷新新容器后的后置处理逻辑，注意这里为空方法
10. 调用ApplicationRunner和CommandLineRunner的run方法，我们实现这两个接口可以在spring容器启动后需要的一些东西 比如加载一些业务数据等
11. 报告启动异常，即若启动过程中抛出异常，此时哟个FailureAnalyzers来报告异常
12. 最终返回容器对象，这里调用方法没有声明对象来接收

### Spring Boot 自动装配

#### 一）概念

SpringBoot 定义了一套接口规范，这套规范规定：SpringBoot 在启动时会扫描外部引用 jar 包中的`META-INF/spring.factories`文件，将文件中配置的类型信息加载到 Spring 容器（此处涉及到 JVM 类加载机制与 Spring 的容器知识），并执行类中定义的各种操作。对于外部 jar 来说，只需要按照 SpringBoot 定义的标准，就能将自己的功能装置进 SpringBoot。

#### 二）实现原理

@SpringBootApplication三个核心注解

- `@EnableAutoConfiguration`：启用 SpringBoot 的自动配置机制（是实现自动装配的重要注解）
- `@Configuration`：允许在上下文中注册额外的 bean 或导入其他配置类
- `@ComponentScan`： 扫描被`@Component`注解的 bean，注解默认会扫描启动类所在的包下所有的类

1.@SpringBootApplication --->@EnableAutoConfiguration ---> @Import(AutoConfigurationImportSelector.class)

2.AutoConfigurationImportSelector 类实现了 ImportSelector 接口，也就实现了这个接口中的 selectImports 方法，该方法主要用于获取所有符合条件的类的全限定类名，这些类需要被加载到 IoC 容器中。

```java
private static final String[] NO_IMPORTS = new String[0];
public String[] selectImports(AnnotationMetadata annotationMetadata) {
        // <1>.判断自动装配开关是否打开
        if (!this.isEnabled(annotationMetadata)) {
            return NO_IMPORTS;
        } else {
          //<2>.获取所有需要装配的bean
            AutoConfigurationMetadata autoConfigurationMetadata = AutoConfigurationMetadataLoader.loadMetadata(this.beanClassLoader);
            AutoConfigurationImportSelector.AutoConfigurationEntry autoConfigurationEntry = this.getAutoConfigurationEntry(autoConfigurationMetadata, annotationMetadata);
            return StringUtils.toStringArray(autoConfigurationEntry.getConfigurations());
        }
    }
```

selectImprts ---> getAutoConfigurationEntry()方法---> getCandidateConfiguration方法

--->SpringFactoriesLoader的loadFactoryNames方法

得到所有的spring.factories内的配置内容，告知spring相关的配置类,加载对应的bean

1. 判断自动装配开关是否打开。默认 spring.boot.enableautoconfiguration=true，可在 application 中设置
2. 获取 EnableAutoConfiguration 注解中的 exclude 和 excludeName
3. 获取需要自动装配的所有配置类，读取 META-INF/spring.factories
4. 只有配置类中@ConditionalOnXXX 中的所有条件都满足，该类才会被装配

#### 三）实现一个Starter

1. 创建 xxx-spring-boot-starter 工程，引入 Spring Boot 相关依赖
2. 创建 xxxConfiguration，加@Configuration注解
3. 在 xxx-spring-boot-starter 工程的 resources 包下创建 META-INF/spring.factories 文件

**SPI 服务发现机制**

SPI ，全称为 Service Provider Interface(服务提供者接口)，是一种服务发现机制。SPI 的本质是将接口实现类的全限定名配置在文件中，并由服务加载器读取配置文件，加载实现类。这样可以在运行时，动态为接口替换实现类。

1. Service provider提供Interface的具体实现后，在目录META-INF/services下的文件(以Interface全路径命名)中添加具体实现类的全路径名；
2. 接口实现类的jar包存放在使用程序的classpath中；
3. 使用程序使用ServiceLoader动态加载实现类(根据目录META-INF/services下的配置文件找到实现类的全限定名并调用classloader来加载实现类到JVM);
4. SPI的实现类必须具有无参数的构造方法。

### Spring Boot 核心注解

- **@SpringBootConfiguration**

  组合了 @Configuration 注解，实现配置文件的功能。

- **@EnableAutoConfiguration**

  打开自动配置的功能，也可以关闭某个自动配置的选项，如关闭数据源自动配置功能： @SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })。

- **@ComponentScan**

  Spring组件扫描。

### IOC与AOP

**ioc是什么，有什么用？**

依赖倒置原则：
a.高层模块不应该依赖于底层模块，二者都应该依赖于抽象。
b.抽象不应该依赖于细节，细节应该依赖于抽象。

概念：

资源不由使用资源的双方管理，而由不使用资源的第三方管理，这可以带来很多好处。第一，资源集中管理，实现资源的可配置和易管理。第二，降低了使用资源双方的依赖程度。

**bean作用域有哪些，说一下各种使用场景？**

单例（Singleton）：在整个应用中，只创建bean的一个实例；
原型（Prototype）：每次注入或者通过Spring上下文获取的时候，都会创建一个新的bean实例；
会话（Session）：在Web应用中，为每个会话创建一个bean实例；
请求（Request）：在Web应用中，为每次请求创建一个bean实例；

**aop是什么，有哪些实现方式？**

```text
面向切面编程，通过预编译方式和运行期动态代理实现程序功能的统一维护的一种技术
```

- Aspect（切面）： Aspect 声明类似于 Java 中的类声明，在 Aspect 中会包含着一些 Pointcut 以及相应的 Advice。
- Joint point（连接点）：表示在程序中明确定义的点，典型的包括方法调用，对类成员的访问以及异常处理程序块的执行等等，它自身还可以嵌套其它 joint point。
- Pointcut（切点）：表示一组 joint point，这些 joint point 或是通过逻辑关系组合起来，或是通过通配、正则表达式等方式集中起来，它定义了相应的 Advice 将要发生的地方。
- Advice（增强）：Advice 定义了在 Pointcut 里面定义的程序点具体要做的操作，它通过 before、after 和 around 来区别是在每个 joint point 之前、之后还是代替执行的代码。
- Target（目标对象）：织入 Advice 的目标对象。
- Weaving（织入）：将 Aspect 和其他对象连接起来, 并创建 Adviced object 的过程

实现方式：

1. 为什么不直接都使用JDK动态代理：JDK动态代理只能代理接口类，所以很多人设计架构的时候会使用XxxService, XxxServiceImpl的形式设计，一是让接口和实现分离，二是也有助于代理。
2. 为什么不都使用Cgilb代理：因为JDK动态代理不依赖其他包，Cglib需要导入ASM包，对于简单的有接口的代理使用JDK动态代理可以少导入一个包。

**拦截器是什么，什么场景使用？**

Servlet中的过滤器Filter是实现了javax.servlet.Filter接口的服务器端程序，主要的用途是过滤字符编码、做一些业务逻辑判断等。其工作原理是，只要你在web.xml文件配置好要拦截的客户端请求，它都会帮你拦截到请求，此时你就可以对请求或响应(Request、Response)统一设置编码，简化操作；同时还可进行逻辑判断，如用户是否已经登陆、有没有权限访问该页面等等工作

**aop里面的cglib原理是什么？**

参考前面反射

**aop切方法的方法的时候，哪些方法是切不了的？为什么？**

### 其他重要注解

#### Transactional

使用范围

- 作用于类：放在类上，表示所有该类的public方法都配置相同的事务属性信息。
- 作用于方法：当类配置了@Transactional，方法也配置了@Transactional，方法的事务会覆盖类的事务配置信息。
- 作用于接口：不推荐，因为一旦标注在Interface上并且配置了Spring AOP 使用CGLib动态代理，将会导致@Transactional注解失效

原理

- 声明式事务管理建立在AOP之上的。其本质是对方法前后进行拦截，然后在目标方法开始之前创建或者加入一个事务，在执行完目标方法之后根据执行情况提交或者回滚事务

常用属性

- propagation 代表事务的传播行为，默认值为 Propagation.REQUIRED
  - Propagation.REQUIRED：如果当前存在事务，则加入该事务，如果当前不存在事务，则创建一个新的事务。
  - Propagation.SUPPORTS：如果当前存在事务，则加入该事务；如果当前不存在事务，则以非事务的方式继续运行。
  - Propagation.MANDATORY：如果当前存在事务，则加入该事务；如果当前不存在事务，则抛出异常。
  - Propagation.REQUIRES_NEW：重新创建一个新的事务，如果当前存在事务，暂停当前的事务。**(** 当类A中的 a 方法用默认Propagation.REQUIRED模式，类B中的 b方法加上采用 Propagation.REQUIRES_NEW模式，然后在 a 方法中调用 b方法操作数据库，然而 a方法抛出异常后，b方法并没有进行回滚，因为Propagation.REQUIRES_NEW会暂停 a方法的事务 **)**
  - Propagation.NOT_SUPPORTED：以非事务的方式运行，如果当前存在事务，暂停当前的事务。
  - Propagation.NEVER：以非事务的方式运行，如果当前存在事务，则抛出异常。
  - Propagation.NESTED ：和 Propagation.REQUIRED 效果一样。
- isolation ：事务的隔离级别，默认值为 Isolation.DEFAULT
- timeout ：事务的超时时间，默认值为 -1。如果超过该时间限制但事务还没有完成，则自动回滚事务。
- readOnly ：指定事务是否为只读事务，默认值为 false
- rollbackFor ：用于指定能够触发事务回滚的异常类型，可以指定多个异常类型
- noRollbackFor：抛出指定的异常类型，不回滚事务，也可以指定多个异常类型

失效场景

- 应用在非 public 修饰的方法上
- 属性 propagation 设置错误
- 属性 rollbackFor 设置错误
- 同一个类中方法调用，导致@Transactional失效
- 异常被catch捕获导致@Transactional失效
- 数据库引擎不支持事务

#### PathVariable 

当使用@RequestMapping URI template 样式映射时， 即 someUrl/{paramId}, 这时的paramId可通过 @Pathvariable注解绑定它传过来的值到方法的参数上。

## 2.Spring Cloud

### 微服务中主要框架

|                  | Spring Cloud        | Spring Cloud Alibaba |
| ---------------- | ------------------- | -------------------- |
| 服务注册与发现   | Eureka、Consul      | Nacos                |
| 分布式配置中心   | Spring Cloud Config | Nacos                |
| 客户端负载均衡   | Ribbon              | Ribbon               |
| 断路器           | Hystrix             | Sentinel             |
| API网关          | Netflix Zuul        | Gateway              |
| 分布式事务一致性 | --                  | Seata                |
| 分布式链路跟踪   | Sleuth、Zipkin      | SkyWalking           |

#### 1. 服务注册与发现

|            | Nacos                      | Eureka             | Consul            | Zookeeper  |
| ---------- | -------------------------- | ------------------ | ----------------- | ---------- |
| 一致性协议 | 支持 AP/CP 切换            | AP                 | CP                | CP         |
| 健康检查   | TCP/HTTP/MYSQL/Client Beat | Client Beat        | TCP/HTTP/gRPC/Cmd | Keep Alive |
| 访问协议   | HTTP/DNS                   | HTTP               | HTTP/DNS          | TCP        |
| K8S集成    | 支持                       | 不支持             | 支持              | 不支持     |
| 迭代       | 迭代                       | 目前已经不进行升级 | 迭代              | 迭代       |

##### 1.1 Zookeeper

临时节点：与客户端会话绑定，一旦会话失效，这个客户端所创建的所有临时节点都会被移除；

zookeeper 提供了分布式数据的发布/订阅功能，允许客户端向服务端注册一个 watcher 监听节点变更

##### 1.2 Nacos



#### 2.分布式配置中心

|              | Nacos                  | Spring Cloud Config      | Apollo                 |
| ------------ | ---------------------- | ------------------------ | ---------------------- |
| 配置实时推送 | 支持（Http长轮询1s内） | 支持（Spring Cloud Bus） | 支持（Http长轮询1s内） |
| 多语言       | 支持，提供了Open API   | 只支持java               | 支持，提供了Open API   |
| 配置回滚     | 支持                   | 支持                     | 支持                   |
| 版本管理     | 支持                   | 支持                     | 支持                   |
| 配置格式校验 | 支持                   | 不支持                   | 支持                   |

#### 3.客户端负载均衡

IRule接口是所有负载均衡策略的父接口，里面核心是choose方法，用来选择一个服务实例

AbstractLoadBalancerRule

使用LoadBalancer替换Ribbon

```xml
 <dependency>
     <groupId>com.alibaba.cloud</groupId>
     <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
     <exclusions>
          <exclusion>
              <groupId>org.springframework.cloud</groupId>
              <artifactId>spring-cloud-starter-netflix-ribbon</artifactId>
          </exclusion>
      </exclusions>
</dependency>
<dependency>
     <groupId>org.springframework.cloud</groupId>
     <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```



#### 4.限流与容灾

##### 限流

限流的主要作用是损失一部分用户可用性，为大部分用户提供稳定可靠的服务。

方式：

- 计算器算法：指定周期内累加访问次数，当访问次数到达阈值时，触发限流策略。缺点：临界问题
- 滑动窗口算法：在固定窗口中分割出多个小时间窗口，分别在每个小时间窗口中记录访问次数，然后根据时间将窗口往前滑动删除过期的小时间窗口。最终只需要统计滑动窗口范围内的所有小时间窗口总的计数即可。
- 令牌桶限流算法：系统以恒定速度往令牌桶中放入令牌，如果此时有客户端请求过了，需要先从令牌桶中拿到令牌
- 漏桶限流算法：与令牌桶原理相差不大,区别是漏桶无法处理短时间内的突发流量。

##### 熔断与降级

一旦某个链路上被依赖的服务不可用，很可能出现请求堆积从而导致服务雪崩。

服务熔断是指当某个服务提供者无法正常提供服务时，暂时将出现故障的接口隔离出来，断绝与外部接口的联系，当触发熔断后，持续一段时间内该服务调用的请求直接失败，知道目标服务恢复正常

|              | Sentinel                             | Hystrix               |
| ------------ | ------------------------------------ | --------------------- |
| 隔离策略     | 基于并发数                           | 线程池隔离/信号量隔离 |
| 熔断降级策略 | 基于响应时间或失败比率               | 基于失败比率          |
| 实时指标实现 | 滑动窗口                             | 滑动窗口              |
| 限流         | 基于QPS/并发数，支持基于调用关系限流 | 不支持                |
| 负载保护     | 支持                                 | 不支持                |



#### 5.API网关



#### 6.分布式事务一致性



#### 7.分布式链路跟踪

##### SkyWalking

为了解决不同的分布式追踪系统 API 不兼容的问题，诞生了 OpenTracing 规范

调用链标准 OpenTracing 的数据模型，主要有以下三个

- **Trace**：一个完整请求链路
- **Span**：一次调用过程(需要有开始时间和结束时间)
- **SpanContext**：Trace 的全局上下文信息, 如里面有traceId

skywalking以下主要问题解决方案：

1. 怎么**自动**采集 span 数据：自动采集，对业务代码无侵入：使用插件化+Agent形式实现自动采集
2. 如何跨进程传递 context：放到 Http 和 gRPC 中的Header中，不影响业务
3. traceId 如何保证全局唯一：SnowFlake算法（雪花算法）
4. 请求量这么多采集会不会影响性能
   - SkyWalking 默认设置了 3 秒采样 3 次，其余请求不采样
   - 如果上游有携带 Context 过来(说明上游采样了)，则下游**强制**采集数据。这样可以保证链路完整。

**.Net core探针原理**

DiagnosticSource 实现了一个消息的生产者消费者模型，在某个地方触发消息，然后可以在任意地方接收。微软在很多官方库里都预留了性能打点，例如：HttpContext、HttpClient、SqlClient、EntityFrameworkCore等，还有gRPC、CAP、SmartSql等一些第三方库等也都提前留了打点。它们在开始做某件事、做完某件事、做错某件事的时候，都会对进程内触发一个消息，让我们可以通过 DiagnosticSource 消费到这个消息，然后就可以用它来记录某次事件的具体历史了，便是实现了tracing。

**Java探针原理**

Java探针使用了 Instrumentation与字节码生成框架Byte Buddy

Java Agent 本身就是 java 命令的一个参数（即 -javaagent）。-javaagent 参数之后需要指定一个 jar 包，这个 jar 包需要同时满足下面两个条件：

1. 在 META-INF 目录下的 MANIFEST.MF 文件中必须指定 premain-class 配置项。
2. premain-class 配置项指定的类必须提供了 premain() 方法。在 Java 虚拟机启动时，执行 main() 函数之前，虚拟机会先找到 -javaagent 命令指定 jar 包，然后执行premain-class 中的 premain() 方法。

使用 Java Agent 的步骤大致如下：

1. 定义一个 MANIFEST.MF 文件，在其中添加 premain-class 配置项。
2. 创建 premain-class 配置项指定的类，并在其中实现 premain() 方法
3. 将 MANIFEST.MF 文件和 premain-class 指定的类一起打包成一个 jar 包
4. 用 -javaagent 指定该 jar 包的路径即可执行其中的 premain() 方法