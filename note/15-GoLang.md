## 基本

## 协程

## Gin

### 路由原理

httprouter是一个高性能路由分发器，它负责将不同方法的多个路径分别注册到各个handle函数，当收到请求时，负责快速查找请求的路径是否有相对应的处理函数，并且进行下一步业务逻辑处理。golang的gin框架采用了httprouter进行路由匹配，httprouter 是通过radix tree来进行高效的路径查找；同时路径还支持两种通配符匹配。

httprouter会对每种http方法（post、get等）都会生成一棵基数树，

```go
type Router struct {
    // 这个radix tree是最重要的结构
    // 按照method将所有的方法分开, 然后每个method下面都是一个radix tree
    trees map[string]*node

    // 当/foo/没有匹配到的时候, 是否允许重定向到/foo路径
    RedirectTrailingSlash bool
    // 是否允许修正路径
    RedirectFixedPath bool
    // 如果当前无法匹配, 那么检查是否有其他方法能match当前的路由
    HandleMethodNotAllowed bool
    // 是否允许路由自动匹配options, 注意: 手动匹配的option优先级高于自动匹配
    HandleOPTIONS bool
    // 当no match的时候, 执行这个handler. 如果没有配置,那么返回NoFound
    NotFound http.Handler 
    // 当no natch并且HandleMethodNotAllowed=true的时候,这个函数被使用
    MethodNotAllowed http.Handler
    // panic函数
    PanicHandler func(http.ResponseWriter, *http.Request, interface{})
}
```

其中树节点node结构如下：

```go
type node struct {
    // 保存这个节点上的URL路径
    path      string
    // 判断当前节点路径是不是参数节点, 例如上图的:post部分就是wildChild节点
    wildChild bool
    // 节点类型包括static, root, param, catchAll
    // static: 静态节点, 例如上面分裂出来作为parent的s
    // root: 如果插入的节点是第一个, 那么是root节点
    // catchAll: 有*匹配的节点
    // param: 除上面外的节点
    nType     nodeType
    // 记录路径上最大参数个数
    maxParams uint8
    // 和children[]对应, 保存的是分裂的分支的第一个字符
    // 例如search和support, 那么s节点的indices对应的"eu"
    // 代表有两个分支, 分支的首字母分别是e和u
    indices   string
    // 保存孩子节点
    children  []*node
    // 当前节点的处理函数
    handle    Handle
    // 优先级, 看起来没什么卵用的样子@_@
    priority  uint32
}
```

gin路由

简单的来说每一个注册的 url 都会通过 / 切分为 n 个树节点（httprouter 会有一些区别，会存在根分裂），然后挂到相应 method 树上去，所以业务中有几种不同的 method 接口，就会产生对应的前缀树。在 httprouter 中，节点被分为 4 种类型：

-- static - 静态节点，/user /api 这种

-- root - 根结点

-- param - 参数节点 /user/{id}，id 就是一个参数节点

-- catchAll - 通配符

其实整个匹配的过程也比较简单，通过对应的 method 拿到前缀树，然后开始进行一个广度优先的匹配。

这里值得学习的一点是，httprouter 对下级节点的查找进行了优化，简单来说就是把当前节点的下级节点的首字母维护在本身，匹配时先进行索引的查找。

**注意：**

gin中相同http方法的路由树中，参数结点和静态结点是冲突的，也就是:

get 方法，如果存在了 /user/:name这样的路径，就不能再添加/user/getName这样的静态路径，否则会报冲突；

## GORM