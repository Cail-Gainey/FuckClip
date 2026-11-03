package app.fuckclip.hook

import android.content.Context

/**
 * Resolves the system server [Context] through reflection on framework internal classes.
 *
 * 通过反射系统框架内部类解析获取 system_server 进程的全局系统级 [Context]。
 */
internal object SystemContextResolver {
    @Volatile
    private var cachedContext: Context? = null

    /**
     * Resolves and caches the system server [Context].
     *
     * 解析并缓存 system_server 进程的系统级 [Context]。
     */
    fun getSystemContext(): Context? {
        cachedContext?.let { return it }

        val context = invokeStaticContext("android.app.AppGlobals", "getInitialApplication")
            ?: getActivityThreadSystemContext()
            ?: invokeStaticContext("android.app.ActivityThread", "currentApplication")

        if (context != null) {
            cachedContext = context
        }
        return context
    }

    /**
     * Resets the cached system context.
     *
     * 重置缓存的系统上下文引用。
     */
    fun reset() {
        cachedContext = null
    }

    private fun getActivityThreadSystemContext(): Context? {
        return try {
            val activityThreadClass = Class.forName("android.app.ActivityThread")
            val currentMethod = activityThreadClass.getDeclaredMethod("currentActivityThread")
            currentMethod.isAccessible = true
            val activityThread = currentMethod.invoke(null) ?: return null
            val getSystemContextMethod = activityThreadClass.getDeclaredMethod("getSystemContext")
            getSystemContextMethod.isAccessible = true
            getSystemContextMethod.invoke(activityThread) as? Context
        } catch (_: Throwable) {
            null
        }
    }

    private fun invokeStaticContext(className: String, methodName: String): Context? {
        return try {
            val clazz = Class.forName(className)
            val method = clazz.getDeclaredMethod(methodName)
            method.isAccessible = true
            method.invoke(null) as? Context
        } catch (_: Throwable) {
            null
        }
    }
}
