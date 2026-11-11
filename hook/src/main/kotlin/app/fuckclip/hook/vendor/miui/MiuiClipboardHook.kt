package app.fuckclip.hook.vendor.miui

import android.util.Log
import app.fuckclip.hook.ClipboardHookContext
import app.fuckclip.hook.ClipboardSystemHook
import app.fuckclip.hook.vendor.VendorClassDiscovery
import java.lang.reflect.Modifier

/**
 * Discovers Xiaomi MIUI and HyperOS clipboard stub and service classes and registers interceptors.
 *
 * 探测小米 MIUI 与澎湃 HyperOS 剪贴板 Stub 及服务实现类并安装拦截器的系统钩子。
 */
internal class MiuiClipboardHook(
    /**
     * Runtime hook context used to register discovered services and resolve policies.
     *
     * 用于注册已发现服务并解析规则的运行时钩子上下文。
     */
    private val context: ClipboardHookContext
) : ClipboardSystemHook {
    private val gateInterceptors = MiuiGateInterceptors(context)
    private val inspectedClasses = HashSet<Class<*>>()

    /**
     * Probes MIUI / HyperOS stub and service classes using [classLoader] and installs hooks.
     *
     * 使用 [classLoader] 探测 MIUI / HyperOS Stub 与服务类并安装钩子。
     */
    override fun install(classLoader: ClassLoader?) {
        val loaders = VendorClassDiscovery.buildClassLoaders(classLoader, MiuiClipboardHook::class.java)

        resolveStubImplementationClass(loaders)?.let { stubImplClass ->
            installOnStubClass(stubImplClass)
        }

        for (className in MIUI_STUB_CLASSES) {
            VendorClassDiscovery.findClass(className, loaders)?.let { stubClass ->
                installOnStubClass(stubClass)
            }
        }

        VendorClassDiscovery.findClass(CLIPBOARD_SERVICE_CLASS, loaders)?.let { serviceClass ->
            gateInterceptors.installServiceGates(serviceClass)
        }
    }

    /**
     * Installs MIUI / HyperOS hooks against a newly discovered clipboard [serviceClass] and its enclosing class.
     *
     * 针对新发现的剪贴板服务类 [serviceClass] 及其外层类补充安装 MIUI / HyperOS 钩子。
     */
    override fun onServiceDiscovered(serviceClass: Class<*>) {
        gateInterceptors.installServiceGates(serviceClass)
        serviceClass.enclosingClass?.let { outerClass ->
            gateInterceptors.installServiceGates(outerClass)
        }
        install(serviceClass.classLoader)
    }

    /**
     * Clears discovered class and gate tracking state on module hot reload.
     *
     * 在模块热重载时清空已扫描类与闸门追踪状态。
     */
    @Synchronized
    override fun reset() {
        inspectedClasses.clear()
        gateInterceptors.reset()
    }

    @Synchronized
    private fun installOnStubClass(stubClass: Class<*>) {
        if (stubClass.isInterface || !inspectedClasses.add(stubClass)) {
            return
        }
        context.module.log(Log.INFO, TAG, "Found MIUI/HyperOS clipboard stub class: ${stubClass.name}")
        context.hookClipboardService(stubClass, includeVendorReads = true)
        gateInterceptors.installStubGates(stubClass)
    }

    private fun resolveStubImplementationClass(loaders: List<ClassLoader>): Class<*>? {
        val stubHolder = VendorClassDiscovery.findClass(STUB_HOLDER_CLASS, loaders) ?: return null
        for (accessorName in listOf("get", "getInstance")) {
            try {
                val accessor = stubHolder.getDeclaredMethod(accessorName)
                if (Modifier.isStatic(accessor.modifiers)) {
                    accessor.isAccessible = true
                    val instance = accessor.invoke(null)
                    if (instance != null) {
                        return instance.javaClass
                    }
                }
            } catch (_: Throwable) {}
        }
        return null
    }

    internal companion object {
        /**
         * Log tag for [MiuiClipboardHook].
         *
         * [MiuiClipboardHook] 的日志标签。
         */
        const val TAG = "MiuiClipboardHook"

        /**
         * Class name of the MIUI clipboard stub singleton holder.
         *
         * MIUI 剪贴板 Stub 单例持有类的类名。
         */
        const val STUB_HOLDER_CLASS = "com.android.server.clipboard.ClipboardServiceStub"

        /**
         * Class name of the system clipboard service.
         *
         * 系统剪贴板服务实现类的类名。
         */
        const val CLIPBOARD_SERVICE_CLASS = "com.android.server.clipboard.ClipboardService"

        /**
         * Candidate class names of MIUI and HyperOS clipboard stub implementations.
         *
         * MIUI 与 HyperOS 剪贴板 Stub 扩展实现类的候选类名列表。
         */
        val MIUI_STUB_CLASSES = listOf(
            "com.android.server.clipboard.ClipboardServiceStubImpl",
            "com.android.server.clipboard.ClipboardServiceInjector",
            "com.miui.server.clipboard.ClipboardServiceStubImpl",
            "com.miui.server.clipboard.MiuiClipboardService"
        )
    }
}
