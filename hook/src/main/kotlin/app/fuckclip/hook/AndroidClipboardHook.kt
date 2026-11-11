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
     * Runtime hook context coordinating method interception and service discovery.
     *
     * 协调方法拦截与服务发现的运行时钩子上下文。
     */
    private val context: ClipboardHookContext
) : ClipboardSystemHook {
    /**
     * Polls ServiceManager on the main looper until the clipboard service is ready and installs hooks, ignoring [classLoader].
     *
     * 通过主线程 Looper 轮询 ServiceManager 直至剪贴板服务就绪并安装钩子（忽略 [classLoader]）。
     */
    override fun install(classLoader: ClassLoader?) {
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
                            context.module.log(Log.INFO, TAG, "Clipboard service located: ${binder.javaClass.name}")
                            context.hookClipboardService(binder.javaClass)
                            context.onServiceInstanceDiscovered(binder)
                            return
                        }
                    } catch (t: Throwable) {
                        context.module.log(Log.WARN, TAG, "Failed during clipboard service lookup", t)
                    }

                    if (attempts < MAX_ATTEMPTS) {
                        handler.postDelayed(this, POLL_INTERVAL_MS)
                    } else {
                        context.module.log(Log.ERROR, TAG, "Clipboard service lookup timed out after $attempts attempts")
                    }
                }
            }
            handler.post(pollRunnable)
        } catch (t: Throwable) {
            context.module.log(Log.ERROR, TAG, "Failed to initialize generic clipboard hook", t)
        }
    }

    private companion object {
        const val TAG = "AndroidClipboardHook"
        const val POLL_INTERVAL_MS = 500L
        const val MAX_ATTEMPTS = 120
    }
}
