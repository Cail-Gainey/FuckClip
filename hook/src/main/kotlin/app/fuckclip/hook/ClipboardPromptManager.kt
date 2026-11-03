package app.fuckclip.hook

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import app.fuckclip.core.PrefKeys
import app.fuckclip.core.limiter.RogueAppLimiter
import app.fuckclip.core.model.WhitelistRule
import app.fuckclip.hook.model.PendingRequest
import io.github.libxposed.api.XposedModule
import org.json.JSONArray

private const val SYNC_READ_TIMEOUT_SECONDS = 12L

/**
 * Manages clipboard authorization prompt queueing, dialog display, timeout scheduling, and decision dispatch.
 *
 * 管理剪贴板授权弹窗排队、界面展示、超时控制与用户授权决策派发。
 */
internal class ClipboardPromptManager(
    private val hook: ClipboardHook,
    private val module: XposedModule,
    private val rogueLimiter: RogueAppLimiter,
    private val rememberedRules: HashMap<String, Pair<Int, Int>>
) {
    private val handler = Handler(Looper.getMainLooper())
    private val pendingQueue = ArrayList<PendingRequest>()
    private var activeDialog: ClipboardRequestDialog? = null
    private var dialogTimeoutRunnable: Runnable? = null

    /**
     * Enqueues a clipboard access request for prompting on the main thread.
     *
     * 将剪贴板访问请求排入授权弹窗队列，并调度至主线程展示。
     *
     * The interceptor runs on a Binder thread that has no Looper of its own, so dialogs can never be
     * created there; every dialog and queue operation is therefore marshalled onto the main thread.
     *
     * 拦截器运行在不具备 Looper 的 Binder 线程上，无法在该线程创建对话框；
     * 因此所有弹窗与队列操作统一转交主线程执行。
     *
     * @param pending Clipboard access request awaiting a user decision.
     */
    fun enqueueRequest(pending: PendingRequest) {
        handler.post { enqueueOnMainThread(pending) }
    }

    /**
     * Registers a pending request and starts prompting when no dialog is currently active.
     *
     * 注册待处理请求，并在当前没有活动弹窗时立即拉起询问流程。
     */
    private fun enqueueOnMainThread(pending: PendingRequest) {
        if (activeDialog != null) {
            val existingIndex = pendingQueue.indexOfFirst {
                it.packageName == pending.packageName && it.isWrite == pending.isWrite
            }
            if (existingIndex >= 0) {
                val existing = pendingQueue[existingIndex]
                synchronized(existing.syncList) {
                    existing.syncList.addAll(pending.syncList)
                }
                return
            }
            pendingQueue.add(pending)
            return
        }

        pendingQueue.add(pending)
        displayHeadRequest()
    }

    /**
     * Dismisses active dialogs and cancels pending requests during reset.
     *
     * 在模块重置时取消弹窗超时、解散当前对话框并拒绝队列中所有等待请求。
     */
    fun reset() {
        handler.post {
            cancelDialogTimeout()
            for (pending in pendingQueue) {
                rogueLimiter.onPromptFinished(pending.packageName)
                synchronized(pending.syncList) {
                    for (sync in pending.syncList) {
                        sync.granted.set(false)
                        sync.latch.countDown()
                    }
                }
            }
            pendingQueue.clear()
            activeDialog?.dismiss()
            activeDialog = null
        }
    }

    /**
     * Creates and shows the authorization dialog for the head request of the pending queue.
     *
     * 为等待队列头部的请求创建并显示授权弹窗。
     *
     * Must run on the main thread. If the dialog cannot be created or shown, the request is denied
     * once and the queue keeps moving instead of stalling forever.
     *
     * 必须在主线程执行。若弹窗无法创建或显示，则该请求被单次拒绝，队列继续推进而不会永久阻塞。
     */
    private fun displayHeadRequest() {
        if (activeDialog != null) return
        val pending = pendingQueue.firstOrNull() ?: return

        val context = hook.getSystemContext()
        if (context == null) {
            module.log(Log.ERROR, TAG, "System context unavailable, denying prompt for ${pending.packageName}")
            failPendingRequest(pending, "Denied by prompt failure (no system context)")
            return
        }

        val prefs = try { module.getRemotePreferences(PrefKeys.PREFS_GROUP) } catch (_: Throwable) { null }
        val preferredLanguage = prefs?.getString(PrefKeys.KEY_SELECTED_LANGUAGE, "").orEmpty()
        val colorMode = prefs?.getInt(PrefKeys.KEY_THEME_COLOR_MODE, 0) ?: 0
        val keyColor = prefs?.getInt(PrefKeys.KEY_THEME_KEY_COLOR, 0) ?: 0

        val dialog = try {
            ClipboardRequestDialog(
                context = context,
                preferredLanguage = preferredLanguage,
                colorMode = colorMode,
                keyColor = keyColor
            )
        } catch (t: Throwable) {
            module.log(Log.ERROR, TAG, "Failed creating clipboard request dialog", t)
            failPendingRequest(pending, "Denied by prompt failure (dialog creation)")
            return
        }
        activeDialog = dialog

        val pm = context.packageManager
        val appInfo = try {
            pm.getApplicationInfo(pending.packageName, 0)
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
        val appLabel = appInfo?.loadLabel(pm) ?: pending.packageName
        val appIcon: Drawable? = appInfo?.loadIcon(pm)

        val resolvedClip = pending.clipData
            ?: ClipboardReader.readPrimaryClip(pending.thisObject, pending.userId)
            ?: ClipboardReader.readPrimaryClip(pending.thisObject, 0)

        val clipText = hook.extractClipText(resolvedClip, context)

        dialog.bindRequest(
            appLabel = appLabel,
            packageName = pending.packageName,
            appIcon = appIcon,
            isWrite = pending.isWrite,
            clipContent = clipText
        ) { granted, remember ->
            handleDecision(pending, granted, remember)
        }

        try {
            dialog.show()
        } catch (t: Throwable) {
            module.log(Log.ERROR, TAG, "Failed showing clipboard request dialog", t)
            activeDialog = null
            try { dialog.dismiss() } catch (_: Throwable) {}
            failPendingRequest(pending, "Denied by prompt failure (dialog display)")
            return
        }
        scheduleDialogTimeout()
    }

    private fun handleDecision(pending: PendingRequest, granted: Boolean, remember: Boolean) {
        cancelDialogTimeout()
        rogueLimiter.onPromptFinished(pending.packageName)

        val decidedRule = if (granted) WhitelistRule.RULE_ALLOW else WhitelistRule.RULE_DENY
        val action = if (pending.isWrite) "WRITE" else "READ"
        val resolvedClip = pending.clipData
            ?: ClipboardReader.readPrimaryClip(pending.thisObject, pending.userId)
            ?: ClipboardReader.readPrimaryClip(pending.thisObject, 0)
        val text = hook.extractClipText(resolvedClip, hook.getSystemContext())

        hook.notifyHookLog(
            packageName = pending.packageName,
            action = action,
            allowed = granted,
            detail = if (granted) "Allowed by user dialog" else "Denied by user dialog",
            content = text
        )

        if (!remember) {
            val prefs = try { module.getRemotePreferences(PrefKeys.PREFS_GROUP) } catch (_: Throwable) { null }
            val denyCooldownSeconds = prefs?.getInt(
                PrefKeys.KEY_DENY_COOLDOWN_SECONDS,
                (RogueAppLimiter.DEFAULT_DENY_COOLDOWN_MS / 1000).toInt()
            ) ?: (RogueAppLimiter.DEFAULT_DENY_COOLDOWN_MS / 1000).toInt()
            val grantBurstSeconds = prefs?.getInt(
                PrefKeys.KEY_GRANT_BURST_SECONDS,
                (RogueAppLimiter.DEFAULT_GRANT_BURST_MS / 1000).toInt()
            ) ?: (RogueAppLimiter.DEFAULT_GRANT_BURST_MS / 1000).toInt()

            if (granted) {
                rogueLimiter.setGrantBurstWindow(pending.packageName, grantBurstSeconds * 1000L)
            } else {
                rogueLimiter.setDenyCooldown(pending.packageName, denyCooldownSeconds * 1000L)
            }
        }

        if (remember) {
            val currentPair = synchronized(rememberedRules) {
                rememberedRules[pending.packageName] ?: getRulePairFromPrefs(pending.packageName)
            }
            val updatedPair = if (pending.isWrite) {
                currentPair.copy(second = decidedRule)
            } else {
                currentPair.copy(first = decidedRule)
            }
            synchronized(rememberedRules) {
                rememberedRules[pending.packageName] = updatedPair
            }
            hook.broadcastRuleUpdate(pending.packageName, updatedPair.first, updatedPair.second)
        }

        if (pending.isWrite && granted) {
            pending.replay?.transact()
        }

        settlePendingRequest(pending, granted)
    }

    /**
     * Denies a request whose dialog could not be raised and releases all of its waiting state.
     *
     * 当授权弹窗无法弹出时，以拒绝方式终结该请求并释放其全部等待状态。
     *
     * @param pending Request that never reached the user.
     * @param detail Audit detail describing the failure cause.
     */
    private fun failPendingRequest(pending: PendingRequest, detail: String) {
        cancelDialogTimeout()
        rogueLimiter.onPromptFinished(pending.packageName)

        val resolvedClip = pending.clipData
            ?: ClipboardReader.readPrimaryClip(pending.thisObject, pending.userId)
            ?: ClipboardReader.readPrimaryClip(pending.thisObject, 0)
        val text = hook.extractClipText(resolvedClip, hook.getSystemContext())

        hook.notifyHookLog(
            packageName = pending.packageName,
            action = if (pending.isWrite) "WRITE" else "READ",
            allowed = false,
            detail = detail,
            content = text
        )

        settlePendingRequest(pending, granted = false)
    }

    /**
     * Releases the waiting latches of [pending], drops it from the queue and advances to the next request.
     *
     * 释放 [pending] 的等待闭锁，将其移出队列并推进显示下一条待处理请求。
     *
     * @param pending Request that has just been resolved.
     * @param granted Whether clipboard access was granted.
     */
    private fun settlePendingRequest(pending: PendingRequest, granted: Boolean) {
        synchronized(pending.syncList) {
            for (sync in pending.syncList) {
                sync.granted.set(granted)
                sync.latch.countDown()
            }
        }

        activeDialog = null
        val resolvedIndex = pendingQueue.indexOfFirst { it === pending }
        if (resolvedIndex >= 0) {
            pendingQueue.removeAt(resolvedIndex)
        }

        if (pendingQueue.isNotEmpty()) {
            displayHeadRequest()
        }
    }

    private fun getRulePairFromPrefs(packageName: String): Pair<Int, Int> {
        try {
            val prefs = module.getRemotePreferences(PrefKeys.PREFS_GROUP)
            val raw = prefs.getString(PrefKeys.KEY_WHITELIST, null)
            if (!raw.isNullOrBlank()) {
                val array = JSONArray(raw)
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    if (obj.optString("packageName") == packageName) {
                        return Pair(
                            obj.optInt("readRule", WhitelistRule.RULE_REQUEST),
                            obj.optInt("writeRule", WhitelistRule.RULE_REQUEST)
                        )
                    }
                }
            }
        } catch (_: Throwable) {}
        return Pair(WhitelistRule.RULE_REQUEST, WhitelistRule.RULE_REQUEST)
    }

    private fun scheduleDialogTimeout() {
        cancelDialogTimeout()
        val r = Runnable {
            activeDialog?.let { dialog ->
                if (dialog.isShowing) {
                    dialog.dismiss()
                }
            }
        }
        dialogTimeoutRunnable = r
        handler.postDelayed(r, (SYNC_READ_TIMEOUT_SECONDS + 2) * 1000L)
    }

    private fun cancelDialogTimeout() {
        dialogTimeoutRunnable?.let { handler.removeCallbacks(it) }
        dialogTimeoutRunnable = null
    }

    private companion object {
        const val TAG = "ClipboardPromptManager"
    }
}
