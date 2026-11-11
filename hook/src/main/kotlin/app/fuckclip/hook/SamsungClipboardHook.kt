package app.fuckclip.hook

import android.annotation.SuppressLint
import android.os.Parcel
import android.util.Log
import io.github.libxposed.api.XposedInterface

/**
 * Fallback installer hooking IClipboard.Stub.onTransact for Samsung and custom vendor ROMs.
 *
 * 为三星及各厂商定制 ROM 挂钩 IClipboard.Stub.onTransact 的兜底路径安装器。
 */
internal class SamsungClipboardHook(
    /**
     * Runtime hook context coordinating method interception and service discovery.
     *
     * 协调方法拦截与服务发现的运行时钩子上下文。
     */
    private val context: ClipboardHookContext
) : ClipboardSystemHook {
    /**
     * Hooks IClipboard.Stub.onTransact to capture underlying clipboard service instances, ignoring [classLoader].
     *
     * 挂钩 IClipboard.Stub.onTransact 兜底入口（忽略 [classLoader]）以捕获底层剪贴板服务实例。
     */
    @SuppressLint("PrivateApi")
    override fun install(classLoader: ClassLoader?) {
        try {
            val stubClass = Class.forName("android.content.IClipboard\$Stub")
            val onTransactMethod = stubClass.getDeclaredMethod(
                "onTransact",
                Int::class.javaPrimitiveType,
                Parcel::class.java,
                Parcel::class.java,
                Int::class.javaPrimitiveType
            )
            onTransactMethod.isAccessible = true

            context.module.hook(onTransactMethod)
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    chain.thisObject?.let { service ->
                        context.hookClipboardService(service.javaClass)
                        context.onServiceInstanceDiscovered(service)
                    }
                    chain.proceed()
                }

            context.module.log(Log.INFO, TAG, "Hooked IClipboard.Stub.onTransact fallback")
        } catch (t: Throwable) {
            context.module.log(Log.WARN, TAG, "Failed to hook IClipboard.Stub.onTransact fallback", t)
        }
    }

    private companion object {
        const val TAG = "SamsungClipboardHook"
    }
}
