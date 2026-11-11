package app.fuckclip.hook

/**
 * Lifecycle contract implemented by AOSP and vendor clipboard hook installers.
 *
 * AOSP 及各厂商剪贴板钩子安装器实现的统一生命周期契约。
 */
internal interface ClipboardSystemHook {
    /**
     * Installs clipboard service hooks using the optional system server [classLoader].
     *
     * 使用可选的 system_server 类加载器 [classLoader] 安装剪贴板服务钩子。
     */
    fun install(classLoader: ClassLoader? = null)

    /**
     * Inspects a discovered clipboard [serviceClass] to attach any system-specific hooks or delegate probes.
     *
     * 检查已发现的剪贴板服务类 [serviceClass]，补充挂载特定系统钩子或委托探测逻辑。
     */
    fun onServiceDiscovered(serviceClass: Class<*>) {}

    /**
     * Inspects a live clipboard [serviceInstance] to hook attached system-specific delegate objects.
     *
     * 检查运行时的剪贴板服务实例 [serviceInstance]，为挂载的系统专有委托对象安装钩子。
     */
    fun onServiceInstance(serviceInstance: Any?) {}

    /**
     * Clears cached hook state during module hot reload.
     *
     * 在模块热重载时清理内部缓存的挂钩状态。
     */
    fun reset() {}
}
