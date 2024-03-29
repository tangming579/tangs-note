### 1. 概要

github：https://github.com/xuxueli/xxl-job

作用：分布式任务调度平台

困惑点：

1. 如何用多线程实现数量庞大的定时任务
2. 多节点部署时，如何保证只有一个执行
3. 服务器与集成的客户端通信机制

### 2. 项目结构

```
xxl-job

├─xxl-job-admin     调度中心服务端
│ 
├─xxl-job-core      核心类库，被服务端和客户端同时引用
│ 
├─xxl-job-executor-samples  
│        ├─xxl-job-executor-sample-frameless     普通java示例
│        └─xxl-job-executor-sample-springboot    springboot示例     

```

### 3. 总体设计思路

- 注册模块接收用户的作业信息并写入到数据库中，然后服务端启动两个线程scheduleThread、ringThread来触发任务
- 其中scheduleThread不断扫描数据库，获取将来一段时间（nowTime+5秒）需要执行的Job，然后将需要执行的作业根据执行时间取模并写入到一个Map结构中，即ringData
- 通过函数refreshNextValidTime更新Job下一次执行的时间并写入数据库。这样就能不断的产生作业写入ringData。
- ringThread不断从ringData获取数据并执行作业。为了避免超时，ringTread每一次只获取两个key的数据（ringTread是按照时间的秒级对60取模，所以ringTread一共有60个可以）。如果获取一次循环时间没有到，还需要休眠，一遍保证下一次取到整秒级的key。
- 调度器将扫描线程和执行线程隔离，因为扫描线程需要和数据库交互，且使用了排他锁，性能较慢。执行线程不与中间件交互，直接扫描时间轮，性能较高，可保证任务精准触发。

启动类：

```java
public void init() throws Exception {
        /**
        * 任务触发器线程池，负责具体任务调度
        * 分为fastTriggerPool、slowTriggerPool两个线程池
        * 当任务数量1分钟超时超过10个时，加入慢线程池
        */
        JobTriggerPoolHelper.toStart();
		/**
         * 30秒执行一次,维护注册表信息， 判断在线超时时间90s
         * 1. 删除90s未有心跳的执行器节点；jobRegistry
         * 2. 获取所有的注册节点，更新到jobGroup(执行器)
         */
        JobRegistryHelper.getInstance().start();
        // 运行事变监视器,主要失败发送邮箱,重试触发器
        JobFailMonitorHelper.getInstance().start();
        // 将丢失主机调度日志设置为失败
        JobCompleteHelper.getInstance().start();
        // 统计一些失败成功报表,删除过期日志
        JobLogReportHelper.getInstance().start();
        // 调度计时器
        JobScheduleHelper.getInstance().start();
    }
```



### 4. 关键技术点

#### 1. 调度器线程

- 每5整秒执行一次，通过写锁锁定xxl_job_lock表，这样只有一个调度中心会执行任务；

- 根据两个执行线程池最大可处理任务数，从数据库xxl_job_info表中读取未来5s可执行任务列表（trigger_next_time）

- 根据任务执行具体秒及相应策略，立即将任务放到执行线程中或将任务放到时间轮中

- 更新数据库中任务执行时间和上次执行时间

当xxjob存在集群部署的时候，存在多个线程争抢查询数据库，会造成触发器重复执行。为了防止这种情况，设置手动提交查询添加写锁。在此期间其他线程访问的时候都会阻塞等待。

