### 概要

github：https://github.com/xuxueli/xxl-job

作用：分布式任务调度平台

困惑点：

1. 如何用多线程实现数量庞大的定时任务
2. 多节点部署时，如何保证只有一个执行
3. 服务器与集成的客户端如何通信

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

### 关键技术点

#### 1. 时间轮算法

#### 2. 快慢两个执行线程池

#### 3. 轻量级设计

#### 4. 路由策略

#### 5. 注册中心

### 框架缺点

1. 多端口问题，[参考](https://huaweicloud.csdn.net/63311521d3efff3090b51aff.html?spm=1001.2101.3001.6661.1&utm_medium=distribute.pc_relevant_t0.none-task-blog-2%7Edefault%7ECTRLIST%7Eactivity-1-116640829-blog-125324364.pc_relevant_multi_platform_whitelistv4&depth_1-utm_source=distribute.pc_relevant_t0.none-task-blog-2%7Edefault%7ECTRLIST%7Eactivity-1-116640829-blog-125324364.pc_relevant_multi_platform_whitelistv4&utm_relevant_index=1)

