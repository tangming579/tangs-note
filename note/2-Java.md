## 0. 基础

Java 8 新特性

- Interface
  - `default` ：在接口中定义方法实现，实现类自动拥有该方法实现
  -  `static`：在接口中定义方法实现，实现类不继承静态方法
  - 函数式接口：有且仅有一个抽象方法（可以有多个非抽象方法），可以被隐式转换为 lambda 表达式
- Lambda 表达式

- Stream：数据处理操作的源生成的字节序列，源可以是数组、文件、集合、函数。它不是数据结构并不保存数据，目的在于计算
- Optional
- 时间类增强：非线程安全、时区处理麻烦、各种格式化、时间计算繁琐

**为什么需要包装类**

1. 作为和基本数据类型对应的类类型存在，方便涉及到对象的操作（如空值，集合存储）。
2. 包含每种基本数据类型的相关属性如最大值、最小值等，以及相关的操作方法。

### 泛型

[参考](https://segmentfault.com/a/1190000039835272)

`Java`的泛型是在`1.5`引入的，只在**编译期**做泛型检查，**运行期**泛型就会消失，我们把这称为“泛型擦除”，类型擦除后保留原始类型。

原始类型就是擦除去了泛型信息，最后在字节码中的类型变量的真正类型，无论何时定义一个泛型，相应的原始类型都会被自动提供，类型变量擦除，并使用其限定类型(无限定的变量用Object)替换。

问：是否能够通过instanceof查询ArrayList的类限定类型（泛型信息）？

不能。

**Java协变和逆变**

- 协变：将父类保持了子类型的继承关系。通过协变实现子类型的泛型类型可以赋值给父类型泛型。遵守只读不写

  ```
  假设可以写入，即假设可以写入Number及其子类，此时有A，B都是T的子类，那么写入A后，进行获取时，由于泛型擦除是无法知道获取的是A还是B，也就不知道该强转成A还是B，所以会报错
  
  ArrayList<? extends Number> list1 = new ArrayList<Integer>();
  ```

- 逆变：逆转了子类型的关系。将父类型泛型赋值给子类型泛型。遵守只写不读

  ```
  假设可以进行读取，即读出来的是T或者其父类，同样由于泛型擦除没法确定读出来的具体类型，只能用Object接收，这样无任何意义
           
  ArrayList<? super Integer> list2 = new ArrayList<Number>();
  ```

**使用场景**

《Effective Java》给出精炼的描述：**producer-extends, consumer-super（PECS）**

从数据流来看，extends是限制数据来源的（**生产者**），而super是限制数据流入的（**消费者**）

**泛型反射**

利用反射来获取泛型的类型（泛型信息）

1．获取当前类

2．获取目标字段

3．获取包含泛型类型的类型 getGenericType()

4．强转至子类ParameterizedType因为Type没有任何对应的方法

5．获得泛型真正的类型 getActualTypeArguments()



> - getType()：返回一个 Class 对象，它标识了此 Field 对象所表示字段的声明类型。
> - getGenericType()：返回一个 Type 对象，它表示此 Field 对象所表示字段的声明类型。

```java
public class testgetGenericType{
	private Map<String, Number> collection;
    
    public static void main(String[] args){
        Class<?> clazz = TestgetGenericType.class;//取得class
        Field field = clazz.getDeclaredFiled("collection");//取得字段变量
        Type type = field.getGenericType();//取得泛型的类型
        ParameterizedType ptype = (ParameterizedType)type;//转成参数化类型
        System.out.printLn(ptype.getActualTypeArguments()[0]);
        System.out.printLn(ptype.getActualTypeArguments()[1]);
    }
}

//输出结果：
//class.java.lang.String
//class.java.lang.Number
```

## 1. 多线程

### 实现多线程方式

- 实现多线程方式
  - 继承Thread类，重写run函数
  - 实现Runnable接口
  - 实现Callable接口
- 三种方式区别
  - 实现Runnable接口可以避免java单继承特性带来的局限；增强程序的健壮性，代码能够被多个线程共享，代码与数据是独立的；适合多个相同程序代码的线程区处理同一资源的情况
  - 继承Thread和实现Runnable接口启动线程都是使用start方法，然后JVM虚拟机将此线程放到就绪队列中，如果有处理机可用，则执行run方法
  - 实现Callable接口要实现call方法，并且线程执行完毕后会有返回值，其他两种没有返回值

并发编程特性

1. 原子性：即一个或者多个操作作为一个整体，要么全部执行，要么都不执行，并且操作在执行过程中不会被线程调度机制打断
2. 可见性：当多个线程访问同一个变量时，一个线程修改了这个变量的值，其他线程能够立即看得到修改的值
3. 有序性：即程序执行的顺序按照代码的先后顺序执行

**线程中的方法**

- wait：**强迫一个线程等待**

  针对已经获取对象锁的线程进行操作，线程获取对象锁后，调用这个方法释放锁，所以需要在同步代码块中调用(否则会发生
  IllegalMonitorStateException的异常

- notify：**随机通知一个线程继续运行**；
  
  notify() 调用后，并不是马上就释放对象锁的，而是在相应的synchronized(){}语句块执行结束，自动释放锁后，JVM会在wait()对象
  锁的线程中随机选取一线程，赋予其对象锁，唤醒线程，继续执行。
  
- notifyAll：**将所有等待的线程唤醒**

- join：调用所在的线程等待，直到thread方法执行完成（本质就是先wait，执行完后notifyAll）

- sleep：**使调用该方法的线程暂停执行一段时间**
  
  让其他线程有机会继续执行，但它并不释放对象锁。也就是如果有Synchronized同步块，其他线程仍然不同访问共享数据(注意该
  方法要捕获异常)
  
- yeild：**线程让步**

  主要用于执行一些耗时较长的计算任务时，为了防止计算机处于“卡顿”的线程，会时不时的让出一些CPU的资源，给操作系统内的其他进程使用。

  - 不会将线程转入阻塞状态，只会给优先级相同，或优先级更高的线程执行机会，因此完全有可能某个线程被yield()方法暂停之后，立即再次获得处理器资源被执行
  - sleep()方法声明抛出了InterruptedException异常，yeild 没有声明抛出异常

- interrupt：**发送终止信号**

  可以让需要停止的线程看到停止信号后，主动把手上的工作做到一个阶段完成，然后再主动退出

### ThreadPoolExecutor

线程资源必须通过线程池提供，不允许在应用中自行显示的创建线程：

- 减少在创建和销毁线程上所消耗的时间及系统资源

线程池不允许使用Executors创建，其弊端

- FixedThreadPool和SingleThreadPool：允许的请求队列长度为Integer.MAX_VALUE，可能会堆积大量的请求，从而导致OOM

- CachedThreadPool和ScheduledThreadPool：允许的创建线程数量为Integer.MAX_VALUE，可能会堆积大量的请求，从而导致OOM

execute与submit方法的区别

- 返回值不同，submit有返回值，而execute没有返回值

- 使用submit，其执行的task是java.util.concurrent.FutureTask，可以从中获取返回值

```java
BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(25);
ThreadPoolExecutor pool = new ThreadPoolExecutor(5, 10, 30, TimeUnit.MINUTES, queue);
pool.setRejectedExecutionHandler(new RejectedExecutionHandler() {
      @Override
      public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            //TODO Auto-generated method stub
      }
   }
});
doWork work = new doWork();
pool.execute(work);
```

参数说明：

| 序号 | 名称            | 类型                     | 含义                                                 |
| ---- | --------------- | ------------------------ | ---------------------------------------------------- |
| 1    | corePoolSize    | int                      | 核心线程池大小                                       |
| 2    | maximumPoolSize | int                      | 最大线程池大小                                       |
| 3    | keepAliveTime   | long                     | 线程池中超过 corePoolSize 数目的空闲线程最大存活时间 |
| 4    | unit            | TimeUnit                 | 时间单位                                             |
| 5    | workQueue       | BlockingQueue<Runnable>  | 线程等待队列                                         |
| 6    | threadFactory   | ThreadFactory            | 线程创建工厂                                         |
| 7    | handler         | RejectedExecutionHandler | 拒绝策略                                             |

ThreadPoolExecutor新任务提交处理流程：

```
1、如果poolSize < corePoolSize，无论是否有空闲的线程新增一个线程处理新提交的任务；
2、如果poolSize >= corePoolSize 且任务队列未满时，就将新提交的任务提交到阻塞队列排队，等候处理workQueue.offer(command)；
3、如果poolSize >= corePoolSize 且任务队列满时；
	3.1、当前poolSize<maximumPoolSize，那么就新增线程来处理任务；
	3.2、当前poolSize=maximumPoolSize，那么意味着线程池的处理能力已经达到了极限，此时需要拒绝新增加的任务。至于如何拒绝处理新增的任务，取决于线程池的饱和策略RejectedExecutionHandler。
```

workQueue一般分为以下几种

- **ArrayBlockingQueue：** 一个基于数组的阻塞队列，按先进先出排序。初始化的时候，必须传入一个容量大小的值
- **LinkedBlockingQueue：** 一个基于链表的阻塞队列，按先进先出排序，吞吐量通常要高于ArrayBlockingQueue。默认的容量大小为：Integer.MAX_VALUE 但不推荐；生产者的锁PutLock，消费者的锁takeLock
- **SynchronousQueue：** 一个不存储元素的阻塞队列。每个插入操作必须等到另一个线程调用移除操作，否则插入操作一直处于阻塞状态，吞吐量通常要高于LinkedBlockingQueue，静态工厂方法Executors.newCachedThreadPool使用了这个队列。
- **PriorityBlockingQueue：** 一个具有优先级的无限阻塞队列。

RejectedExecutionHandler（饱和策略）

- AbortPolicy：丢弃任务，抛运行时异常（默认）
- CallerRunsPolicy：执行任务。
- DiscardPolicy：不处理，丢弃掉。
- DiscardOldestPolicy：当任务被拒绝添加时，会抛弃任务队列中最旧的任务也就是最先加入队列的，再把这个新任务添加进去。
- 也可根据需要实现自定义策略。如记录日志或持久化不能处理的任务。

线程池的关闭　　

- shutdown()：不会立即终止线程池，而是要等所有任务缓存队列中的任务都执行完后才终止，但再也不会接受新的任务
- shutdownNow()：立即终止线程池，并尝试打断正在执行的任务，并且清空任务缓存队列，返回尚未执行的任务

使用建议

- CPU密集型任务配置尽可能少的线程数量，IO密集型任务则配置尽可能多的线程
- 建议使用有界队列防止异常撑满内存导致整个系统不可用
- 通过线程池提供的参数进行监控，如taskCount、completedTaskCount、largestPoolSize
- 围绕beforeExecute()、afterExecute()和terminated()三个接口实现扩展

与ThreadPoolTaskExecutor 关系：

1. ThreadPoolExecutor是一个java类，ThreadPoolTaskExecutor实现了InitializingBean, DisposableBean 等，具有spring特性
2. ThreadPoolTaskExecutor只关注自己增强的部分，任务执行还是ThreadPoolExecutor处理。
3.  ThreadPoolTaskExecutor 不会自动创建ThreadPoolExecutor需要手动调initialize才会创建

### volatile

volatile值有可见性和禁止指令重排（有序性），无法保证原子性

**工作内存与主内存**

- 每个线程都有一个独立的工作内存，用于存储线程私有的数据

- Java内存模型中规定所有变量都存储在主内存，主内存是共享内存区域，所有线程都可以访问

- 线程对变量的操作(读取赋值等)必需在工作内存中进行。（线程安全问题的根本起因）

  （1）首先要将变量从主内存拷贝的自己的工作内存空间

  （2）而后对变量进行操作，操作完成后再将变量写回主内存

  （3）不能直接操作主内存中的变量，工作内存中存储着主内存中的变量副本拷贝

  （4）因而不同的线程间无法访问对方的工作内存，线程间的通信(传值)必需通过主内存来完成。

**指令重排**

编译器可以对程序指令进行重排序，但是有一个条件，就是不管怎么重排序都不能改变单线程执行程序的结果

- volatile关键字为域变量的访问提供了一种免锁机制 
- 使用volatile修饰域相当于告诉虚拟机该域可能会被其他线程更新 
- 因此每次使用该域就要重新计算，而不是使用寄存器中的值 
- volatile不会提供任何原子操作，它也不能用来修饰final类型的变量

**volatile 原理**

- 当对 volatile 变量执行写操作后，JMM（Java 内存模型）会把工作内存中的最新变量强制刷新到主内存
- 写操作会导致其他线程中的缓存无效

### 锁概念

- 可重入锁：：指的是同一个线程外层函数获得锁之后，内层仍然能获取到该锁的代码，在同一个线程在外层方法获 取锁的时候，在进入内层方法或会自动获取该锁
- 自旋锁：是指定尝试获取锁的线程不会立即堵塞，而是采用循环的方式去尝试获取锁，这样的好处是减少线程上线文切换的消耗，缺点就是循环会消耗 CPU。
- 公平锁：是指多个线程按照申请的顺序来获取值
- 独占锁（写锁）：指该锁一次只能被一个线程持有，ReentrantLook / Synchronized都是独占锁
- 共享锁（读锁）：该锁可以被多个线程持有，ReentrantReadWriteLock 其读锁是共享锁而写锁是独占锁
- 悲观锁：总是假设最坏的情况，每次去拿数据的时候都认为别人会修改，所以每次在拿数据的时候都会上锁，这样别人想拿这个数据就会阻塞直到它拿到锁
- 乐观锁：总是假设最好的情况，每次去拿数据的时候都认为别人不会修改，所以不会上锁，但是在更新的时候会判断一下在此期间别人有没有去更新这个数据
- 偏向锁：
- 轻量锁：使用自旋，竞争的线程不会阻塞，提高了程序的响应速度
- 重量锁：不使用自旋，获取锁失败的线程直接进入阻塞状态

锁主要存在四种状态，依次是**：无锁状态、偏向锁状态、轻量级锁状态、重量级锁状态**，锁可以从偏向锁升级到轻量级锁，再升级的重量级锁。但是锁的升级是单向的，也就是说只能从低到高升级，不会出现锁的降级。

### synchornized

**lock和synchronized**

| 类别       | synchronized                        | lock                                    |
| ---------- | ----------------------------------- | --------------------------------------- |
| 层次       | java关键字，内置的语言实现          | 是一个接口                              |
| 锁实现机制 | 监视器模式                          | 依赖AQS                                 |
| 锁释放     | 自动释放监视器                      | 在finally块中调用unlock，不然会造成死锁 |
| 灵活性     | 不灵活                              | 支持响应中断、超时、尝试获取锁          |
| 锁类型     | 可重入、不可中断、非公平            | 可重入、可中断、可公平                  |
| 性能       | JDK1.6之后使用CAS优化，性能差距不大 | 以提高多个线程进行读操作的效率          |
| 条件队列   | 关联一个条件队列                    | 可关联多个条件队列                      |

synchronized修饰的对象有几种：

- 修饰一个类：其作用的范围是synchronized后面括号括起来的部分，**作用的对象是这个类的所有对象**；
- 修饰一个方法：被修饰的方法称为同步方法，其作用的范围是整个方法，**作用的对象是调用这个方法的对象**；
- 修饰一个静态的方法：其作用的范围是整个方法，**作用的对象是这个类的所有对象**；
- 修饰一个代码块：被修饰的代码块称为同步语句块，其作用范围是大括号{}括起来的代码块，**作用的对象是调用这个代码块的对象**；

synchronized(this)是对象锁，如果有多个对象就有相对应的多个锁。（修饰一个代码块）
synchronized(类的名.class)是全局锁，不管有几个对象就公用一把锁。（修饰一个类）

**Syschronized关键字 Sychronized代码块区别？static synchroniezd?**

- synchronized代码块比synchronized方法要灵活。因为也许一个方法中只有一部分代码只需要同步，如果此时对整个方法用synchronized进行同步，会影响程序执行效率。而使用synchronized代码块就可以避免这个问题，synchronized代码块可以实现只对需要同步的地方进行同步。
- 如果一个线程执行一个对象的非static synchronized方法，另外一个线程需要执行这个对象所属类的static synchronized方法，此时不会发生互斥现象，因为访问static synchronized方法占用的是类锁，而访问非static synchronized方法占用的是对象锁，所以不存在互斥现象

**原理**

1. **monitorenter**：每个对象都是一个监视器锁（monitor）。当monitor被占用时就会处于锁定状态，线程执行monitorenter指令时尝试获取monitor的所有权，过程如下：

   > 1. 如果monitor的进入数为0，则该线程进入monitor，然后将进入数设置为1，该线程即为monitor的所有者；
   > 2. 如果线程已经占有该monitor，只是重新进入，则进入monitor的进入数加1；
   > 3. 如果其他线程已经占用了monitor，则该线程进入阻塞状态，直到monitor的进入数为0，再重新尝试获取monitor的所有权；

2. **monitorexit**：执行monitorexit的线程必须是objectref所对应的monitor的所有者。指令执行时，monitor的进入数减1，如果减1后进入数为0，那线程退出monitor，不再是这个monitor的所有者。其他被这个monitor阻塞的线程可以尝试去获取这个 monitor 的所有权。

3. 方法的同步并没有通过指令 `monitorenter` 和 `monitorexit` 来完成，相对于普通方法，其常量池中多了 `ACC_SYNCHRONIZED` 标示符。JVM就是根据该标示符来实现方法的同步的：

   > 当方法调用时，调用指令将会检查方法的 ACC_SYNCHRONIZED 访问标志是否被设置，如果设置了，执行线程将先获取monitor，获取成功之后才能执行方法体，方法执行完后再释放monitor。**在方法执行期间，其他任何线程都无法再获得同一个monitor对象。**

参考：https://blog.csdn.net/qq_43198052/article/details/119973666

### ReentrantLock

| 锁/类型                | 公平/非公平锁 | 可重入/不可重入锁 | 共享/独享锁          | 乐观/悲观锁 |
| ---------------------- | ------------- | ----------------- | -------------------- | ----------- |
| synchronized           | 非公平锁      | 可重入锁          | 独享锁               | 悲观锁      |
| ReentrantLock          | 都支持        | 可重入锁          | 独享锁               | 悲观锁      |
| ReentrantReadWriteLock | 都支持        | 可重入锁          | 读锁-共享，写锁-独享 | 悲观锁      |

**ReentrantLock**

 ReenreantLock类的常用方法有：

​    ReentrantLock() : 创建一个ReentrantLock实例
​    lock() : 获得锁
​    unlock() : 释放锁

相比**synchronize**，**ReentrantLock**增加了一些高级功能

- 等待可中断 : ReentrantLock提供了一种能够中断等待锁的线程的机制，通过lock.lockInterruptibly()来实现这个机制。
- 可实现公平锁 : ReentrantLock可以指定是公平锁还是非公平锁。而synchronize只能是非公平锁。
- 可实现选择性通知（锁可以绑定多个条件）: synchronize关键字与wait()和notify()/notifyAll()方法相结合可以实现等待/通知机制。ReentrantLock类需要借助于Condition接口与newCondition()方法。

### CAS

**概念**

CAS(compare and swap)，比较并交换。是一种非阻塞的轻量级的乐观锁，可以解决多线程并行情况下使用锁造成性能损耗的一种机制，整个J.U.C都是建立在CAS之上的。

一个线程从主内存中得到num值，并对num进行操作，写入值的时候，线程会把第一次取到的num值和主内存中num值进行比较，如果相等，就会将改变后的num写入主内存，如果不相等，则一直循环对比，直到成功为止

**CAS 缺点**

1. **ABA问题**（解决添加版本号；AtomicStampedReference，这个类的compareAndSet方法作用是首先检查当前引用是否等于预期引用，并且当前标志是否等于预期标志，如果全部相等，则以原子方式将该引用和该标志的值设置为给定的更新值。）
2. **循环时间长开销大，占用CPU资源**。如果自旋锁长时间不成功，会给CPU带来很大的开销。（如果JVM能支持处理器提供的pause指令那么效率会有一定的提升）
3. **只能保证一个共享变量的原子操作**。Java1.5开始JDK提供了AtomicReference类来保证引用对象之间的原子性，你可以把多个变量放在一个对象里来进行CAS操作。

**CAS使用的时机**

1. 线程数较少、等待时间短可以采用自旋锁进行CAS尝试拿锁，较于synchronized高效。
2. 线程数较大、等待时间长，不建议使用自旋锁，占用CPU较高

### AQS

AQS全称是AbstractQueuedSynchronizer，是一个抽象队列同步器，java并发包下很多API都是基于AQS来实现的加锁和释放锁等功能的，**AQS是JUC实现并发编程的核心**，如ReentrantLock、Semaphore、CountDownLatch等

CAS+ CLH(变种双向链表) + state变量

主要属性

| 属性名称             | 说明                                                     |
| -------------------- | -------------------------------------------------------- |
| state                | 锁的状态，初始值为0                                      |
| exclusiveOwnerThread | 当前获得锁的线程，初始值为null                           |
| 队列                 | 没有抢到锁的线程会包装成一个node节点存放到一个双向链表中 |

AQS实现了一个FIFO的队列。底层实现的数据结构是一个**双向链表**。可以看成是一个用来实现同步锁以及其他涉及到同步功能的核心组件

AQS的实现依赖内部的同步队列，也就是FIFO的双向队列，如果当前线程竞争锁失败，那么AQS会把当前线程以及等待状态信息构造成一个Node加入到同步队列中，同时再阻塞该线程。当获取锁的线程释放锁以后，会从队列中唤醒一个阻塞的节点(线程)。

不同于synchronized同步队列和等待队列只有一个，AQS的等待队列是有多个，因为AQS可以实现排他锁（ReentrantLock）和非排他锁（ReentrantReadWriteLock——读写锁），读写锁就是一个需要多个等待队列的锁。等待队列（Condition）用来保存被阻塞的线程的。

在condition中，多少个condition就对应多少个等待队列，这样就可以区分出希望将当前获得锁的线程放进哪个等待队列中，以达到精准的通知/等待机制。

AQS实现了两套加锁解锁的方式，那就是**独占式**和**共享式**。

参考：

https://tech.meituan.com/2019/12/05/aqs-theory-and-apply.html

https://www.freesion.com/article/6246850783/

### ThreadLocal

ThreadLocal用来提供线程内部的局部变量。

通常情况下，我们创建的成员变量都是线程不安全的。因为他可能被多个线程同时修改，此变量对于多个线程之间彼此并不独立，是共享变量。而使用ThreadLocal创建的变量只能被当前线程访问，其他线程无法访问和修改。

ThreadLocal类提供了如下方法：

```java
public T get() { }
public void set(T value) { }
public void remove() { }
protected T initialValue() { }
```

ThreadLocal的值保存在Thread中属性threadLocals内，作为一个特殊的Map，它的key值就是我们ThreadLocal实例，而value值这是我们设置的值。

**应用场景**

实例需要在整个线程中共享，但不希望被多线程共享（比如工具类，典型的就是`SimpleDateFormat`）。

- 比如存储 交易id等信息。每个线程私有。
- 比如aop里记录日志需要before记录请求id，end拿出请求id。
- 比如jdbc连接池

在著名的框架Hiberante中，数据库连接的代码：

```java
private static final ThreadLocal threadSession = new ThreadLocal();  

public static Session getSession() throws InfrastructureException {  
    Session s = (Session) threadSession.get();  
    try {  
        if (s == null) {  
            s = getSessionFactory().openSession();  
            threadSession.set(s);  
        }  
    } catch (HibernateException ex) {  
        throw new InfrastructureException(ex);  
    }  
    return s;  
}  
```

### Atomic

- 本质：自旋锁 + CAS

- 优缺点：参考 CAS

- Java 8 的改进：

  LongAdder、LongAccumulator、DoubleAdder、DoubleAccumulator

  实现原理是：使用**分段CAS**以及**自动分段迁移**的方式来大幅度提升多线程高并发执行CAS操作的性能

- 是否可以完全替代？`AtomicLong`提供了很多cas方法，例如`getAndIncrement`、`getAndDecrement`等，使用起来非常的灵活，而`LongAdder`只有`add`和`sum`，使用起来比较受限。如果我们的场景仅仅是需要用到加和减操作的话，那么可以直接使用更高效的 LongAdder，但如果我们需要利用 CAS 比如 compareAndSet 等操作的话，就需要使用 AtomicLong 来完成。

### 并发辅助类

1. CountDownLatch：利用它可以实现类似计数器的功能。比如有一个任务A，它要等待其他4个任务执行完毕之后才能执行，此时就可以利用CountDownLatch来实现这种功能了。

   ```java
   //调用await()方法的线程会被挂起，它会等待直到count值为0才继续执行
   public void await() throws InterruptedException { };   
   //和await()类似，只不过等待一定的时间后count值还没变为0的话就会继续执行
   public boolean await(long timeout, TimeUnit unit) throws InterruptedException { }; 
   public void countDown() { };  //将count值减1
   ```

2. CyclicBarrier：所有等待线程都被释放以后，CyclicBarrier可以被重用

   当所有线程线程写入操作完毕之后，所有线程就继续进行后续的操作了。如果说想在所有线程写入操作完之后，进行额外的其他操作可以为CyclicBarrier提供Runnable参数

3. Semaphore：翻译成字面意思为 信号量，Semaphore可以控同时访问的线程个数，通过 acquire() 获取一个许可，如果没有就等待，而 release() 释放一个许可。

**Callable、Future、FutureTask**

### 进程线程协程

## 2. 集合

### 集合分类

#### List集合

- 集合中的元素可以重复，访问集合中的元素可以根据元素的索引来访问

实现类：

1. ArrayList：长度可变的数组。查找快。
2. LinkedList：基于链表。插入删除快。同时实现List接口和Deque接口，能对它进行队列操作，即可以根据索引来随机访问集合中的元素，也能将LinkedList当作双端队列使用，自然也可以被当作"栈来使用（可以实现“fifo先进先出，filo后入先出”）

ArrayList是线程不安全的，经常会使用以下方式替代

- Collections.synchronizedList：使用了同步锁
- CopyOnWriteArrayList：可重入锁，而且还需要进行数组的复制

LinkedList也是非线程安全的，经常会使用以下方式替代

- Collections.synchronizedList：使用了同步锁

- ConcurrentLinkedQueue
- LinkedBlockingDeque

#### Set集合

- Set集合中的对象无排列顺序，且没有重复的对象

- Set判断集合中两个对象相同不是使用"=="运算符，而是根据equals方法

Set集合的主要实现类：

1. HashSet：按照哈希算法来存储集合中的对象，速度较快。
2. LinkedHashSet：不仅实现了哈希算法，还实现了链表的数据结构，提供了插入和删除的功能。遍历时，LinkedHashSet将会按元素的添加顺序来访问集合里的元素。
3. TreeSet：实现了SortedSet接口（此接口主要用于排序操作，即实现此接口的子类都属于排序的子类）。

#### Map集合

- Map集合中保存Key-value对形式的元素，访问时只能根据每项元素的key来访问其value
- key和value都可以是任何引用类型的数据

Map集合的主要实现类：

1. HashMap：按照哈希算法来存取key，有很好的存取性能，和HashSet一样，要求覆盖equals()方法和hasCode()方法
2. LinkedHashMap：使用双向链表来维护key-value对的次序，该链表负责维护Map的迭代顺序，与key-value对的插入顺序一致。
3. TreeMap：一个红黑树数据结构，每个key-value对即作为红黑树的一个节点。实现了SortedMap接口，能对key进行排序。TreeMap可以保证所有的key-value对处于有序状态。同样，TreeMap也有两种排序方式（自然排序、定制排序）

**ArrayList,Vector,HashMap,Hashtable扩容机制？**

1. arraylist,初始容量10，(oldCapacity * 3)/2 + 1
2. vector,初始容量10，oldCapacity * 2
3. hashmap,初始容量16，达到阀值扩容，为原来的两倍
4. hashtable，初始容量11，达到阀值扩容，oldCapacity * 2 + 1

### HashMap

**实现原理**

- hashmap是数组和链表的结合体，数组每个元素存的是链表的头结点
- 往hashmap里面放键值对的时候先得到key的hashcode，然后重新计算hashcode，（让1分布均匀因为如果分布不均匀，低位全是0，则后来计算数组下标的时候会冲突），然后与length-1按位与，计算数组出数组下标
- 如果该下标对应的链表为空，则直接把键值对作为链表头结点，如果不为空，则遍历链表看是否有key值相同的，有就把value替换，没有就把该对象最为链表的第一个节点，原有的节点最为他的后续节点
- 初始容量16，达到阀值扩容，阀值等于最大容量*负载因子，扩容每次2倍，总是2的n次方
- 链表长度超过8时转为红黑树，当删除小于6时重新变为链表

**并发问题**

ReHash造成环形链表，程序会进入死循环

### ConcurrentHashMap

ConcurrentHashMap是线程安全的，用来替代HashTable。

**JDK1.7**

使用分段锁技术，将数据分成一段一段（Segment）的存储，然后给每一段数据配一把锁（Segment继承了ReentrantLock），当一个线程占用锁访问其中一个段数据的时候，其他段的数据也能被其他线程访问。ConcurrentHashMap定位一个元素的过程需要进行两次Hash操作。第一次Hash定位到Segment，第二次Hash定位到元素所在的链表的头部。

**JDK1.8**

JDK1.8的实现已经摒弃了Segment的概念，而是直接用Node数组+链表或红黑树（的数据结构来实现，并发控制使用Synchronized和CAS来操作。

JDK1.8的Node节点中value和next都用volatile修饰，保证并发的可见性。

synchronized 只锁定当前链表或红黑⼆叉树的首节点，这样只要 hash 不冲突，就不会产⽣并发

### HashMap HashTable区别

1. HashMap是继承自AbstractMap类，而HashTable是继承自Dictionary类
1. Dictionary类是一个已经被废弃的类。父类都被废弃，自然而然也没人用它的子类Hashtable了。
2. Hashtable不允许null值（key和value都不可以），HashMap允许null值（key和value都可以）
2. Hashtable是线程安全的，它的每个方法中都加入了Synchronize方法
2. HashMap 把 Hashtable 的 contains 方法去掉了，改成 containsValue 和 containsKey。
3. 哈希值的使用不同，HashTbale是古老的除留余数法。而HashMap重新计算Hash值，减少碰撞。
4. 初始大小和扩容机制不同，HashMap中Hash数组的默认大小是16，而且一定是2的指数。

**并发集合—CopyOnWriteArrayList和CopyOnWriteArraySet**

CopyOnWrite容器即写时复制的容器。通俗的理解是当我们往一个容器添加元素的时候，不直接往当前容器添加，而是先将当前容器进行Copy，复制出一个新的容器，然后新的容器里添加元素，添加完元素之后，再将原容器的引用指向新的容器。这样做的好处是我们可以对CopyOnWrite容器进行并发的读，而不需要加锁，因为当前容器不会添加任何元素。所以CopyOnWrite容器也是一种读写分离的思想，读和写不同的容器。

### HashSet和HashMap

1.将HashSet或HashMap转换为线程安全，使用Collections.synchronizedSet或Collections.synchronizedMap方法；
2.使用Collections.newSetFromMap(new ConcurrentHashMap<Integer, Boolean>())或使用java.util.concurrent包下的ConcurrentHashMap；
3.仍然使用HashSet或HashMap，使用时手动进行加锁或同步；

- LinkedHashSet：链表维护元素的次序
- HashSet：树结构实现红黑树算法。元素是按顺序进行排列，但是contains()等方法都是复杂度为O(log (n))

### fail-fast机制

fail-fast是一种错误检测机制，一旦检测到可能发生错误，就立马抛出异常，程序不继续往下执行。

解决方法

1. 在单线程的遍历过程中，如果要进行remove操作，可以调用迭代器的remove方法而不是集合类的remove方法
2. 使用java并发包(java.util.concurrent)中的类来代替 ArrayList 和hashMap。

## 3. JVM

### 内存模型

Java 虚拟机在执行 Java 程序的过程会把它所管理的内存划分为若干个不同的数据区域：

**线程私有区域**

1. **程序计数器**：任何时间一个线程都只有一个方法在执行，也就是当前方法。字节码解释器工作时通过改变这个计数器的值来选取下一条需要执行的指令（唯一一个不会出现 `OutOfMemoryError` 的内存区域）。
2. **虚拟机栈**：通常说的栈空间，描述的是 Java 方法执行的内存模型：每个方法在执行的时候会创建一个栈帧用于存储局部变量表、操作数栈、动态链接、方法出口等信息。通常所说的栈内存就是指虚拟机栈或虚拟机栈中的局部变量表部分。
3. **本地方法栈**：与虚拟机栈作用类似，只不过 Java虚拟机栈为虚拟机执行 Java 方法服务，而本地方法栈为执行 Native 方法服务（在 HotSpot 中和虚拟机栈合二为一）

**运行时数据区域（所有线程共享）**

1. **Java 堆**：存放对象实例，是 Java 虚拟机所管理的内存最大的一块，是垃圾收集器管理的主要区域，从内存回收的角度，还可以细分为新生代和老年代，再细一点有Eden 区、From Survivor 区、To Survivor区等。
2. **方法区**：是一个抽象概念，用于存储已被虚拟机加载的类信息、常量、静态变量、JIT 编译后的代码等数据
   - 永久代：JDK 1.8 之前的方法区实现
   - 元空间：使用本地内存，主要考虑到永久代内存上限无法调整（元空间并不在运行时数据区）

#### 栈帧结构

栈帧（Stack Frame）用于支持虚拟机进行方法调用和方法执行的数据结构，每一个方法从调用开始执行到完成的过程，都对应着一个栈帧在虚拟机栈里面从入栈到出栈的过程

- **局部变量表**：用于定义方法参数和方法内部定义的局部变量（boolean、byte、char、short、int、float、long、double）。
- **操作数栈**：也称**表达式栈**，主要用于保存**计算过程的中间结果**，同时作为计算过程中**临时的存储空间**
- **动态链接**：当一个方法要调用其他方法，需要将常量池中指向方法的符号引用转化为其在内存地址中的直接引用（相对应的在类加载过程中转换为直接引用被称为**静态解析**）
- **方法返回地址**：方法退出的过程其实就是把当前栈帧出栈，需要返回方法被调用的位置。之后PC计数器的值指向方法调用指令后面的一条指令
- **额外附加信息**：如调试相关信息，这部分信息完全取决于具体虚拟机的视线。

### 编译过程

JAVA源码编译由三个过程组成：源码编译机制。类加载机制。类执行机制

- **代码编译**
  - 由JAVA源码编译器来完成。主要是将源码编译成字节码文件（class文件）。字节码文件格式主要分为两部分：常量池和方法字节码。

- **类加载机制**
  - 加载：将字节码从不同的数据源转化为二进制字节流加载到内存中
  - 验证：验证阶段主要包括四个检验过程：文件格式验证、元数据验证、字节码验证和符号引用验证
  - 准备：为类中的所有静态变量分配内存空间，并为其设置一个初始值
  - 解析：将常量池中所有的符号引用转为直接引用
  - 初始化：则是根据程序员自己写的逻辑去初始化类变量和其他资源

#### 双亲委派机制

概念：

如果一个类加载器收到了类加载请求，它并不会自己先去加载，而是把这个请求委托给父类的加载器去执行，如果父类加载器还存在其父类加载器，则进一步向上委托，依次递归，请求最终将到达顶层的启动类加载器，如果父类加载器可以完成类加载任务，就成功返回，倘若父类加载器无法完成此加载任务，子加载器才会尝试自己去加载

双亲委派模型有效解决了以下问题：

- 每一个类都只会被加载一次，避免了重复加载
- 每一个类都会被尽可能的加载（从引导类加载器往下，每个加载器都可能会根据优先次序尝试加载它）
- 有效避免了某些恶意类的加载（比如自定义了Java.lang.Object类，一般而言在双亲委派模型下会加载系统的Object类而不是自定义的Object类）

> 问：可以不可以自己写个String类
>
> 答案：不可以，因为 根据类加载的双亲委派机制，会去加载父类，父类发现冲突了String就不再加载了;

**自定义类加载器**

要创建用户自己的类加载器，只需要继承java.lang.ClassLoader类，然后覆盖它的findClass(String name)方法即可，即指明如何获取类的字节码流。

如果要符合双亲委派规范，则重写findClass方法（用户自定义类加载逻辑）；要破坏的话，重写loadClass方法(双亲委派的具体逻辑实现)

https://www.cnblogs.com/aspirant/p/7200523.html

#### 装箱与拆箱

有了基本类型之后为什么还要有包装器类型呢？核心：让基本类型具备对象的特征,实现更多的功能.

装箱过程是通过调用包装器的valueOf方法实现的，而拆箱过程是通过调用包装器的 xxxValue方法实现的。（xxx代表对应的基本数据类型）

### 对象创建过程

虚拟机遇到一条 new 指令时

1. 类加载检查：指令参数是否能在常量池中定位到这个类的符号引用，并且检查符号引用代表的类是否已被加载过

2. 分配内存：分配方式

   指针碰撞 ： 

   - 适用场合 ：堆内存规整（即没有内存碎片）的情况下。
   - 原理 ：用过的内存全部整合到一边，没有用过的内存放在另一边，中间有一个分界指针，只需要向着没用过的内存方向将该指针移动对象内存大小位置即可。
   - 使用该分配方式的 GC 收集器：Serial, ParNew

   空闲列表 ： 

   - 适用场合 ： 堆内存不规整的情况下。
   - 原理 ：虚拟机会维护一个列表，该列表中会记录哪些内存块是可用的，在分配的时候，找一块儿足够大的内存块儿来划分给对象实例，最后更新列表记录。
   - 使用该分配方式的 GC 收集器：CMS

3. 初始化零值：赋字段的默认零值

4. 设置对象头：例如这个对象是哪个类的实例、如何才能找到类的元数据信息、对象的哈希码、对象的 GC 分代年龄等信息

5. 执行 init 方法：把对象按照程序员的意愿进行初始化

### 垃圾回收 GC

**可达性分析算法**

**GC Roots** 是JVM确定当前绝对不能被回收的对象（如方法区中类静态属性引用的对象、方法还在运行没出栈的本地变量表引用的对象 )，以这些对象为根，根据引用关系开始向下搜寻，存在直接或间接引用链的对象就存活，不存在引用链的对象就回收

**分代收集算法**

根据对象存货周期的不同将内存划分为几块，一般把java堆分为新生代和老年代，进行分代回收

- 标记-清除收集算法：首先标记出所有需要回收的对象，在标记完成后统一回收所有被标记的对象，不足：
  1. 标记和清除效率都不高
  2. 标记清除后悔产生大量不连续的内存碎片

**1. 新生代**：每次垃圾回收都有大量对象死去，之后少量存活，适合采用复制算法。

- 复制算法：将内存划分为大小相同的两块（Eden和Survior默认比例是8:1），每次只使用其中一块。当这一块内存用完了，就将还存活着的对象复制到另外一块上面，然后再把已使用的内存空间一次清理掉，不足：

  1. 对象存活率较高时要进行较多的复制操作，效率将会变低
  2. 如果不想浪费50%的空间，需要有额外的空间进行分配担保，以应对被使用的内存中所有对象都100%存活的极端情况

  分配担保：新生代内存不足时，把新生代的存活的对象搬到老生代，这里老年代是担保人

**2. 老年代**：对象存活率高，没有额外空间对他进行分配担保，适合采用标记-整理、标记-清除搜集算法

- 标记-整理算法：标记过程与 “标记-清除” 算法一致，但后续步骤不是直接对可回收对象进行清理，而是让所有存活对象都向一端移动，然后直接清理掉端边界以外内存

#### MinorGC

从年轻代空间（包括 Eden 和 Survivor 区域）回收内存被称为 Minor GC，触发条件

- Eden区域满了
- 当Eden区的内存不够新生对象内存分配时

#### Major GC/Full GC

- MajorGC：清理老年代，Major GC发生过程常常伴随一次Minor

- FullGC：Full GC可以看做是Major GC+Minor GC的一整个过程，是清理整个堆空间（包括年轻代和老年代），触发条件
  - Minor GC 时 Survivor空间不足，判断是否允许担保失败，如果不允许则进行Full GC。如果允许，并且每次晋升到老年代的对象平均大小>老年代最大可用连续内存空间，也会进行Full GC。
  - MinorGC后存活的对象超过了老年代剩余空间
  - 方法区内存不足时
  - System.gc()，可用通过-XX:+ DisableExplicitGC来禁止手动调用System.gc()方法
  - CMS GC异常，CMS运行期间预留的内存无法满足程序需要，就会出现一次“Concurrent Mode Failure”失败，会触发Full GC

#### GC 类型

1. **Serial GC（新生代收集器-复制算法）、Serial Old GC（老年代收集器-标记-整理算法）**

   是最基本、历史最悠久的收集器，是单线程的收集器，在进行垃圾回收时必须暂停其他所有工作线程。是JDK8中client模式下默认GC。优点：简单高效（与其他收集器的单线程比）

2. **ParNew GC（新生代收集器-复制算法）**

   Serial GC的多线程版本，配合老年代的CMS GC（只有 Serial 和 ParNew 可以和 CMS 配合）使用

   该收集器也是激活CMS后，新生代的默认垃圾收集器。一般和CMS进行搭配使用.

3. **Parrallel GC（新生代收集器-复制算法）、Parrallel Old GC（老年代收集器-标记-整理算法）**

   也是Serial GC的多线程版本，是JDK8中server模式下的默认GC，吞吐量优先。

   吞吐量：CPU 用于运行用户代码的时间 / CPU 总耗时（垃圾收集的时间越短越好），适合与用户交互的程序

   - -XX：MaxGCPauseMillis：最大垃圾收集停顿时间
   - -XX：GCTimeRatio：直接设置吞吐量大小

4. **CMS GC（老年代并行收集器-标记-清除算法）**

   特点：并发收集、低停顿。但是会占用更多CPU资源和用户争抢线程，基于标记-清除算法，可能产生内存碎片化问题，因此长时间后会触发full GC，而full GC停顿时间是很长的。

   **初始标记：** 暂停所有的其他线程，并记录下直接与 root 相连的对象，速度很快 ；

   **并发标记：** 同时开启 GC 和用户线程，记录可达对象。

   **重新标记：** 重新标记阶段就是为了修正并发标记期间因为用户程序继续运行而导致标记产生变动的那一部分对象的标记记录，这个阶段的停顿时间一般会比初始标记阶段的时间稍长，远远比并发标记阶段时间短

   **并发清除：** 开启用户线程，同时 GC 线程开始对未标记的区域做清扫。

5. **G1 GC（Garbage First）**

   兼顾吞吐量和停顿时间，JDK9以后的默认GC。步骤：初始标记**、**并发标记、最终标记**、**筛选回收

   G1 收集器在后台维护了一个优先列表，每次根据允许的收集时间，优先选择回收价值最大的 Region(这也就是它的名字 Garbage-First 的由来) 。这种使用 Region 划分内存空间以及有优先级的区域回收方式，保证了 G1 收集器在有限时间内可以尽可能高的收集效率（把内存化整为零）

6. **ZGC（标记-复制算法）：**

   JDK11 中推出的一款低延迟垃圾回收器，适用于大内存低延迟服务的内存管理和回收，在 128G 的大堆下，最大停顿时间才 1.68 ms，停顿时间远胜于 G1 和 CMS

```
-XX:+UseSerialGC
-XX:+UseParallelGC
-XX:+UseParNewGC
-XX:+UseG1GC
```

#### GC 日志

IDEA中配置GC日志：将参数值设置到VM options中即可

Docker中配置GC日志：修改 dockerfile 中执行启动命令设置Jvm参数

```shell
对应的参数列表
java -jar
-XX:MetaspaceSize=128m （元空间默认大小）
-XX:MaxMetaspaceSize=128m （元空间最大大小）
-XX:+PrintGC 　　输出GC日志
-XX:+PrintGCDetails 输出GC的详细日志
-XX:+PrintGCTimeStamps 输出GC的时间戳（以基准时间的形式）
-XX:+PrintGCDateStamps 输出GC的时间戳（以日期的形式，如 2013-05-04T21:53:59.234+0800）
-XX:+PrintHeapAtGC 　　在进行GC的前后打印出堆的信息
-Xloggc:../logs/gc.log 日志文件的输出路径
-Xms1024m （堆最大大小）堆内存设置过小，会频繁发生GC
-Xmx1024m （堆默认大小）每次JVM增加堆大小时，它都必须向操作系统请求额外的内存，这会花费一些时间
-Xmn256m （新生代大小）
-Xss256k （栈最大深度大小）
-XX:SurvivorRatio=8 （新生代分区比例 8:2）
-XX:+HeapDumpOnOutOfMemoryError  （在OOM的时候生成heap dump）
-XX:+HeapDumpBeforeFullGC （在full GC前生成heap dump）
-XX:HeapDumpPath=/opt/tmp/heapdump.hprof
-XX:+UseConcMarkSweepGC （指定使用的垃圾收集器，这里使用CMS收集器）
app.jar
```

### 调优

1. 堆内存`–Xms`、`-Xmx`，把-Xms 和 -Xmx 设置为一致，是为了避免频繁扩容和GC释放堆内存造成的系统开销/压力

2. 显式新生代内存

   将新对象预留在新生代，由于 Full GC 的成本远高于 Minor GC，因此尽可能将对象分配在新生代是明智的做法，实际项目中根据 GC 日志分析新生代空间大小分配是否合理，适当通过“-Xmn”命令调节新生代大小，最大限度降低新对象直接进入老年代的情况。

   ```shell
   -XX:NewSize=256m  新生代分配 最小 256m 的内存
   -XX:MaxNewSize=1024m 最大 1024m 的内存
   -XX:NewRatio=1 设置老年代与新生代内存的比值为 1
   -Xmn256m 新生代分配 256m 的内存（NewSize 与 MaxNewSize 设为一致）
   ```

#### JDK 命令行工具

这些命令在 JDK 安装目录下的 bin 目录下：

- **`jps`** (JVM Process Status）: 类似 UNIX 的 `ps` 命令。用于查看所有 Java 进程的启动类、传入参数和 Java 虚拟机参数等信息；
- **`jstat`**（JVM Statistics Monitoring Tool）: 用于收集 HotSpot 虚拟机各方面的运行数据;
- **`jinfo`** (Configuration Info for Java) : Configuration Info for Java,显示虚拟机配置信息;
- **`jmap`** (Memory Map for Java) : 生成堆转储快照;
- **`jhat`** (Java Heap Analysis Tool) : 用于分析 heapdump 文件，它会建立一个 HTTP/HTML 服务器，让用户可以在浏览器上查看分析结果;
- **`jstack`** (Stack Trace for Java) : 生成虚拟机当前时刻的线程快照，线程快照就是当前虚拟机内每一条线程正在执行的方法堆栈的集合。

#### JConsole

JConsole 是基于 JMX 的可视化监视、管理工具。可以很方便的监视本地及远程服务器的 java 进程的内存使用情况

## 4. IO

常用类

| 类               | 说明                                                         |
| ---------------- | ------------------------------------------------------------ |
| File             | 文件类，用于文件或者目录的描述信息                           |
| RandomAccessFile | 一个独立的类，直接继承至Object。它的功能丰富，可以从文件的任意位置进行存取（输入输出）操作。 |
| InputStream      | 字节输入流                                                   |
| OutputStream     | 字节输出流                                                   |
| Reader           | 字符输入流                                                   |
| Writer           | 字符输出流                                                   |

**Serializable**

序列化：将对象的状态信息转换为可以存储或传输的形式的过程

**Cloneable**

Cloneable接口的clone方法默认浅拷贝，将浅拷贝变为深拷贝需要对象中的子对象的类实现Cloneable接口，并在对象的clone方法中调用子对象clone方法，然后这个子对象便成了深拷贝。

### NIO

参考：http://wiki.jikexueyuan.com/project/java-nio-zh/java-nio-tutorial.html

Java NIO基本组件如下：


- 通道和缓冲区(*Channels and Buffers*)：在标准I/O API中，使用字符流和字节流。 在NIO中，使用通道和缓冲区。数据总是从缓冲区写入通道，并从通道读取到缓冲区。
- 选择器(*Selectors*)：Java NIO提供了“选择器”的概念。这是一个可以用于监视多个通道的对象，如数据到达，连接打开等。因此，单线程可以监视多个通道中的数据。
- 非阻塞I/O(*Non-blocking I/O*)：Java NIO提供非阻塞I/O的功能。这里应用程序立即返回任何可用的数据，应用程序应该具有池化机制，以查明是否有更多数据准备就绪。

### Netty

Channel

- FileChannel：操作文件
- DatagramChannel：UDP协议支持
- SocketChannel：TCP协议支持
- ServerSocketChannel：监听TCP协议Accept事件，之后创建SocketChannel

拆包器

- 固定长度拆包器：FixedLengthFrameDecoder

- 行拆包器：LineBaseFrameDecoder

- 分隔符拆包器：DelimeterBasedFrameDecoder

- 基于长度域拆包器：LengthFiledBasedFrameDecoder

## 5. MyBatis

### 核心组件

Mybatis底层封装了JDBC,使用了动态代理模式。

1.**SqlSessionFactoryBuilder(构造器)**：它可以从XML、注解或者手动配置Java代码来创建SqlSessionFactory。

2.**SqlSessionFactory**：用于创建SqlSession (会话) 的工厂

3.**SqlSession**： Mybatis最核心的类，可以用于执行语句、提交或回滚事务以及获取映射器Mapper的接口

4.**SQL Mapper**：它由一个Java接口和XML文件（或注解）构成，需要给出对应的SQL和映射规则，它负责发送SQL去执行，并返回结果。

5.**Executor**：SqlSession执行增删改查都是委托给Executor完成的

### 工作流程

1. 读取并解析mybatis-config.xml文件，通过SqlSessionFactoryBuilder创建SqlSessionFactory对象

   > InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
   > //这一行代码正是初始化工作的开始。
   > SqlSessionFactory factory = new SqlSessionFactoryBuilder().build(inputStream);

2. 通过SqlSessionFactory创建SqlSession对象

   > SqlSession是MyBatis中用于和数据库交互的`顶层类`，通常将它与ThreadLocal绑定，一个会话使用一个SqlSession，并且在使用完毕后需要close。
   >
   > SqlSession中的两个最重要的参数，configuration与初始化时的相同，Executor为执行器

3. 通过SqlSession拿到Mapper代理对象

4. 通过MapperProxy调用Mapper中增删改查的方法

### 数据库预编译

- 概念：数据库接受到sql语句之后，需要词法和语义解析，优化sql语句，制定执行计划。这需要花费一些时间。但是很多情况，我们的一条sql语句可能会反复执行，或者每次执行的时候只有个别的值不同。预编译语句就是将可能只有个别值不同的同一条 sql 语句中的值用占位符替代，可以视为将sql语句模板化或者说参数化。一次编译、多次运行，省去了解析优化等过程。
- 三个阶段：（往往 步骤 1、2 加起来的时间比 步骤 3的时间还要长）
  1. 词法和语义解析
  2. 优化sql语句，制定执行计划
  3. 执行并返回结果
- 作用：
  - 预编译之后的 sql 多数情况下可以直接执行，DBMS 不需要再次编译，提升性能
  - 防止 sql 注入：其后注入进来的参数系统将不会认为它会是一条SQL语句，而默认其是一个参数，参数中的or或者and 等就不是SQL语法保留字了。
- Mybatis 如何实现预编译：mybatis 底层使用PreparedStatement，过程是先将带有占位符（即”?”）的sql模板发送至mysql服务器，由服务器对此无参数的sql进行编译后，将编译结果缓存，然后直接执行带有真实参数的sql。`核心是通过#{ } 实现的`。

**Mabatis中#{}和${}的区别**

${} 则只是简单的字符串替换；#{} 在预处理时，会把参数部分用一个占位符 ? 代替

#{} 的参数替换是发生在 DBMS 中，而 ${} 则发生在动态解析过程中

优先使用 #{}。因为 ${} 会导致 sql 注入的问题

### Mapper 原理

**通常一个Xml映射文件，都会写一个Dao接口与之对应，请问，这个Dao接口的工作原理是什么？Dao接口里的方法，参数不同时，方法能重载吗？**

- Dao接口就是**Mapper**接口。

- 接口的**全限名**就是映射文件中的**namespace**的值

- 接口的方法名就是映射文件中的**MappedStatement**的**id**值

- 接口方法内的参数，就是传递给sql的参数。

- Mapper接口是没有实现类的，当调用接口方法时，**接口全限名+方法名拼接字符串作为key值**，可**唯一定位**一个**MappedStatement**。

  Dao接口里的方法，是不能重载的，因为是全限名+方法名的保存和寻找策略。Dao接口的工作原理是JDK动态代理，MyBatis运行时会使用JDK动态代理为Dao接口生产代理proxy对象，代理对象会拦截接口方法，转而执行MappedStatement所代表的sql，然后将sql执行结果返回。

  在MyBatis中，每一个< select >、< insert >、< update >、< delete >标签，都会被解析成一个MappedStatement对象。

### PageHelper原理

PageHelper是 MyBatis 的一个插件，内部实现了一个PageInterceptor拦截器。Mybatis会加载这个拦截器到拦截器链中。在我们使用过程中先使用PageHelper.startPage这样的语句在当前线程上下文中设置一个ThreadLocal变量，再利用PageInterceptor这个分页拦截器拦截，从ThreadLocal中拿到分页的信息，如果有分页信息拼装分页SQL（limit语句等）进行分页查询，最后再把ThreadLocal中的东西清除掉。

## 6. 其他

### 基础

**为什么string不可以被继承**

出于安全性和效率考虑

- String对象是缓存在字符串池中的，继承会破坏String的不可变性、缓存性以及hascode的计算方式
- 人们非常流行把String作为HashMap的键值，如果String可变，就会产生不同的Hash值
- String被广泛用于Java类的参数，如果String是可变的，就会导致严重的安全威胁
- 为了线程安全(字符串自己便是线程安全的)
- String的不可变性为Java的类加载机制的安全性提供了根本的保障

**为什么decimal精度高**

十进制整数在转化成二进制数时不会有精度问题，那么把十进制小数扩大N倍让它在整数的维度上进行计算，并保留相应的精度信息。

```java
public class BigDecimal { 
	//值的绝对long型表示
	private final transient long intCompact;
	//值的小数点后的位数
	private final int scale;
}
```

**lambda**

lambda 表达式常用方法：sort() filter() map() flatmap() collection() collectionAndThen() collect() orElse() 

map 与 flatmap：

- map: 对于Stream中包含的元素使用给定的转换函数进行转换操作，新生成的Stream只包含转换生成的元素
- flatMap：和map类似，不同的是其每个元素转换得到的是Stream对象，会把子Stream中的元素压缩到父集合中

### 反射

**静态解析**：符号引用就是假如类A引用了类B，加载阶段是静态解析，这时候B还没有被放到JVM内存中，这时候A引用的只是代表B的符号，这是符号引用。直接引用就是类A在解析阶段发现自己引用了B，如果这个时候B还没被加载。就是直接触发B的类加载，之后B的符号引用会被替换成实际地址。这被称为直接引用。 

**动态解析**：后期绑定其实就是动态解析。如果代码使用了多态。B是一个抽象类或者接口，A就不能知道究竟要用哪个来替换，只能等到实际发生调动时在进行实际地址的替换。这就是为什么有的解析发生在初始化之后。

获取类对象有三种方法：

- 通过forName() -> 示例：Class.forName(“PeopleImpl”)
- 通过getClass() -> 示例：new PeopleImpl().getClass()
- .class直接获取 -> 示例：PeopleImpl.class

常用方法：

- getName()：获取类完整方法；
- getSuperclass()：获取类的父类；
- newInstance()：创建实例对象；
- getFields()：获取当前类和父类的public修饰的所有属性；
- getDeclaredFields()：获取当前类（不包含父类）的声明的所有属性；
- getMethod()：获取当前类和父类的public修饰的所有方法；
- getDeclaredMethods()：获取当前类（不包含父类）的声明的所有方法；

### 动态代理

动态代理是一种方便运行时动态构建代理、动态处理代理方法调用的机制，很多场景都是利用类似机制做到的，比如用来包装 RPC 调用、面向切面的编程（AOP）

动态代理技术的常见实现方式有两种：

1. 基于接口的 JDK 动态代理

   JDK Proxy 是通过实现 InvocationHandler 接口来实现的，代码如下：

   ```java
   // JDK 代理类
   class AnimalProxy implements InvocationHandler {
       private Object target; // 代理对象
       public Object getInstance(Object target) {
           this.target = target;
           // 取得代理对象
           return Proxy.newProxyInstance(target.getClass().getClassLoader(), target.getClass().getInterfaces(), this);
       }
       @Override
       public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
           System.out.println("调用前");
           Object result = method.invoke(target, args); // 方法调用
           System.out.println("调用后");
           return result;
       }
   }
   public static void main(String[] args) {
       // JDK 动态代理调用
       AnimalProxy proxy = new AnimalProxy();
       Animal dogProxy = (Animal) proxy.getInstance(new Dog());
       dogProxy.eat();
   }
   </pre>
   ```

   注意：JDK Proxy 只能代理实现接口的类（即使是extends继承类也是不可以代理的）。

2. 基于继承的 CGLib 动态代理

   Cglib 是针对类来实现代理的，他的原理是对指定的目标类生成一个子类，并覆盖其中方法实现增强，但因为采用的是继承，所以不能对 final 修饰的类进行代理。Cglib 可以通过 Maven 直接进行版本引用

JDK Proxy 的优势：

- 最小化依赖关系，减少依赖意味着简化开发和维护，JDK 本身的支持，更加可靠；
- 平滑进行 JDK 版本升级，而字节码类库通常需要进行更新以保证在新版上能够使用；

Cglib 框架的优势：

- 可调用普通类，不需要实现接口；
- 高性能；

### 异常处理

Exception和RuntimeException区别

1.RuntimeException是Exception的一个子类，因此，通常说的区别，即Exception和继承Exception的RuntimeException的区别

​     RuntimeException:运行时异常，可以理解为必须运行才能发现的异常，因此运行之前可以不catch，抛异常时，则交由上级(JVM)处理，bug中断程序

​     非RuntimeException:必须有try...catch处理

2.从方法的设计者角度来说

​    RuntimeException：方法使用者无法处理的异常

​    非RuntimeException：方法使用者能处理的异常，如读取文件，使用者完全可以处理文件不处理的情况

3.从2的角度出发，可以看看异常都有哪些

   RuntimeException：NullPointerException、NumberFormatException、ArrayIndexOutOfBoundsException等转换、越界、计算类型异常

 非RuntimeException：SQLException、IOException

Error：

一般留给JDK内部自己使用，比如内存溢出OutOfMemoryError，这类严重的问题，应用进程什么都做不了，只能终止。用户抓住此类Error，一般无法处理，尽快终止往往是最安全的方式

《Effictive Java》：对于可以恢复的情况使用检查异常，对于编程中的错误使用运行异常