```java
public void start() {
scheduleThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //为什么不直接TimeUnit.MILLISECONDS.sleep(5000)？
                    //保证多个调度中心同时执行时，sleep的时间点一致（都在整秒时执行）
                    TimeUnit.MILLISECONDS.sleep(5000 - System.currentTimeMillis() % 1000);
                } catch (InterruptedException e) {
                    if (!scheduleThreadToStop) {
                        logger.error(e.getMessage(), e);
                    }
                }
                // pre-read count: treadpool-size * trigger-qps (each trigger cost 50ms, qps = 1000/50 = 20)
                // 两个执行线程池最大可处理任务数
                int preReadCount = (XxlJobAdminConfig.getAdminConfig().getTriggerPoolFastMax() + XxlJobAdminConfig.getAdminConfig().getTriggerPoolSlowMax()) * 20;

                while (!scheduleThreadToStop) {
                    long start = System.currentTimeMillis();
                    Connection conn = null;
                    Boolean connAutoCommit = null;
                    PreparedStatement preparedStatement = null;

                    boolean preReadSuc = true;
                    try {
                        conn = XxlJobAdminConfig.getAdminConfig().getDataSource().getConnection();
                        connAutoCommit = conn.getAutoCommit();
                        // 设置手动提交
                        conn.setAutoCommit(false);
						// 获取任务调度锁表中数据，加写锁
                        preparedStatement = conn.prepareStatement("select * from xxl_job_lock where lock_name = 'schedule_lock' for update");
                        preparedStatement.execute();
                    	long start = System.currentTimeMillis();

                    	Connection conn = null;
                    	Boolean connAutoCommit = null;
                    	PreparedStatement preparedStatement = null;

                        long nowTime = System.currentTimeMillis();
                        // 1、从数据库中获取当前时间后5秒,同时最多可负载的任务列表
                        List<XxlJobInfo> scheduleList = XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().scheduleJobQuery(nowTime + PRE_READ_MS, preReadCount);
                        if (scheduleList != null && scheduleList.size() > 0) {
                            // 2、将数据推送到时间轮中
                            for (XxlJobInfo jobInfo : scheduleList) {
                                // 触发器过期时间>5s
                                if (nowTime > jobInfo.getTriggerNextTime() + PRE_READ_MS) {
                                    // 1、misfire match
                                    MisfireStrategyEnum misfireStrategyEnum = MisfireStrategyEnum.match(jobInfo.getMisfireStrategy(), MisfireStrategyEnum.DO_NOTHING);
                                    if (MisfireStrategyEnum.FIRE_ONCE_NOW == misfireStrategyEnum) {
                                        // FIRE_ONCE_NOW 》 trigger
                                        JobTriggerPoolHelper.trigger(jobInfo.getId(), TriggerTypeEnum.MISFIRE, -1, null, null, null);
                                    }

                                    // 2、更新数据库xxl_job_info表中任务的下次执行时间
                                    refreshNextValidTime(jobInfo, new Date());

                                } else if (nowTime > jobInfo.getTriggerNextTime()) {
                                    // 2.2、trigger-expire < 5s：direct-trigger && make next-trigger-time

                                    // 1、trigger
                                    JobTriggerPoolHelper.trigger(jobInfo.getId(), TriggerTypeEnum.CRON, -1, null, null, null);
                                    // 2、fresh next
                                    refreshNextValidTime(jobInfo, new Date());

                                    // next-trigger-time in 5s, pre-read again
                                    if (jobInfo.getTriggerStatus() == 1 && nowTime + PRE_READ_MS > jobInfo.getTriggerNextTime()) {

                                        // 1、make ring second
                                        int ringSecond = (int) ((jobInfo.getTriggerNextTime() / 1000) % 60);

                                        // 2、push time ring
                                        pushTimeRing(ringSecond, jobInfo.getId());

                                        // 3、fresh next
                                        refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));

                                    }

                                } else {
                                    // 2.3、trigger-pre-read：time-ring trigger && make next-trigger-time

                                    // 1、make ring second
                                    int ringSecond = (int) ((jobInfo.getTriggerNextTime() / 1000) % 60);

                                    // 2、push time ring
                                    pushTimeRing(ringSecond, jobInfo.getId());

                                    // 3、fresh next
                                    refreshNextValidTime(jobInfo, new Date(jobInfo.getTriggerNextTime()));

                                }

                            }

                            // 3、update trigger info
                            for (XxlJobInfo jobInfo : scheduleList) {
                                XxlJobAdminConfig.getAdminConfig().getXxlJobInfoDao().scheduleUpdate(jobInfo);
                            }

                        } else {
                            preReadSuc = false;
                        }

                    } catch (Exception e) {
                        if (!scheduleThreadToStop) {
                            logger.error(">>>>>>> xxl-job, JobScheduleHelper#scheduleThread error:{}", e);
                        }
                    } finally {
                        // 所有工作都处理完，再提交
                        if (conn != null) {
                            try {
                                conn.commit();
                            } catch (SQLException e) {
                                if (!scheduleThreadToStop) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                            try {
                                conn.setAutoCommit(connAutoCommit);
                            } catch (SQLException e) {
                                if (!scheduleThreadToStop) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                            try {
                                conn.close();
                            } catch (SQLException e) {
                                if (!scheduleThreadToStop) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                        }

                        // close PreparedStatement
                        if (null != preparedStatement) {
                            try {
                                preparedStatement.close();
                            } catch (SQLException e) {
                                if (!scheduleThreadToStop) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                        }
                    }
                    long cost = System.currentTimeMillis() - start;


                    // Wait seconds, align second
                    if (cost < 1000) {  // scan-overtime, not wait
                        try {
                            // pre-read period: success > scan each second; fail > skip this period;
                            TimeUnit.MILLISECONDS.sleep((preReadSuc ? 1000 : PRE_READ_MS) - System.currentTimeMillis() % 1000);
                        } catch (InterruptedException e) {
                            if (!scheduleThreadToStop) {
                                logger.error(e.getMessage(), e);
                            }
                        }
                    }

                }
            }
        });
        scheduleThread.setDaemon(true);
        scheduleThread.setName("xxl-job, admin JobScheduleHelper#scheduleThread");
        scheduleThread.start();
}
```

