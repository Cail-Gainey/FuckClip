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
import app.fuckclip.hook.vendor.coloros.ColorOsClipboardHook
import app.fuckclip.hook.vendor.miui.MiuiClipboardHook
import app.fuckclip.hook.vendor.originos.OriginOsClipboardHook
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedModule
import org.json.JSONArray
import java.lang.reflect.Method
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
    override val module: XposedModule
) : ClipboardHookContext {
    private val metadataHook = ClipboardMetadataHook(this)
    private val systemHooks: List<ClipboardSystemHook> = listOf(
        AndroidClipboardHook(this),
        SamsungClipboardHook(this),
        MiuiClipboardHook(this),
        OriginOsClipboardHook(this),
        ColorOsClipboardHook(this)
    )

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
     * Installs clipboard interception hooks across registered AOSP and vendor system hooks using [classLoader].
     *
     * 使用 [classLoader] 为已注册的 AOSP 及各厂商系统钩子安装剪贴板拦截器。
     */
    fun install(classLoader: ClassLoader? = null) {
        for (systemHook in systemHooks) {
            systemHook.install(classLoader)
        }
    }

    /**
     * Resets internal caches, queue, and hooked method states during hot reloading.
     *
     * 在模块热重载时重置内部缓存、等待队列与已挂钩方法状态。
     */
    @Synchronized
    fun reset() {
        promptManager.reset()
        hookedMethods.clear()
        scannedClasses.clear()
        rememberedRules.clear()
        for (systemHook in systemHooks) {
            systemHook.reset()
        }
        SystemContextResolver.reset()
        ClipboardReader.reset()
        rogueLimiter.reset()
    }

    /**
     * Inspects the clipboard service implementation [clazz] and attaches read, write, and metadata interceptors.
     *
     * 扫描剪贴板服务实现类 [clazz]，为读取、写入及元数据探测方法挂载拦截器。
     */
    @Synchronized
    override fun hookClipboardService(clazz: Class<*>, includeVendorReads: Boolean) {
        if (!scannedClasses.add(clazz)) {
            return
        }

        var currentClass: Class<*>? = clazz
        while (currentClass != null && currentClass != Any::class.java) {
            for (method in currentClass.declaredMethods) {
                when (val kind = ClipboardMethodClassifier.classify(method, includeVendorReads)) {
                    ClipboardMethodKind.READ, ClipboardMethodKind.VENDOR_READ ->
                        hookMethod(method, isWrite = false)
                    ClipboardMethodKind.WRITE ->
                        hookMethod(method, isWrite = true)
                    ClipboardMethodKind.HAS_CLIP, ClipboardMethodKind.CLIP_DESCRIPTION ->
                        if (hookedMethods.add(method)) {
                            metadataHook.hookMethod(method, kind)
                        }
                    ClipboardMethodKind.NONE -> Unit
                }
            }
            currentClass = currentClass.superclass
        }

        if (!includeVendorReads) {
            for (systemHook in systemHooks) {
                systemHook.onServiceDiscovered(clazz)
            }
        }
    }

    /**
     * Notifies registered system hooks when a live clipboard [serviceInstance] is discovered.
     *
     * 当发现运行时剪贴板服务实例 [serviceInstance] 时通知已注册的系统钩子。
     */
    override fun onServiceInstanceDiscovered(serviceInstance: Any?) {
        for (systemHook in systemHooks) {
            systemHook.onServiceInstance(serviceInstance)
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
        if (ClipboardAccessScope.isIntercepting() || ClipboardAccessScope.isUserGrantActive()) {
            return chain.proceed()
        }
        val packageName = extractCallerPackage(chain.args, isWrite = false)
            ?: return chain.proceed()

        if (isFrameworkCaller(packageName)) {
            return chain.proceed()
        }

        return ClipboardAccessScope.intercept {
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
                    return@intercept null
                }
                is ReadThrottleResult.FastTrackAllow -> {
                    val original = proceedAuthorizedRead(chain)
                    val clip = original ?: ClipboardReader.readPrimaryClip(chain.thisObject, userId)
                    val text = extractClipText(clip, getSystemContext())
                    notifyHookLog(packageName, "READ", allowed = true, detail = "Allowed by burst window", content = text)
                    return@intercept clip
                }
                ReadThrottleResult.Proceed -> Unit
            }

            when (rule) {
                WhitelistRule.RULE_ALLOW -> {
                    val original = proceedAuthorizedRead(chain)
                    val clip = original ?: ClipboardReader.readPrimaryClip(chain.thisObject, userId)
                    val text = extractClipText(clip, getSystemContext())
                    notifyHookLog(packageName, "READ", allowed = true, detail = "Allowed by rule", content = text)
                    clip
                }
                WhitelistRule.RULE_DENY -> {
                    val peekedClip = ClipboardReader.readPrimaryClip(chain.thisObject, userId)
                        ?: ClipboardReader.readPrimaryClip(chain.thisObject, 0)
                    val text = extractClipText(peekedClip, getSystemContext())
                    notifyHookLog(packageName, "READ", allowed = false, detail = "Denied by configured rule", content = text)
                    null
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

                    if (answered && sync.granted.get()) {
                        val original = proceedAuthorizedRead(chain)
                        original ?: peekedClip ?: ClipboardReader.readPrimaryClip(chain.thisObject, userId)
                    } else {
                        null
                    }
                }
            }
        }
    }

    private fun handleSetPrimaryClip(
        chain: XposedInterface.Chain,
        method: Method,
        invoker: XposedInterface.Invoker<*, Method>
    ): Any? {
        if (ClipboardAccessScope.isIntercepting() || ClipboardAccessScope.isUserGrantActive()) {
            return chain.proceed()
        }
        val packageName = extractCallerPackage(chain.args, isWrite = true)
        val clipData = chain.args.firstOrNull() as? ClipData

        if (packageName == null || isFrameworkCaller(packageName)) {
            return chain.proceed()
        }

        return ClipboardAccessScope.intercept {
            when (resolvePackageRule(packageName, isWrite = true)) {
                WhitelistRule.RULE_ALLOW -> {
                    val result = ClipboardAccessScope.withUserGrant { chain.proceed() }
                    val text = extractClipText(clipData, getSystemContext())
                    notifyHookLog(packageName, "WRITE", allowed = true, detail = "Allowed by rule", content = text)
                    result
                }
                WhitelistRule.RULE_DENY -> {
                    val text = extractClipText(clipData, getSystemContext())
                    notifyHookLog(packageName, "WRITE", allowed = false, detail = "Denied by rule", content = text)
                    null
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
                        null
                    } finally {
                        Binder.restoreCallingIdentity(callingToken)
                    }
                }
            }
        }
    }

    private fun proceedAuthorizedRead(chain: XposedInterface.Chain): ClipData? {
        return try {
            ClipboardAccessScope.withUserGrant {
                chain.proceed() as? ClipData
            }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun extractCallerPackage(args: List<Any?>, isWrite: Boolean): String? =
        ClipboardMethodClassifier.extractPackageArg(args, preferredIndex = if (isWrite) 1 else 0)
            ?: resolveCallingPackage()

    /**
     * Resolves the calling application package name from the current Binder UID.
     *
     * 根据当前 Binder 调用方 UID 解析发起请求的应用包名。
     */
    override fun resolveCallingPackage(): String? =
        getSystemContext()?.packageManager?.getNameForUid(Binder.getCallingUid())

    /**
     * Checks whether [packageName] or the current Binder caller belongs to the system framework.
     *
     * 判断 [packageName] 或当前 Binder 调用方是否属于系统框架进程。
     */
    override fun isFrameworkCaller(packageName: String): Boolean {
        val callingPid = Binder.getCallingPid()
        val callingUid = Binder.getCallingUid()
        return packageName == "android" ||
            callingUid == android.os.Process.SYSTEM_UID ||
            callingPid == android.os.Process.myPid() ||
            Looper.myLooper() == Looper.getMainLooper()
    }

    /**
     * Resolves the configured permission rule for [packageName] according to [isWrite].
     *
     * 根据是否为写入操作 [isWrite] 解析指定应用 [packageName] 的权限规则。
     */
    override fun resolvePackageRule(packageName: String, isWrite: Boolean): Int {
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
     * Extracts plain text representation from [clipData] using [context].
     *
     * 使用 [context] 从 [clipData] 中解析提取纯文本内容。
     */
    internal fun extractClipText(clipData: ClipData?, context: Context?): String? =
        ClipboardReader.extractClipText(clipData, context)

    /**
     * Emits a broadcast notifying the manager app of a clipboard hook event.
     *
     * 发送广播通知管理器应用剪贴板拦截或放行事件。
     */
    override fun notifyHookLog(
        packageName: String,
        action: String,
        allowed: Boolean,
        detail: String,
        content: String?
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
