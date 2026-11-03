package app.fuckclip.hook

import android.content.Context
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log

/**
 * Generic path installer that queries ServiceManager for the clipboard service instance.
 *
 * 通过轮询 ServiceManager 获取剪贴板服务实例的通用路径安装器。
 */
internal class AndroidClipboardHook(
    /**
     * Target clipboard hook coordinator managing method interception.
     *
     * 管理方法拦截的剪贴板钩子协调器。
     */
    private val hook: ClipboardHook
) {
    /**
     * Polls ServiceManager until the clipboard service is available and installs hooks.
     *
     * 轮询 ServiceManager 直至剪贴板服务就绪并安装钩子。
     */
    fun install() {
        try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getDeclaredMethod("getService", String::class.java)
            getServiceMethod.isAccessible = true

            val handler = Handler(Looper.getMainLooper())
            val pollRunnable = object : Runnable {
                private var attempts = 0

                override fun run() {
                    attempts++
                    try {
                        val binder = getServiceMethod.invoke(null, Context.CLIPBOARD_SERVICE) as? IBinder
                        if (binder != null) {
                            hook.module.log(Log.INFO, TAG, "Clipboard service located: ${binder.javaClass.name}")
                            hook.hookClipboardService(binder.javaClass)
                            return
                        }
                    } catch (t: Throwable) {
                        hook.module.log(Log.WARN, TAG, "Failed during clipboard service lookup", t)
                    }

                    if (attempts < MAX_ATTEMPTS) {
                        handler.postDelayed(this, POLL_INTERVAL_MS)
                    } else {
                        hook.module.log(Log.ERROR, TAG, "Clipboard service lookup timed out after $attempts attempts")
                    }
                }
            }
            handler.post(pollRunnable)
        } catch (t: Throwable) {
            hook.module.log(Log.ERROR, TAG, "Failed to initialize generic clipboard hook", t)
        }
    }

    private companion object {
        const val TAG = "AndroidClipboardHook"
        const val POLL_INTERVAL_MS = 500L
        const val MAX_ATTEMPTS = 120
    }
}
