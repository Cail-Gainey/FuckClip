package app.fuckclip.hook

import io.github.libxposed.api.XposedModule

/**
 * Decoupled runtime contract exposing module services, policy resolution, and audit logging to hook installers.
 *
 * 向各钩子安装器提供模块能力、规则解析与审计日志接口的解耦运行时契约。
 */
internal interface ClipboardHookContext {
    /**
     * Active Xposed module instance used to register method hooks and emit diagnostic logs.
     *
     * 用于注册方法钩子并输出诊断日志的活动 Xposed 模块实例。
     */
    val module: XposedModule

    /**
     * Scans [clazz] for clipboard entry points and installs read, write, and metadata hooks; enables vendor reads when [includeVendorReads] is true.
     *
     * 扫描 [clazz] 中的剪贴板入口方法并安装读取、写入及元数据钩子；当 [includeVendorReads] 为 true 时同时识别厂商专有读取入口。
     */
    fun hookClipboardService(clazz: Class<*>, includeVendorReads: Boolean = false)

    /**
     * Notifies registered system hooks when a live clipboard [serviceInstance] is discovered.
     *
     * 当发现运行时剪贴板服务实例 [serviceInstance] 时通知已注册的系统钩子。
     */
    fun onServiceInstanceDiscovered(serviceInstance: Any?)

    /**
     * Resolves the calling application package name from the current Binder UID.
     *
     * 根据当前 Binder 调用方 UID 解析发起请求的应用包名。
     */
    fun resolveCallingPackage(): String?

    /**
     * Checks whether [packageName] or the current Binder caller belongs to the system framework.
     *
     * 判断 [packageName] 或当前 Binder 调用方是否属于系统框架进程。
     */
    fun isFrameworkCaller(packageName: String): Boolean

    /**
     * Resolves the configured permission rule for [packageName] according to [isWrite].
     *
     * 根据是否为写入操作 [isWrite] 解析指定应用 [packageName] 的权限规则。
     */
    fun resolvePackageRule(packageName: String, isWrite: Boolean): Int

    /**
     * Emits an audit log broadcast for [packageName] recording [action], [allowed], [detail], and optional clipboard [content].
     *
     * 为 [packageName] 发送剪贴板审计日志广播，记录操作类型 [action]、放行结果 [allowed]、详情 [detail] 及可选内容 [content]。
     */
    fun notifyHookLog(
        packageName: String,
        action: String,
        allowed: Boolean,
        detail: String,
        content: String? = null
    )
}
