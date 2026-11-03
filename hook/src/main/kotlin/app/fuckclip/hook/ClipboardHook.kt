package app.fuckclip.hook

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Looper
import android.util.Log
import app.fuckclip.core.ModuleConstants
import app.fuckclip.core.PrefKeys
import app.fuckclip.core.limiter.ReadThrottleResult
import app.fuckclip.core.limiter.RogueAppLimiter
import app.fuckclip.core.model.WhitelistRule
import app.fuckclip.hook.model.PendingRequest
import app.fuckclip.hook.model.ReadSync
import app.fuckclip.hook.model.Replay
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import org.json.JSONArray
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.TimeUnit

/**
 * Range of application UIDs allocated per Android secondary user profile.
 *
 * Android 多用户环境中每个用户分配的 UID 区间跨度。
 */
private const val PER_USER_RANGE = 100_000

/**
 * Timeout in seconds for synchronous read prompts before defaulting to deny.
 *
 * 同步读取弹窗授权的超时时长（秒），超时后默认拒绝。
 */
private const val SYNC_READ_TIMEOUT_SECONDS = 12L

/**
 * Maximum number of clipboard characters captured for audit logs and prompt previews.
 *
 * 记录日志与弹窗预览时截取的剪贴板最大字符数。
 */
private const val MAX_CAPTURED_TEXT_CHARS = 8_192

/**
 * Marker appended when clipboard text is cut off at [MAX_CAPTURED_TEXT_CHARS].
 *
 * 剪贴板文本超出 [MAX_CAPTURED_TEXT_CHARS] 截断时追加的省略标记。
 */
private const val TRUNCATION_MARKER = "…"
 

/**
 * Interceptor responsible for managing clipboard access permissions (Ask, Allow, Deny) across applications.
 *
 * 负责管理应用剪贴板权限（询问、允许、拒绝三种模式）并提供悬浮拦截弹窗的核心钩子协调器。
 */
