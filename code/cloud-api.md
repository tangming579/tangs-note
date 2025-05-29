## 注解

### @ConditionalOnExpression

```
@ConditionalOnExpression("'${tmf.project-impl}'.equals('jdbc')")
```

当括号中的内容为true时，使用该注解的类被实例化

### @Before, @BeforeClass

| **特性**           | **Junit 4**  | **Junit 5** |
| ------------------ | ------------ | ----------- |
| 注解在静态方法上   | @BeforeClass | @BeforeAll  |
| 注解在静态方法上   | @AfterClass  | @AfterAll   |
| 注解在非静态方法上 | @Before      | @BeforeEach |
| 注解在非静态方法上 | @After       | @AfterEach  |

![enter image description here](https://i.stack.imgur.com/HKspz.png)

## 类库

### kubernetes-client

fabric8io

```xml
<dependency>
    <groupId>io.fabric8</groupId>
    <artifactId>kubernetes-client-bom</artifactId>
    <version>${fabric8io.kubernetes-client.version}</version>
    <type>pom</type>
    <scope>import</scope>
</dependency>
```

### elasticsearch-client

elasticsearch-rest-high-level-client

```xml
<dependency>
    <groupId>org.elasticsearch</groupId>
    <artifactId>elasticsearch</artifactId>
    <version>6.8.12</version>
</dependency>
<dependency>
    <groupId>org.elasticsearch.client</groupId>
    <artifactId>elasticsearch-rest-high-level-client</artifactId>
    <version>6.8.12</version>
</dependency>
```



### powermock

```xml
<!-- 单元测试，参考skywalking -->
<dependency>
    <groupId>org.powermock</groupId>
    <artifactId>powermock-module-junit4</artifactId>
    <version>${powermock.version}</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.powermock</groupId>
    <artifactId>powermock-api-mockito2</artifactId>
    <version>${powermock.version}</version>
    <scope>test</scope>
</dependency>
```

#### mock

> T PowerMockito.mock(Class clazz);

可以用于模拟指定类的对象实例。

当模拟非final类（接口、普通类、虚基类）的非final方法时，不必使用@RunWith和@PrepareForTest注解。否则必须使用

#### spy

如果一个对象，我们只希望模拟它的部分方法，而希望其它方法跟原来一样，可以使用PowerMockito.spy方法代替PowerMockito.mock方法。

通过when语句设置过的方法，调用的是模拟方法；而没有通过when语句设置的方法，调用的是原有方法。

#### ArgumentCaptor

- argument.capture() 捕获方法参数
- argument.getValue() 获取方法参数值，如果方法进行了多次调用，它将返回最后一个参数值
- argument.getAllValues() 方法进行多次调用后，返回多个参数值

举例：

```java
@Test  
public void argumentCaptorTest() {  
    List mock = mock(List.class);  
    List mock2 = mock(List.class);  
    mock.add("John");  
    mock2.add("Brian");  
    mock2.add("Jim");      
    ArgumentCaptor argument = ArgumentCaptor.forClass(String.class);    
    verify(mock).add(argument.capture());  
    assertEquals("John", argument.getValue());  
    verify(mock2, times(2)).add(argument.capture());  
    assertEquals("Jim", argument.getValue());  
    assertArrayEquals(new Object[]{"Brian","Jim"},argument.getAllValues().toArray());  
}  
```

#### verifyNoMoreInteractions

所有的调用都得到了验证，即 mock 所有调用的方法都通过verify进行了验证

#### Whitebox.setInternalState

设置私有属性值。

### jacoco

它是一个开源的覆盖率工具([官网地址](https://www.eclemma.org/jacoco/) )，针对的开发语言是java，Maven中；可以使用其JavaAgent技术监控Java程序等等，很多第三方的工具提供了对JaCoCo的集成，如 Jenkins 等。

## 其他

#### map.computeIfAbsent

对 hashMap 中指定 key 的值进行重新计算，如果不存在这个 key，则添加到 hashMap 中。

#### CompletableFuture.supplyAsync

该任务将在`ForkJoinPool.commonPool()`中异步完成运行，最后，`supplyAsync()`将返回新的`CompletableFuture`，其值是通过调用给定的Supplier所获得的值。

#### parallelStream

parallelStream其实就是一个并行执行的流.它通过默认的ForkJoinPool,可能提高你的多线程任务的速度.

#### Collections.synchronizedList

实现List的线程安全