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

- wait:导致当前线程等待，这个方法会释放锁，所以需要在同步代码块中调用(否则会发生
  IllegalMonitorStateException的异常
- notify:随机选择一个等待中的线程将其唤醒；notify()调用后，并不是马上就释放对象锁
  的，而是在相应的synchronized(){}语句块执行结束，自动释放锁后，JVM会在wait()对象
  锁的线程中随机选取一线程，赋予其对象锁，唤醒线程，继续执行。
- notifyAll:将所有等待的线程唤醒

- join:等待调用该方法的线程执行完毕后再往下继续执行(该方法也要捕获异常)
- sleep:使调用该方法的线程暂停执行一段时间，让其他线程有机会继续执行，但它并不释
  放对象锁。也就是如果有Synchronized同步块，其他线程仍然不同访问共享数据(注意该
  方法要捕获异常)
- yeild:与sleep()类似，只是不能由用户指定暂停多长时间，并且yield()方法只能让同优先
  级的线程有执行的机会

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

- **ArrayBlockingQueue：** 一个基于数组的有界阻塞队列，此队列按先进先出原则对元素进行排序。
- **LinkedBlockingQueue：** 一个基于链表结构的无界阻塞队列，此队列按先进先出排序元素，吞吐量通常要高于ArrayBlockingQueue。静态工厂方法Executors.newFixedThreadPool()使用了这个队列。
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

AQS的实现依赖内部的同步队列，也就是FIFO的双向队列，如果当前线程竞争锁失败，那么AsQS会把当前线程以及等待状态信息构造成一个Node加入到同步队列中，同时再阻塞该线程。当获取锁的线程释放锁以后，会从队列中唤醒一个阻塞的节点(线程)。

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

### fail-fast机制

fail-fast是一种错误检测机制，一旦检测到可能发生错误，就立马抛出异常，程序不继续往下执行。

解决方法

1. 在单线程的遍历过程中，如果要进行remove操作，可以调用迭代器的remove方法而不是集合类的remove方法
2. 使用java并发包(java.util.concurrent)中的类来代替 ArrayList 和hashMap。

## 3. JVM


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

### 双亲委派机制

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

### 装箱与拆箱

有了基本类型之后为什么还要有包装器类型呢？核心：让基本类型具备对象的特征,实现更多的功能.

装箱过程是通过调用包装器的valueOf方法实现的，而拆箱过程是通过调用包装器的 xxxValue方法实现的。（xxx代表对应的基本数据类型）

### GC算法

**分代收集算法**

根据对象存货周期的不同将内存划分为几块，一般把java堆分为新生代和老年代，进行分代回收

**1. 新生代**：每次垃圾回收都有大量对象死去，之后少量存活，适合采用复制算法。

- 复制算法：将内存划分为大小相同的两块（Eden和Survior默认比例是8:1），每次只使用其中一块。当这一块内存用完了，就将还存活着的对象复制到另外一块上面，然后再把已使用的内存空间一次清理掉，不足：

  1. 对象存活率较高时要进行较多的复制操作，效率将会变低
  2. 如果不想浪费50%的空间，需要有额外的空间进行分配担保，以应对被是使用的内粗中所有对象都100%存活的极端情况

  分配担保：新生代内存不足时，把新生代的存活的对象搬到老生代，这里老年代是担保人

**2. 老年代**：对象存活率高，没有额外空间对他进行分配担保，适合采用标记-整理、标记-清除搜集算法

- 标记-清除收集算法：首先标记出所有需要回收的对象，在标记完成后统一回收所有被标记的对象，不足：
  1. 标记和清除效率都不高
  2. 标记清除后悔产生大量不连续的内存碎片

- 标记-整理算法：标记过程与 “标记-清除” 算法一致，但后续步骤部署直接对可回收对象进行清理，而是让所有存活对象都向一端移动，然后直接清理掉端边界以外内存

#### MinorGC

从年轻代空间（包括 Eden 和 Survivor 区域）回收内存被称为 Minor GC，触发条件

- Eden区域满了
- 新生对象需要分配到新生代的Eden，当Eden区的内存不够时

#### Major GC/Full GC

- MajorGC：清理老年代，Major GC发生过程常常伴随一次Minor

- FullGC：Full GC可以看做是Major GC+Minor GC的一整个过程，是清理整个堆空间（包括年轻代和老年代），触发条件
  - 上面Minor GC时介绍中Survivor空间不足时，判断是否允许担保失败，如果不允许则进行Full GC。如果允许，并且每次晋升到老年代的对象平均大小>老年代最大可用连续内存空间，也会进行Full GC。
  - MinorGC后存活的对象超过了老年代剩余空间
  - 方法区内存不足时
  - System.gc()，可用通过-XX:+ DisableExplicitGC来禁止调用System.g
  - CMS GC异常，CMS运行期间预留的内存无法满足程序需要，就会出现一次“Concurrent Mode Failure”失败，会触发Full GC

### JVM调优

## 4. Spring

### 对Spring Boot理解

SpringBoot是一个快速开发框架，快速的将一些常用的第三方依赖整合（原理：通过Maven子父工程的方式），简化XML配置，全部采用注解形式，内置Http服务器（Jetty和Tomcat），最终以java应用程序进行执行，它是为了简化Spring应用的创建、运行、调试、部署等而出现的，使用它可以做到专注于Spring应用的开发，而无需过多关注XML的配置。

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

## 5.Spring Cloud

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

a. JVM启动，读取到javaagent参数，初始化其指定的Jar包，调用其的Agent_OnLoad函数。

b. 在Agent_OnLoad函数中，会通过获取JVM实例，调用RegisterEvent初始化注册JVMTI的事件回调函数，获取ClassFileLoadHook。

c. 同时Agent_OnLoad函数中创建完成的sun.instrument.InstrumentationImpl中调用loadClassAndCallPremain，去初始化Premain-Class指定类的premain方法。

d. 执行Jar包中的premain函数，通过Instrumentation向JVM注册Agent的`ClassFileTransform`实例，这一步很关键。

e. Agent初始化完毕后，JVM调用main函数。JVM运行过程中在ClassLoader加载class文件之前，JVM每次都会（注意是每次，这就使得其具备了在运行时修改类方法体的能力）调用ClassFileLoadHook回调，该回调会调用ClassFileTransformer的transform函数，生成字节码。由于是在解析class之前，以二进制流的形式，对后续解析无影响。

## 6. IO

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

## 7. MyBatis

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

 **Mabatis中#{}和${}的区别**

${} 则只是简单的字符串替换；#{} 在预处理时，会把参数部分用一个占位符 ? 代替

#{} 的参数替换是发生在 DBMS 中，而 ${} 则发生在动态解析过程中

优先使用 #{}。因为 ${} 会导致 sql 注入的问题

**通常一个Xml映射文件，都会写一个Dao接口与之对应，请问，这个Dao接口的工作原理是什么？Dao接口里的方法，参数不同时，方法能重载吗？**

Dao接口里的方法，是不能重载的，因为是全限名+方法名的保存和寻找策略。

Dao接口的工作原理是JDK动态代理，Mybatis运行时会使用JDK动态代理为Dao接口生成代理proxy对象，代理对象proxy会拦截接口方法，转而执行MappedStatement所代表的sql，然后将sql执行结果返回。

## 8. 其他

### 基础

**为什么string不可以被继承**

出于安全性和效率考虑

- String对象是缓存在字符串池中的，继承会破坏String的不可变性、缓存性以及hascode的计算方式
- 人们非常流行把String作为HashMap的键值，如果String可变，就会产生不同的Hash值
- String被广泛用于Java类的参数，如果String是可变的，就会导致严重的安全威胁
- 为了线程安全(字符串自己便是线程安全的)
- String的不可变性为Java的类加载机制的安全性提供了根本的保障

为什么decimal精度高

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