internal class ClipboardHook(
    /**
     * Module instance providing hooking APIs and remote preferences.
     *
     * 提供 Hook API 与远程配置的模块实例。
     */
    internal val module: XposedModule
) {
    private val androidHook = AndroidClipboardHook(this)
    private val samsungHook = SamsungClipboardHook(this)

    private val rogueLimiter = RogueAppLimiter()
    private val hookedMethods = HashSet<Method>()
    private val scannedClasses = HashSet<Class<*>>()

    /**
     * In-memory cache of rules updated via dialog checkbox during this process lifecycle.
     *
     * 进程生命周期内通过弹窗复选框更新的临时规则内存缓存。
     */
    private val rememberedRules = HashMap<String, Pair<Int, Int>>()

    private val promptManager = ClipboardPromptManager(
        hook = this,
        module = module,
        rogueLimiter = rogueLimiter,
        rememberedRules = rememberedRules
    )

    /**
     * Installs clipboard interception hooks for system services using dual-path discovery.
     *
     * 通过双路径发现机制为系统服务安装剪贴板拦截钩子。
     */
    fun install() {
        androidHook.install()
        samsungHook.install()
    }

    /**
     * Resets internal caches, queue, and hooked method states during hot reloading.
     *
     * 在模块热重载时重置内部缓存、等待队列与已挂钩方法状态。
     */
    fun reset() {
        promptManager.reset()
        hookedMethods.clear()
        scannedClasses.clear()
        rememberedRules.clear()
        SystemContextResolver.reset()
        ClipboardReader.reset()
        rogueLimiter.reset()
    }

    /**
     * Inspects the clipboard service implementation [clazz] and attaches interceptors to getPrimaryClip and setPrimaryClip.
     *
     * 扫描剪贴板服务实现类 [clazz]，为 getPrimaryClip 与 setPrimaryClip 方法挂载拦截器。
     */
    internal fun hookClipboardService(clazz: Class<*>) {
        if (!scannedClasses.add(clazz)) {
            return
        }

        var currentClass: Class<*>? = clazz
        while (currentClass != null && currentClass != Any::class.java) {
            for (method in currentClass.declaredMethods) {
                if ((method.modifiers and Modifier.PUBLIC) == 0) {
                    continue
                }
                val params = method.parameterTypes

                if (method.name == "getPrimaryClip" &&
                    method.returnType == ClipData::class.java &&
                    params.isNotEmpty() &&
                    params[0] == String::class.java
                ) {
                    hookMethod(method, isWrite = false)
                } else if (method.name == "setPrimaryClip" &&
                    method.returnType == java.lang.Void.TYPE &&
                    params.size >= 2 &&
                    params[0] == ClipData::class.java &&
                    params[1] == String::class.java
                ) {
                    hookMethod(method, isWrite = true)
                }
            }
            currentClass = currentClass.superclass
        }
    }

    private fun hookMethod(method: Method, isWrite: Boolean) {
        if (!hookedMethods.add(method)) {
            return
        }

        try {
            method.isAccessible = true
            val invoker = module.getInvoker(method).setType(XposedInterface.Invoker.Type.ORIGIN)

            module.hook(method)
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (isWrite) {
                        handleSetPrimaryClip(chain, method, invoker)
                    } else {
                        handleGetPrimaryClip(chain)
                    }
                }
            module.log(Log.INFO, TAG, "Hooked clipboard method: $method (write=$isWrite)")
        } catch (t: Throwable) {
            module.log(Log.ERROR, TAG, "Failed to hook method $method", t)
        }
    }

    private fun handleGetPrimaryClip(chain: XposedInterface.Chain): Any? {
        val packageName = (chain.getArg(0) as? String)
            ?: getSystemContext()?.packageManager?.getNameForUid(Binder.getCallingUid())
            ?: return chain.proceed()

        if (isFrameworkCaller(packageName)) {
            return chain.proceed()
        }

        val rule = resolvePackageRule(packageName, isWrite = false)
        val userId = Binder.getCallingUid() / PER_USER_RANGE

        val prefs = try { module.getRemotePreferences(PrefKeys.PREFS_GROUP) } catch (_: Throwable) { null }
        val rogueThrottleEnabled = prefs?.getBoolean(PrefKeys.KEY_ROGUE_THROTTLE_ENABLED, true) ?: true
        val maxReadFrequency = prefs?.getInt(PrefKeys.KEY_MAX_READ_FREQUENCY, RogueAppLimiter.DEFAULT_MAX_ATTEMPTS)
            ?: RogueAppLimiter.DEFAULT_MAX_ATTEMPTS
        val readTolerance = prefs?.getInt(PrefKeys.KEY_READ_TOLERANCE_COUNT, RogueAppLimiter.DEFAULT_PROMPT_TOLERANCE)
            ?: RogueAppLimiter.DEFAULT_PROMPT_TOLERANCE

        val throttleResult = rogueLimiter.evaluateRead(
            packageName = packageName,
            isRuleRequest = (rule == WhitelistRule.RULE_REQUEST),
            enabled = rogueThrottleEnabled,
            maxAttempts = maxReadFrequency,
            promptTolerance = readTolerance
        )

        when (throttleResult) {
            is ReadThrottleResult.Throttled -> {
                val peekedClip = ClipboardReader.readPrimaryClip(chain.thisObject, userId)
                    ?: ClipboardReader.readPrimaryClip(chain.thisObject, 0)
                val text = extractClipText(peekedClip, getSystemContext())
                notifyHookLog(packageName, "READ", allowed = false, detail = throttleResult.reason, content = text)
                return null
            }
            is ReadThrottleResult.FastTrackAllow -> {
                val original = try {
                    chain.proceed() as? ClipData
                } catch (_: SecurityException) {
                    null
                }
                val clip = original ?: ClipboardReader.readPrimaryClip(chain.thisObject, userId)
                val text = extractClipText(clip, getSystemContext())
                notifyHookLog(packageName, "READ", allowed = true, detail = "Allowed by burst window", content = text)
                return clip
            }
            ReadThrottleResult.Proceed -> {
                // Proceed with normal evaluation
            }
        }

        when (rule) {
            WhitelistRule.RULE_ALLOW -> {
                val original = try {
                    chain.proceed() as? ClipData
                } catch (_: SecurityException) {
                    null
                }
                val clip = original ?: ClipboardReader.readPrimaryClip(chain.thisObject, userId)
                val text = extractClipText(clip, getSystemContext())
                notifyHookLog(packageName, "READ", allowed = true, detail = "Allowed by rule", content = text)
                return clip
            }
            WhitelistRule.RULE_DENY -> {
                val peekedClip = ClipboardReader.readPrimaryClip(chain.thisObject, userId)
                    ?: ClipboardReader.readPrimaryClip(chain.thisObject, 0)
                val text = extractClipText(peekedClip, getSystemContext())
                notifyHookLog(packageName, "READ", allowed = false, detail = "Denied by configured rule", content = text)
                return null
            }
            else -> {
                // RULE_REQUEST: synchronously prompt the user
                var peekedClip = ClipboardReader.readPrimaryClip(chain.thisObject, userId)
                if (peekedClip == null && userId != 0) {
                    peekedClip = ClipboardReader.readPrimaryClip(chain.thisObject, 0)
                }
                val sync = ReadSync()
                val pending = PendingRequest(
                    packageName = packageName,
                    isWrite = false,
                    clipData = peekedClip,
                    replay = null,
                    syncList = mutableListOf(sync),
                    thisObject = chain.thisObject,
                    userId = userId
                )

                rogueLimiter.onPromptStarted(packageName)
                promptManager.enqueueRequest(pending)

                val answered = try {
                    sync.latch.await(SYNC_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                } catch (_: InterruptedException) {
                    false
                }

                return if (answered && sync.granted.get()) {
                    val original = try {
                        chain.proceed() as? ClipData
                    } catch (_: SecurityException) {
                        null
                    }
                    original ?: peekedClip ?: ClipboardReader.readPrimaryClip(chain.thisObject, userId)
                } else {
                    null
                }
            }
        }
    }

    private fun handleSetPrimaryClip(
        chain: XposedInterface.Chain,
        method: Method,
        invoker: XposedInterface.Invoker<*, Method>
    ): Any? {
        val packageName = (chain.getArg(1) as? String)
            ?: getSystemContext()?.packageManager?.getNameForUid(Binder.getCallingUid())
        val clipData = chain.getArg(0) as? ClipData

        if (packageName == null || isFrameworkCaller(packageName)) {
            return chain.proceed()
        }

        val rule = resolvePackageRule(packageName, isWrite = true)

        when (rule) {
            WhitelistRule.RULE_ALLOW -> {
                val result = chain.proceed()
                val text = extractClipText(clipData, getSystemContext())
                notifyHookLog(packageName, "WRITE", allowed = true, detail = "Allowed by rule", content = text)
                return result
            }
            WhitelistRule.RULE_DENY -> {
                val text = extractClipText(clipData, getSystemContext())
                notifyHookLog(packageName, "WRITE", allowed = false, detail = "Denied by rule", content = text)
                return null
            }
            else -> {
                // RULE_REQUEST: suspend call and prompt asynchronously
                val callerUserId = Binder.getCallingUid() / PER_USER_RANGE
                val callingToken = Binder.clearCallingIdentity()
                try {
                    val replay = Replay(method, invoker, chain.thisObject, chain.args.toTypedArray<Any?>(), callingToken)
                    val pending = PendingRequest(
                        packageName = packageName,
                        isWrite = true,
                        clipData = clipData,
                        replay = replay,
                        thisObject = chain.thisObject,
                        userId = callerUserId
                    )
                    promptManager.enqueueRequest(pending)
                    return null
                } finally {
                    Binder.restoreCallingIdentity(callingToken)
                }
            }
        }
    }

    private fun isFrameworkCaller(packageName: String): Boolean {
        val callingPid = Binder.getCallingPid()
        val callingUid = Binder.getCallingUid()
        return packageName == "android" ||
            callingUid == android.os.Process.SYSTEM_UID ||
            callingPid == android.os.Process.myPid() ||
            Looper.myLooper() == Looper.getMainLooper()
    }

    private fun resolvePackageRule(packageName: String, isWrite: Boolean): Int {
        try {
            val prefs = module.getRemotePreferences(PrefKeys.PREFS_GROUP)
            val masterEnabled = prefs.getBoolean(PrefKeys.KEY_MASTER_ENABLED, true)
            if (!masterEnabled) {
                return WhitelistRule.RULE_ALLOW
            }

            synchronized(rememberedRules) {
                rememberedRules[packageName]?.let {
                    return if (isWrite) it.second else it.first
                }
            }

            val rawWhitelist = prefs.getString(PrefKeys.KEY_WHITELIST, null)
            if (!rawWhitelist.isNullOrBlank()) {
                val array = JSONArray(rawWhitelist)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    if (obj.optString("packageName") == packageName) {
                        val readRule = obj.optInt("readRule", WhitelistRule.RULE_REQUEST)
                        val writeRule = obj.optInt("writeRule", WhitelistRule.RULE_REQUEST)
                        return if (isWrite) writeRule else readRule
                    }
                }
            }
        } catch (t: Throwable) {
            module.log(Log.WARN, TAG, "Failed resolving rule for $packageName", t)
        }
        return WhitelistRule.RULE_REQUEST
    }

    /**
     * Broadcasts updated permission rules for [packageName] to the manager app.
     *
     * 向管理器应用广播发送指定 [packageName] 更新后的权限规则。
     */
    internal fun broadcastRuleUpdate(packageName: String, readRule: Int, writeRule: Int) {
        try {
            val context = getSystemContext() ?: return
            val intent = Intent(ModuleConstants.ACTION_UPDATE_RULE).apply {
                setPackage(ModuleConstants.APPLICATION_ID)
                putExtra("pkg", packageName)
                putExtra("readRule", readRule)
                putExtra("writeRule", writeRule)
            }
            val token = Binder.clearCallingIdentity()
            try {
                context.sendBroadcast(intent)
            } finally {
                Binder.restoreCallingIdentity(token)
            }
        } catch (t: Throwable) {
            module.log(Log.WARN, TAG, "Failed sending rule update broadcast", t)
        }
    }

    /**
     * Extracts plain text representation from [clipData] using [context], capping the length at [MAX_CAPTURED_TEXT_CHARS].
     *
     * 使用 [context] 从 [clipData] 中解析提取纯文本内容，长度限制在 [MAX_CAPTURED_TEXT_CHARS] 以内。
     */
    internal fun extractClipText(clipData: ClipData?, context: Context?): String? {
        if (clipData == null || clipData.itemCount <= 0) return null
        val sb = StringBuilder()
        for (i in 0 until clipData.itemCount) {
            val item = clipData.getItemAt(i) ?: continue
            val text = item.text?.toString()
                ?: if (context != null) {
                    try {
                        item.coerceToText(context)?.toString()
                    } catch (_: Throwable) {
                        null
                    }
                } else null
                ?: item.htmlText
                ?: item.uri?.toString()
                ?: item.intent?.toUri(0)
            if (!text.isNullOrEmpty()) {
                if (sb.isNotEmpty()) sb.append("\n")
                val remaining = MAX_CAPTURED_TEXT_CHARS - sb.length
                if (remaining <= 0) break
                if (text.length > remaining) {
                    sb.append(text, 0, remaining)
                    sb.append(TRUNCATION_MARKER)
                    break
                }
                sb.append(text)
            }
        }
        return sb.toString().ifEmpty { null }
    }

    /**
     * Emits a broadcast notifying the manager app of a clipboard hook event.
     *
     * 发送广播通知管理器应用剪贴板拦截或放行事件。
     */
    internal fun notifyHookLog(
        packageName: String,
        action: String,
        allowed: Boolean,
        detail: String,
        content: String? = null
    ) {
        val (shouldEmit, suppressedCount) = rogueLimiter.checkLogDebounce(packageName)
        if (!shouldEmit) {
            return
        }
        val finalDetail = if (suppressedCount > 0) {
            "$detail (+$suppressedCount suppressed)"
        } else {
            detail
        }
        try {
            val context = getSystemContext() ?: return
            val intent = Intent(ModuleConstants.ACTION_HOOK_LOG).apply {
                setPackage(ModuleConstants.APPLICATION_ID)
                putExtra("pkg", packageName)
                putExtra("action", action)
                putExtra("allowed", allowed)
                putExtra("source", finalDetail)
                putExtra("timestamp", System.currentTimeMillis())
                putExtra("content", content)
                putExtra("count", suppressedCount + 1)
            }
            val token = Binder.clearCallingIdentity()
            try {
                context.sendBroadcast(intent)
            } finally {
                Binder.restoreCallingIdentity(token)
            }
        } catch (t: Throwable) {
            module.log(Log.WARN, TAG, "Failed to send hook log broadcast", t)
        }
    }

    /**
     * Resolves and caches the system server [Context].
     *
     * 解析并缓存 system_server 进程的系统级 [Context]。
     */
    internal fun getSystemContext(): Context? = SystemContextResolver.getSystemContext()

    private companion object {
        const val TAG = "ClipboardHook"
    }
}
