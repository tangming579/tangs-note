### OAPServerBootstrap

位置：oap-server/server-bootstrap/

```java
public class OAPServerBootstrap {
    public static void start() {
        //读取系统配置的mode值，init表示初始化模式，做所有初始化的事情。no-init表示非初始化模式，不进行存储初始化
        String mode = System.getProperty("mode");
        RunningMode.setMode(mode);

        //创建ApplicationConfiguration对象
        ApplicationConfigLoader configLoader = new ApplicationConfigLoader();
        ModuleManager manager = new ModuleManager();
        try {
            //通过load()方法加载配置文件
            //读取resources文件夹下的application.yml文件，然后读取系统配置，覆盖读取的application.yml文件的值
            ApplicationConfiguration applicationConfiguration = configLoader.load();
            //初始化模块 通过spi获取所有Module实现,基于yml配置加载spi中存在的相关实现
        	//init包含Module的初始化以及子组件ModuleProvider的初始化 
            manager.init(applicationConfiguration);

            manager.find(TelemetryModule.NAME)
                   .provider()
                   .getService(MetricsCreator.class)
                   .createGauge("uptime", "oap server start up time", MetricsTag.EMPTY_KEY, MetricsTag.EMPTY_VALUE)
                   // Set uptime to second
                   .setValue(System.currentTimeMillis() / 1000d);

            if (RunningMode.isInitMode()) {
                log.info("OAP starts up in init mode successfully, exit now...");
                System.exit(0);
            }
        } catch (Throwable t) {
            log.error(t.getMessage(), t);
            System.exit(1);
        }
    }
}
```

### manager.init：模块加载及启动

```java
public void init(ApplicationConfiguration applicationConfiguration) 
    String[] moduleNames = applicationConfiguration.moduleList();
	//通过spi加载所有实现的Module和ModuleProvider
    ServiceLoader<ModuleDefine> moduleServiceLoader = ServiceLoader.load(ModuleDefine.class);
    ServiceLoader<ModuleProvider> moduleProviderLoader = ServiceLoader.load(ModuleProvider.class);
    //yml中配置了相关module,源码实现了一些模块插件,spi发现所有实现的插件
    HashSet<String> moduleSet = new HashSet<>(Arrays.asList(moduleNames));
    for (ModuleDefine module : moduleServiceLoader) {
        if (moduleSet.contains(module.name())) {
            //执行生命周期方法prepare[完成子组件实例化以及相关生命周期执行]
            module.prepare(this, applicationConfiguration.getModuleConfiguration(module.name()), moduleProviderLoader);
            //加入ModuleManager
            loadedModules.put(module.name(), module);
            moduleSet.remove(module.name());
        }
    }
    // Finish prepare stage
    isInPrepareStage = false;
    if (moduleSet.size() > 0) {
        throw new ModuleNotFoundException(moduleSet.toString() + " missing.");
    }
    BootstrapFlow bootstrapFlow = new BootstrapFlow(loadedModules);
	//启动引导程序 ,完成Provider实现的实例化
    bootstrapFlow.start(this);
	// module和moduleProvider启动完毕后的事件通知
    bootstrapFlow.notifyAfterCompleted();
}
```

### ModuleDefine.prepare

### bootstrapFlow.start

### StorageModuleElasticsearch7Provider

- prepare主要完成StorageModule中定义的service实现与注册
- start完成esclient启动,并根据mode配置决定是否创建索引
- notifyAfterCompleted未做实现