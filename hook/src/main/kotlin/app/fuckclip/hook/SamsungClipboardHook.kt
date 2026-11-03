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
     * Target clipboard hook coordinator managing method interception.
     *
     * 管理方法拦截的剪贴板钩子协调器。
     */
    private val hook: ClipboardHook
) {
    /**
     * Hooks IClipboard.Stub.onTransact to capture the underlying service instance during any clipboard IPC transaction.
     *
     * 挂钩 IClipboard.Stub.onTransact 以在任何剪贴板 IPC 事务期间捕获底层服务实例。
     */
    @SuppressLint("PrivateApi")
    fun install() {
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

            hook.module.hook(onTransactMethod)
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    chain.thisObject?.let { service ->
                        hook.hookClipboardService(service.javaClass)
                    }
                    chain.proceed()
                }

            hook.module.log(Log.INFO, TAG, "Hooked IClipboard.Stub.onTransact fallback")
        } catch (t: Throwable) {
            hook.module.log(Log.WARN, TAG, "Failed to hook IClipboard.Stub.onTransact fallback", t)
        }
    }

    private companion object {
        const val TAG = "SamsungClipboardHook"
    }
}
