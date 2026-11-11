package app.fuckclip.hook.vendor.originos

import android.util.Log
import app.fuckclip.hook.ClipboardHookContext
import app.fuckclip.hook.ClipboardSystemHook
import app.fuckclip.hook.vendor.VendorClassDiscovery
import java.lang.reflect.Field

/**
 * Discovers Vivo OriginOS and Funtouch OS clipboard classes, Binder services, and delegates and registers interceptors.
 *
 * 探测 Vivo OriginOS 与 Funtouch OS 剪贴板类、Binder 服务及委托实例并安装拦截器的系统钩子。
 */
internal class OriginOsClipboardHook(
    /**
     * Runtime hook context used to register discovered services and resolve policies.
     *
     * 用于注册已发现服务并解析规则的运行时钩子上下文。
     */
    private val context: ClipboardHookContext
) : ClipboardSystemHook {
    private val gateInterceptors = OriginOsGateInterceptors(context)
    private val inspectedClasses = HashSet<Class<*>>()

    /**
     * Probes OriginOS clipboard classes and Binder services using [classLoader] and installs hooks.
     *
     * 使用 [classLoader] 探测 OriginOS 剪贴板实现类及 Binder 服务并安装钩子。
     */
    override fun install(classLoader: ClassLoader?) {
        val loaders = VendorClassDiscovery.buildClassLoaders(classLoader, OriginOsClipboardHook::class.java)
        for (className in VIVO_CLIPBOARD_CLASSES) {
            VendorClassDiscovery.findClass(className, loaders)?.let { vivoClass ->
                installOnVivoClass(vivoClass)
            }
        }

        VendorClassDiscovery.probeBinderServices(VIVO_SERVICE_NAMES) { binderClass ->
            installOnVivoClass(binderClass)
        }
    }

    /**
     * Inspects delegate fields declared on [serviceClass] and its enclosing class to attach OriginOS hooks.
     *
     * 检查 [serviceClass] 及其外层类中声明的委托字段并补充挂载 OriginOS 钩子。
     */
    override fun onServiceDiscovered(serviceClass: Class<*>) {
        VendorClassDiscovery.inspectDelegateFieldTypes(serviceClass, ::isVivoDelegateField, ::installOnVivoClass)
        serviceClass.enclosingClass?.let { outerClass ->
            VendorClassDiscovery.inspectDelegateFieldTypes(outerClass, ::isVivoDelegateField, ::installOnVivoClass)
        }
        install(serviceClass.classLoader)
    }

    /**
     * Inspects a live clipboard [serviceInstance] to hook attached Vivo delegate objects.
     *
     * 检查运行时的剪贴板服务实例 [serviceInstance]，为挂载的 Vivo 委托实例安装钩子。
     */
    override fun onServiceInstance(serviceInstance: Any?) {
        VendorClassDiscovery.inspectDelegateInstances(serviceInstance, ::isVivoDelegateField, ::installOnVivoClass)
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
    private fun installOnVivoClass(vivoClass: Class<*>) {
        if (vivoClass.isInterface || !inspectedClasses.add(vivoClass)) {
            return
        }
        context.module.log(Log.INFO, TAG, "Found OriginOS clipboard class: ${vivoClass.name}")
        context.hookClipboardService(vivoClass, includeVendorReads = true)
        gateInterceptors.installGates(vivoClass)
    }

    private fun isVivoDelegateField(field: Field): Boolean =
        field.name == FIELD_VIVO_CLIPBOARD_SERVICE ||
            field.type.name.contains("VivoClipboard", ignoreCase = true) ||
            field.type.name.contains("VivoSupClipboard", ignoreCase = true)

    internal companion object {
        /**
         * Log tag for [OriginOsClipboardHook].
         *
         * [OriginOsClipboardHook] 的日志标签。
         */
        const val TAG = "OriginOsClipboardHook"

        /**
         * Field name on `ClipboardService` holding the Vivo clipboard service implementation.
         *
         * `ClipboardService` 中持有 Vivo 剪贴板服务实现的字段名称。
         */
        const val FIELD_VIVO_CLIPBOARD_SERVICE = "mVivoClipboardService"

        /**
         * Candidate class names of OriginOS and Funtouch OS clipboard service implementations.
         *
         * OriginOS 与 Funtouch OS 剪贴板服务扩展实现类的候选类名列表。
         */
        val VIVO_CLIPBOARD_CLASSES = listOf(
            "com.android.server.clipboard.VivoClipboardServiceImpl",
            "com.vivo.services.clipboard.VivoClipboardServiceImpl",
            "com.android.server.clipboard.VivoSupClipboardServiceImpl",
            "com.vivo.clipboard.VivoSupClipboardService"
        )

        /**
         * Candidate Binder service names registered by OriginOS clipboard extensions.
         *
         * OriginOS 剪贴板扩展在系统服务管理中心注册的候选服务名称列表。
         */
        val VIVO_SERVICE_NAMES = listOf(
            "vivo_clipboard",
            "vivoclipboard",
            "vivo_sup_clipboard"
        )
    }
}
