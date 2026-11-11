package app.fuckclip.hook.vendor.coloros

import android.util.Log
import app.fuckclip.hook.ClipboardHookContext
import app.fuckclip.hook.ClipboardSystemHook
import app.fuckclip.hook.vendor.VendorClassDiscovery
import java.lang.reflect.Field

/**
 * Discovers OPPO ColorOS, OnePlus OxygenOS, and Realme UI clipboard extension classes and delegates and registers interceptors.
 *
 * 探测 OPPO ColorOS、一加 OxygenOS 与 Realme UI 剪贴板扩展类、Binder 服务及委托实例并安装拦截器的系统钩子。
 */
internal class ColorOsClipboardHook(
    /**
     * Runtime hook context used to register discovered services and resolve policies.
     *
     * 用于注册已发现服务并解析规则的运行时钩子上下文。
     */
    private val context: ClipboardHookContext
) : ClipboardSystemHook {
    private val gateInterceptors = ColorOsGateInterceptors(context)
    private val inspectedClasses = HashSet<Class<*>>()

    /**
     * Probes ColorOS extension classes and Binder services using [classLoader] and installs hooks.
     *
     * 使用 [classLoader] 探测 ColorOS 剪贴板扩展类及 Binder 服务并安装钩子。
     */
    override fun install(classLoader: ClassLoader?) {
        val loaders = VendorClassDiscovery.buildClassLoaders(classLoader, ColorOsClipboardHook::class.java)
        for (className in OPLUS_CLIPBOARD_CLASSES) {
            VendorClassDiscovery.findClass(className, loaders)?.let { oplusClass ->
                installOnOplusClass(oplusClass)
            }
        }

        VendorClassDiscovery.probeBinderServices(OPLUS_SERVICE_NAMES) { binderClass ->
            installOnOplusClass(binderClass)
        }
    }

    /**
     * Inspects delegate fields declared on [serviceClass] and its enclosing class to attach ColorOS hooks.
     *
     * 检查 [serviceClass] 及其外层类中声明的扩展委托字段并补充挂载 ColorOS 钩子。
     */
    override fun onServiceDiscovered(serviceClass: Class<*>) {
        VendorClassDiscovery.inspectDelegateFieldTypes(serviceClass, ::isOplusDelegateField, ::installOnOplusClass)
        serviceClass.enclosingClass?.let { outerClass ->
            VendorClassDiscovery.inspectDelegateFieldTypes(outerClass, ::isOplusDelegateField, ::installOnOplusClass)
        }
        install(serviceClass.classLoader)
    }

    /**
     * Inspects a live clipboard [serviceInstance] to hook attached ColorOS extension delegates.
     *
     * 检查运行时的剪贴板服务实例 [serviceInstance]，为挂载的 ColorOS 扩展委托实例安装钩子。
     */
    override fun onServiceInstance(serviceInstance: Any?) {
        VendorClassDiscovery.inspectDelegateInstances(serviceInstance, ::isOplusDelegateField, ::installOnOplusClass)
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
    private fun installOnOplusClass(oplusClass: Class<*>) {
        if (oplusClass.isInterface || !inspectedClasses.add(oplusClass)) {
            return
        }
        context.module.log(Log.INFO, TAG, "Found ColorOS clipboard class: ${oplusClass.name}")
        context.hookClipboardService(oplusClass, includeVendorReads = true)
        gateInterceptors.installGates(oplusClass)
    }

    private fun isOplusDelegateField(field: Field): Boolean =
        field.name in OPLUS_EXT_FIELD_NAMES ||
            field.type.name.contains("ClipboardServiceExt", ignoreCase = true) ||
            field.type.name.contains("OplusClipboard", ignoreCase = true) ||
            field.type.name.contains("ColorClipboard", ignoreCase = true)

    internal companion object {
        /**
         * Log tag for [ColorOsClipboardHook].
         *
         * [ColorOsClipboardHook] 的日志标签。
         */
        const val TAG = "ColorOsClipboardHook"

        /**
         * Field names on `ClipboardService` holding the ColorOS clipboard extension delegate.
         *
         * `ClipboardService` 中持有 ColorOS 剪贴板扩展委托实例的字段名称集合。
         */
        val OPLUS_EXT_FIELD_NAMES = setOf(
            "mClipboardServiceExt",
            "mCsExt",
            "mOplusClipboardServiceExt"
        )

        /**
         * Candidate class names of ColorOS, OxygenOS, and Realme UI clipboard extension implementations.
         *
         * ColorOS、OxygenOS 与 Realme UI 剪贴板扩展实现类的候选类名列表。
         */
        val OPLUS_CLIPBOARD_CLASSES = listOf(
            "com.android.server.clipboard.ClipboardServiceExtImpl",
            "com.oplus.server.content.ClipboardServiceExtImpl",
            "com.oplus.server.clipboard.ClipboardServiceExtImpl",
            "com.color.server.clipboard.ColorClipboardService"
        )

        /**
         * Candidate Binder service names registered by Oplus clipboard extensions.
         *
         * Oplus 剪贴板扩展在系统服务管理中心注册的候选服务名称列表。
         */
        val OPLUS_SERVICE_NAMES = listOf(
            "oplus_clipboard",
            "oplusclipboard",
            "color_clipboard"
        )
    }
}
