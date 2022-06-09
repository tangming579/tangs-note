## 注解

### @ConditionalOnExpression

```
@ConditionalOnExpression("'${tmf.project-impl}'.equals('jdbc')")
```

当括号中的内容为true时，使用该注解的类被实例化

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

### jacoco

## 其他

### dependencyManagement