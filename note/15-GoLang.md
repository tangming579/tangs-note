## 基础

**new 和 make 区别**

- make 只能用来分配及初始化类型为 slice、map、chan 的数据。new 可以分配任意类型的数据；
- new 分配返回的是指针，即类型 *Type。make 返回引用，即 Type；
- new 分配的空间被清零。make 分配空间后，会进行初始化；

**for range 时地址会发生变化吗**

 for a,b := range c 遍历中， a 和 b 在内存中只会存在一份，即之后每次循环时遍历到的数据都是以值覆盖的方式赋给 a 和 b，a，b 的内存地址始终不变。由于有这个特性，for 循环里面如果开协程，不要直接把 a 或者 b 的地址传给协程。解决办法：在每次循环时，创建一个临时变量。

## 数据结构

### Slice

**数组和切片的区别**

- 数组是定长，访问和复制不能超过数组定义的长度，否则就会下标越界，切片长度和容量可以自动扩容
- 数组是值类型，切片是引用类型，每个切片都引用了一个底层数组

**slices能作为map类型的key吗**

可比较的类型都可以作为map key，不能作为map key 的类型包括：slices、maps、functions

### Map

map默认是无序的，保证遍历顺序使用 orderedmap，保证并发sync.Map

**map 中删除一个 key，它的内存会释放吗？**

如果删除的元素是值类型，如int，float，bool，string以及数组和struct，map的内存不会自动释放

如果删除的元素是引用类型，如指针，slice，map，chan等，map的内存会自动释放，但释放的内存是子元素应用类型的内存占用

将map设置为nil后，内存被回收。

### context

应用：上下文控制、多个 goroutine 之间的数据交互等、超时控制：到某个时间点超时，过多久超时，

- Deadline 方法返回一个 time.Time，表示当前 Context 应该结束的时间
- ok 表示有结束时间
- Done 方法当 Context 被取消或者超时时候返回的一个 close 的 channel，告诉给 context 相关的函数要停止当前工作然后返回了
- Err 表示 context 被取消的原因，Value 方法表示 context 实现共享数据存储的地方，是协程安全的。

## 协程

### channel

**defer**

- defer延迟函数，释放资源，收尾工作；如释放锁，关闭文件，关闭链接；捕获panic;
- defer函数紧跟在资源打开后面，否则defer可能得不到执行，导致内存泄露。
- 多个 defer 调用顺序是 LIFO（后入先出），defer后的操作可以理解为压入栈中

**select**

go 的 select 为 golang 提供了多路 IO 复用机制，用于检测是否有读写事件是否 ready。linux 的系统 IO 模型有 select，poll，epoll，go 的 select 和 linux 系统 select 非常相似，特点：

- select 操作至少要有一个 case 语句，出现读写 nil 的 channel 该分支会忽略，在 nil 的 channel 上操作则会报错。
- select 仅支持管道，而且是单协程操作。
- 每个 case 语句仅能处理一个管道，要么读要么写。
- 多个 case 语句的执行顺序是随机的。
- 存在 default 语句，select 将不会阻塞，但是存在 default 会影响

### Mutex



### GPM

**进程、线程、协程**

- 进程：是应用程序的启动实例，每个进程都有独立的内存空间，不同的进程通过进程间的通信方式来通信。
- 线程：从属于进程，每个进程至少包含一个线程，线程是 CPU 调度的基本单位，多个线程之间可以共享进程的资源并通过共享内存等线程间的通信方式来通信。
- 协程：为轻量级线程，与线程相比，协程不受操作系统的调度，协程的调度器由用户应用程序提供，协程调度器按照调度策略把协程调度到线程中运行

## GC

### 收集算法

Go 的 GC 回收有三次演进过程：

- Go V1.3 之前：普通标记清除（mark and sweep）方法，整体过程需要启动 STW，效率极低。
- GoV1.5：三色标记法，堆空间启动写屏障，栈空间不启动，全部扫描之后，需要重新扫描一次栈(需要 STW)，效率普通。
- GoV1.8：三色标记法，混合写屏障机制：栈空间不启动（全部标记成黑色），堆空间启用写屏障，整个过程不要 STW，效率高。

### 内存逃逸

本该分配到栈上的变量，跑到了堆上，这就导致了内存逃逸。2)栈是高地址到低地址，栈上的变量，函数结束后变量会跟着回收掉，不会有额外性能的开销。3)变量从栈逃逸到堆上，如果要回收掉，需要进行 gc，那么 gc 一定会带来额外的性能开销。编程语言不断优化 gc 算法，主要目的都是为了减少 gc 带来的额外性能开销，变量一旦逃逸会导致性能开销变大。

**内存逃逸的情况如下：**

1）方法内返回局部变量指针。

2）向 channel 发送指针数据。

3）在闭包中引用包外的值。

4）在 slice 或 map 中存储指针。

5）切片（扩容后）长度太大。

6）在 interface 类型上调用方法。

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

其他