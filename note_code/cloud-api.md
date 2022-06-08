### 注解

#### @ConditionalOnExpression

```
@ConditionalOnExpression("'${tmf.project-impl}'.equals('jdbc')")
```

当括号中的内容为true时，使用该注解的类被实例化

### 类库

#### powermock

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



### 其他

#### dependencyManagement