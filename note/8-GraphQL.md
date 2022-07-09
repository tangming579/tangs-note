GraphQL 是一个用于 API 的查询语言，是一个使用基于类型系统来执行查询的服务端运行时

|          | Rest                                   | GraphQL                                      |
| -------- | -------------------------------------- | -------------------------------------------- |
| 请求路径 | 不同接口不同，是访问资源的唯一标识     | 同一个地址，类似：http://[ip]:[port]/graphql |
| 状态码   | 通过状态码表示请求结果                 | 不管成不成功都是200                          |
| 请求参数 | 放到 url 中或 body 的 json 中          | 分为query和 graphql variables                |
| 返回期望 | 数据冗余也只能全部接收再对数据进行处理 | 给客户端自主选择数据内容的能力               |
| 接口调试 | 可以给前端提供 Swagger 页面            | 通常使用 PostMan                             |



### 数据类型

对象类型、标量以及枚举是 GraphQL 中你唯一可以定义的类型种类

#### 对象类型

一个 GraphQL schema 中的最基本的组件是对象类型，它就表示你可以从服务上获取到什么类型的对象，以及这个对象有什么字段

```
type Character {
  name: String!
  appearsIn: [Episode!]!
}
```

- `Character` 是一个 **GraphQL 对象类型**，表示其是一个拥有一些字段的类型。
- `name` 和 `appearsIn` 是 `Character` 类型上的**字段**。
- `String` 是内置的**标量**类型之一 
- `String!` 表示这个字段是**非空的**，在类型语言里面，我们用一个感叹号来表示这个特性。
- `[Episode!]!` 表示一个 `Episode` **数组**。

#### 标量

GraphQL 自带一组默认标量类型：

- `Int`：有符号 32 位整数。
- `Float`：有符号双精度浮点值。
- `String`：UTF‐8 字符序列。
- `Boolean`：`true` 或者 `false`。
- `ID`：ID 标量类型表示一个唯一标识符，通常用以重新获取对象或者作为缓存中的键。

#### 枚举

``` 
enum Episode {
  NEWHOPE
  EMPIRE
  JEDI
}
```

### 参数

GraphQL 对象类型上的每一个字段都可能有零个或者多个参数，例如下面的 `length` 字段：

```graphql
type Starship {
  id: ID!
  name: String!
  length(unit: LengthUnit = METER): Float
}
```

`length` 字段定义了一个参数，`unit`。默认值设置为 `METER`