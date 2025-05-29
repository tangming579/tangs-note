## controller-runtime

controller-runtime 提供了一组用于构建控制器的库。可以快速构建出来一个controller 服务。

我们仅需要在controller-runtime的指定watch的resource object和reconcile业务处理逻辑即可, 无需管理controller的生命周期和event的异常处理，Operator脚手架Kubebuilder和Operator SDK也都是基于controller-runtime来完成的.

### main函数分析

```golang
func main() {
	
    // 1. 构建controllerManager
	mgr, err := ctrl.NewManager(ctrl.GetConfigOrDie(), ctrl.Options{
		Scheme:                 scheme,
		MetricsBindAddress:     metricsAddr,
		Port:                   9443,
		HealthProbeBindAddress: probeAddr,
		LeaderElection:         enableLeaderElection,
		LeaderElectionID:       "86f835c3.example.com",
	})

	// 2. 将我们编写的reconciler添加到controllerManager中
	if err = (&controllers.MemcachedReconciler{
		Client: mgr.GetClient(),
		Scheme: mgr.GetScheme(),
	}).SetupWithManager(mgr); err != nil {
		setupLog.Error(err, "unable to create controller", "controller", "Memcached")
		os.Exit(1)
	}

	// 3. 启动controllerManager
	if err := mgr.Start(ctrl.SetupSignalHandler()); err != nil {
		setupLog.Error(err, "problem running manager")
		os.Exit(1)
	}
}
```

###  SetupWithManager

- 构建一个controller，并加入到controllerManager中
- 为该controller设置watch对象，也就是`cachev1alpha1.Memcached{}`
- 为该controller设置Reconciler，也就是`MemcachedReconciler`

```go
// SetupWithManager sets up the controller with the Manager.
func (r *MemcachedReconciler) SetupWithManager(mgr ctrl.Manager) error {
	return ctrl.NewControllerManagedBy(mgr).
		For(&cachev1alpha1.Memcached{}).
		Complete(r)
}
```

### Managers

### Controllers

### Reconcilers