#### 2. 时间轮算法

概念：时间轮出自Netty中的HashedWheelTimer，是一个环形结构，可以用时钟来类比，钟面上有很多bucket，每一个bucket上可以存放多个任务，使用一个List保存该时刻到期的所有任务，同时一个指针随着时间流逝一格一格转动，并执行对应bucket上所有到期的任务。任务通过取模决定应该放入哪个bucket。和HashMap的原理类似，newTask对应put，使用List来解决 Hash 冲突。

xxl-job中的时间轮本质就是一个`concurrentHashMap`，key为执行的秒，value为要执行的job的id集合，`scheduleThread`线程会提前5秒将任务放入时间轮的list。时间轮线程每1秒执行一次，从时间轮从获取到jobIdList，最后进行调度任务；

```java
private volatile static Map<Integer, List<Integer>> ringData = new ConcurrentHashMap<>();

public void start() {
	ringThread = new Thread(new Runnable() {
		@Override
		public void run() {
			while (!ringThreadToStop) {
				try {
                    	// 每整秒触发一次
                        TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
                    } catch (InterruptedException e) {
                        if (!ringThreadToStop) {
                            logger.error(e.getMessage(), e);
                        }
                    }

                    try {
                        // 取到当前秒和上一秒的所有jobId
                        List<Integer> ringItemData = new ArrayList<>();
                        // 避免处理耗时太长，跨过刻度，向前校验一个刻度；
                        int nowSecond = Calendar.getInstance().get(Calendar.SECOND);   
                        for (int i = 0; i < 2; i++) {
                            List<Integer> tmpData = ringData.remove((nowSecond + 60 - i) % 60);
                            if (tmpData != null) {
                                ringItemData.addAll(tmpData);
                            }
                        }
                        //将所有jobId放到执行线程池中
                        if (ringItemData.size() > 0) {
                            for (int jobId : ringItemData) {
                                JobTriggerPoolHelper.trigger(jobId, TriggerTypeEnum.CRON, -1, null, null, null);
                            }
                            // clear
                            ringItemData.clear();
                        }
                    } catch (Exception e) {
                        if (!ringThreadToStop) {
                        }
                    }
                }
            }
        });
        ringThread.setDaemon(true);
        ringThread.setName("xxl-job, admin JobScheduleHelper#ringThread");
        ringThread.start();
}

private void pushTimeRing(int ringSecond, int jobId) {
        List<Integer> ringItemData = ringData.computeIfAbsent(ringSecond, k -> new ArrayList<Integer>());
        ringItemData.add(jobId);
    }
```

#### 3. 快慢两个执行线程池

```java
private volatile long minTim = System.currentTimeMillis() / 60000;  
private volatile ConcurrentMap<Integer, AtomicInteger> jobTimeoutCountMap = new ConcurrentHashMap<>();

public void start() {
    //最大200线程，最多处理1000任务
    fastTriggerPool = new ThreadPoolExecutor(10, 200, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(1000),
            ……                                
            );
    //最大100线程，最多处理2000任务
    slowTriggerPool = new ThreadPoolExecutor(10, 100, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<Runnable>(2000),
            ……                                 
            );
}
                
public void addTrigger(final int jobId, final TriggerTypeEnum triggerType,
                           final int failRetryCount,
                           final String executorShardingParam,
                           final String executorParam,
                           final String addressList) {

        // 当任务1分钟超时超过10次时，加入慢线程池
        ThreadPoolExecutor triggerPool_ = fastTriggerPool;
        AtomicInteger jobTimeoutCount = jobTimeoutCountMap.get(jobId);
        if (jobTimeoutCount != null && jobTimeoutCount.get() > 10) {      // job-timeout 10 times in 1 min
            triggerPool_ = slowTriggerPool;
        }
        // trigger
        triggerPool_.execute(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                try {
                    /**
                    /* 触发任务
                    /* 1.从数据库中获取任务和触发器详细信息
                    /* 2.根据addressList拼接执行器地址列表
                    /* 3.遍历执行器地址，使用Post方式将任务发给执行器，并记录日志
                    */
                    XxlJobTrigger.trigger(jobId, triggerType, failRetryCount, executorShardingParam, executorParam, addressList);
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                } finally {

                    // 每分钟执行一次，清理超时map
                    long minTim_now = System.currentTimeMillis() / 60000;
                    if (minTim != minTim_now) {
                        minTim = minTim_now;
                        jobTimeoutCountMap.clear();
                    }

                    // 执行时间超过500ms，加入超时map
                    long cost = System.currentTimeMillis() - start;
                    if (cost > 500) {  
                        AtomicInteger timeoutCount = jobTimeoutCountMap.putIfAbsent(jobId, new AtomicInteger(1));
                        if (timeoutCount != null) {
                            timeoutCount.incrementAndGet();
                        }
                    }

                }

            }
        });
    }                
```

