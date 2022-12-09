## Skywalking-2-GraphQL

## 1. 基本概念

GraphQL 是一个用于 API 的查询语言，是一个使用基于类型系统来执行查询的服务端运行时

GraphQL的核心包括Query、Mutation、Schemas等

|          | Rest                                   | GraphQL                                      |
| -------- | -------------------------------------- | -------------------------------------------- |
| 请求路径 | 不同接口不同，是访问资源的唯一标识     | 同一个地址，类似：http://[ip]:[port]/graphql |
| 状态码   | 通过状态码表示请求结果                 | 不管成不成功都是200                          |
| 请求参数 | 放到 url 中或 body 的 json 中          | 分为query和 graphql variables                |
| 返回期望 | 数据冗余也只能全部接收再对数据进行处理 | 给客户端自主选择数据内容的能力               |
| 接口调试 | 可以给前端提供 Swagger 页面            | 通常使用 Postman                             |

### 1.1 数据类型（Object Types and Fields）

对象类型、标量以及枚举是 GraphQL 中唯一可以定义的类型种类

#### 1.1.1 对象类型

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

#### 1.1.2 标量

GraphQL 自带一组默认标量类型：

- `Int`：有符号 32 位整数。
- `Float`：有符号双精度浮点值。
- `String`：UTF‐8 字符序列。
- `Boolean`：`true` 或者 `false`。
- `ID`：ID 标量类型表示一个唯一标识符，通常用以重新获取对象或者作为缓存中的键。

#### 1.1.3 枚举

``` 
enum Episode {
  NEWHOPE
  EMPIRE
  JEDI
}
```

### 1.2 参数（Arguments）

GraphQL 对象类型上的每一个字段都可能有零个或者多个参数，例如下面的 `length` 字段：

```graphql
type Starship {
  id: ID!
  name: String!
  length(unit: LengthUnit = METER): Float
}
```

`length` 字段定义了一个参数，`unit`。默认值设置为 `METER`

### 1.3 Schema

Schemas 描述了 数据的组织形态 以及服务器上的那些数据能够被查询，Schemas提供了数据中可用的数据的对象类型，GraphQL中的对象是强类型的，因此schema中定义的所有的对象必须具备类型。类型允许GraphQL服务器确定查询是否有效或者是否在运行时。Schemas可用是两种类型`Query`和`Mutation`。

`Schemas`用GraphQL schemas语言构建，在代码中以 `graphql` 或 `gql` 作为文件名后缀，用法如下

```
    type Author {
      name: String!
      posts: [Post]
    }
```

### 1.4 Query

`Query` 为 `graphql` 的入口查询处，我们可以并且只可以查询 `Query` 下的任意字段 (field)。因此，他组成了 `graphql` 最核心的功能： **查找你所需要的任何数据**。

`schema` 将会是服务端的主体，而 `query` 存在于前端中，类似 REST 中的 API。

```
    query GetAuthor($authorID: Int! = 5) {
      author(id: $authorID) {
        name
      }
    }
```

测试GraphQL接口可以使用Chrome浏览器的 `Altair Graphal Client`插件，也可以使用其他的客户端工具，如：graphql-playground

## 2. Java 中使用 GraphQL

参考：https://www.graphql-java.com/tutorials/getting-started-with-spring-boot/

### 2.1 期望接口

想要实现的查询：

```
{
  bookById(id: "book-1"){
    id
    name
    pageCount
    author {
      firstName
      lastName
    }
  }
}
```

期待返回结果

```json
{
  "bookById": {
    "id":"book-1",
    "name":"Harry Potter and the Philosopher's Stone",
    "pageCount":223,
    "author": {
      "firstName":"Joanne",
      "lastName":"Rowling"
    }
  }
}
```

### 2.2 实现步骤

1. 在 src/main/resources/graphql 目录下新建文件 schema.graphqls ，内容如下：

   ```
   type Query {
     bookById(id: ID): Book
   }
   
   type Book {
     id: ID
     name: String
     pageCount: Int
     author: Author
   }
   
   type Author {
     id: ID
     firstName: String
     lastName: String
   }
   ```

2. 创建类 bookDetails/Book.java 、bookDetails/Author.java

   ```
   public class Book {
       private String id;
       private String name;
       private int pageCount;
       private String authorId;
   	……
   }
   public class Author {
       private String id;
       private String firstName;
       private String lastName;
       ……
   }    
       
   ```

3. 引入类库

   ```
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-graphql</artifactId>
   </dependency>
   ```

4. 增加controller

   ```
   @Controller
   public class BookController {
       @QueryMapping
       public Book bookById(@Argument String id) {
           return Book.getById(id);
       }
   
       @SchemaMapping
       public Author author(Book book) {
           return Author.getById(book.getAuthorId());
       }
   }
   ```

## 3. skywalking中的GraphQL

项目位置：oap-server/server-query-plugin/query-graphql-plugin

pom依赖：

```
<dependency>
    <groupId>com.graphql-java</groupId>
    <artifactId>graphql-java</artifactId>
</dependency>
<dependency>
    <groupId>com.graphql-java</groupId>
    <artifactId>graphql-java-tools</artifactId>
</dependency>
<dependency>
    <groupId>com.google.code.gson</groupId>
    <artifactId>gson</artifactId>
</dependency>
```

- graphql-java-tools

   能够从GraphQL的模式定义 ***.graphqls** 文件构建出对应的Java的POJO类型对象（graphql-java-tools将读取classpath下所有以*.graphqls为后缀名的文件，创建GraphQLSchema对象），同时屏蔽了graphql-java的底层细节，它本身依赖graphql-java。

- graphql-spring-boot-starter

  辅助SpringBoot接入GraphQL的库，它本身依赖graphql-java和graphql-java-servlet（将GraphQL服务发布为通过HTTP可访问的Web服务，封装了一个GraphQLServlet接收GraphQL请求，并提供Servlet Listeners功能）。

类库的使用可以参考：https://github.com/graphql-java-kickstart

