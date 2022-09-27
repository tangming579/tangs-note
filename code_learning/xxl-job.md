### 概要

github：https://github.com/xuxueli/xxl-job

作用：分布式任务调度平台

困惑点：

1. 如何用多线程实现数量庞大的定时任务
2. 多节点部署时，如何保证只有一个执行
3. 服务器与集成的客户端通信机制

### 项目结构

```
xxl-job

├─xxl-job-admin     服务端
│ 
├─xxl-job-core      类库
│ 
├─xxl-job-executor-samples  
│        ├─xxl-job-executor-sample-frameless     普通java示例
│        └─xxl-job-executor-sample-springboot    springboot示例     

```

### 总体设计思路

1. 由注册模块接收用户的作业信息并写入到数据库中，然后服务端启动两个线程scheduleThread、ringThread来触发任务
2. 其中scheduleThread不断扫描数据库，获取将来一段时间（nowTime+5秒）需要执行的Job，然后将需要执行的作业根据执行时间取模并写入到一个Map结构中，即ringData
3. 通过函数refreshNextValidTime更新Job下一次执行的时间并写入数据库。这样就能不断的产生作业写入ringData。
4. ringThread不断从ringData获取数据并执行作业。为了避免超时，ringTread每一次只获取两个key的数据（ringTread是按照时间的秒级对60取模，所以ringTread一共有60个可以）。如果获取一次循环时间没有到，还需要休眠，一遍保证下一次取到整秒级的key。

### 关键技术点

#### 1. 时间轮算法

```java
private volatile static Map<Integer, List<Integer>> ringData = new ConcurrentHashMap<>();

public void start() {
	ringThread = new Thread(new Runnable() {
		@Override
		public void run() {
			while (!ringThreadToStop) {
				try {
                    	//为什么不直接TimeUnit.MILLISECONDS.sleep(1000)？
                    	//保证多个调度中心同时执行时，sleep的时间点一致（都在整秒时执行）
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

#### 2. 快慢两个执行线程池



#### 3. 轻量级设计

#### 4. 路由策略

#### 5. 注册中心

### 框架缺点

1. 通过获取 DB锁来保证集群中执行任务的唯一性，调度中心数量和短任务数量都很多时，性能不高
2. 多端口问题，[参考](https://huaweicloud.csdn.net/63311521d3efff3090b51aff.html?spm=1001.2101.3001.6661.1&utm_medium=distribute.pc_relevant_t0.none-task-blog-2%7Edefault%7ECTRLIST%7Eactivity-1-116640829-blog-125324364.pc_relevant_multi_platform_whitelistv4&depth_1-utm_source=distribute.pc_relevant_t0.none-task-blog-2%7Edefault%7ECTRLIST%7Eactivity-1-116640829-blog-125324364.pc_relevant_multi_platform_whitelistv4&utm_relevant_index=1)