#### 4. 路由策略

路由策略：当执行器集群部署时，提供丰富的路由策略，包括；

- FIRST（第一个）：固定选择第一个机器；
- LAST（最后一个）：固定选择最后一个机器；
- ROUND（轮询）
- RANDOM（随机）：随机选择在线的机器；
- CONSISTENT_HASH（一致性HASH）：每个任务按照Hash算法固定选择某一台机器，且所有任务均匀散列在不同机器上。
- LEAST_FREQUENTLY_USED（最不经常使用）：使用频率最低的机器优先被选举；
- LEAST_RECENTLY_USED（最近最久未使用）：最久未使用的机器优先被选举；
- FAILOVER（故障转移）：按照顺序依次进行心跳检测，第一个心跳检测成功的机器选定为目标执行器并发起调度；
- BUSYOVER（忙碌转移）：按照顺序依次进行空闲检测，第一个空闲检测成功的机器选定为目标执行器并发起调度；
- SHARDING_BROADCAST(分片广播)：广播触发对应集群中所有机器执行一次任务，同时系统自动传递分片参数；可根据分片参数开发分片任务；

- 子任务：每个任务都拥有一个唯一的任务ID(任务ID可以从任务列表获取)，当本任务执行结束并且执行成功时，将会触发子任务ID所对应的任务的一次主动调度。

#### 5. 注册中心

xxl-job 的分布式注册中心，其注册表使用mysql来进行存储，心跳机制通过http请求实现

xxl-job中服务注册需要绑定一个执行器作为载体，当服务注册后首先会将注册机器信息存入注册表中，扫描线程会不断扫描注册表，将注册表中的机器根据appName配置绑定到执行器上。


#### 6. 故障转移

```java
//代码位置：com.xxl.job.admin.core.route.strategy.ExecutorRouteFailover#route
public ReturnT<String> route(TriggerParam triggerParam, List<String> addressList) {

        StringBuffer beatResultSB = new StringBuffer();
        for (String address : addressList) {
            // beat
            ReturnT<String> beatResult = null;
            try {
                ExecutorBiz executorBiz = XxlJobScheduler.getExecutorBiz(address);
                //心跳检测
                beatResult = executorBiz.beat();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                beatResult = new ReturnT<String>(ReturnT.FAIL_CODE, ""+e );
            }
            beatResultSB.append( (beatResultSB.length()>0)?"<br><br>":"")
                    .append(I18nUtil.getString("jobconf_beat") + "：")
                    .append("<br>address：").append(address)
                    .append("<br>code：").append(beatResult.getCode())
                    .append("<br>msg：").append(beatResult.getMsg());

            // beat success
            //心跳正常，返回执行器地址
            if (beatResult.getCode() == ReturnT.SUCCESS_CODE) {

                beatResult.setMsg(beatResultSB.toString());
                beatResult.setContent(address);
                return beatResult;
            }
        }
        return new ReturnT<String>(ReturnT.FAIL_CODE, beatResultSB.toString());

}
```

ExecutorRouteFailover是失败转移路由，route方法遍历执行器地址，然后发送心跳给执行器服务，如果心跳正常，则成功返回该执行器地址，否则返回失败码。

### 框架缺点

1. 通过获取 DB锁来保证集群中执行任务的唯一性，当调度中心数量和短任务数量都很多时，性能不高
2. 多端口问题：客户端为了和调度中心通信，需要单独给定一个端口，其实如果是spring项目，完全可以像consul一样共享接口端口

