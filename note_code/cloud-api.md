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

```
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

```
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

```
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



### jacoco

## 其他

### dependencyManagement