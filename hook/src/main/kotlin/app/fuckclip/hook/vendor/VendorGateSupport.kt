package app.fuckclip.hook.vendor

import android.content.ClipData
import android.util.Log
import app.fuckclip.core.model.WhitelistRule
import app.fuckclip.hook.ClipboardAccessScope
import app.fuckclip.hook.ClipboardHookContext
import app.fuckclip.hook.ClipboardMethodClassifier
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method

/**
 * Shared installer for vendor clipboard permission, data-filtering, and prompt-suppression gates.
 *
 * 用于挂载厂商剪贴板权限校验、数据过滤及提示抑制闸门的共享安装辅助器。
 */
internal class VendorGateSupport(
    /**
     * Runtime hook context providing Xposed hooking APIs, rule lookup, and audit logging.
     *
     * 提供 Xposed 挂钩接口、规则解析与审计日志能力的运行时上下文。
     */
    private val context: ClipboardHookContext,

    /**
     * Log tag used when reporting vendor gate installation results.
     *
     * 上报厂商闸门安装结果时使用的日志标签。
     */
    private val tag: String
) {
    private val hookedMethods = HashSet<Method>()

    /**
     * Clears tracked gate methods on module hot reload.
     *
     * 在模块热重载时清空已挂载的闸门方法记录。
     */
    @Synchronized
    fun reset() {
        hookedMethods.clear()
    }

    /**
     * Attaches a boolean access-check interceptor on [method] returning `true` when granted and `false` on rule denial.
     *
     * 为返回布尔值的权限检查方法 [method] 安装访问校验拦截器。
     */
    fun hookBooleanAccessGate(
        method: Method,
        gateLabel: String,
        denyDetail: String? = null,
        fallbackToUid: Boolean = true,
        respectInterceptingScope: Boolean = true
    ) {
        hookAccessDecisionGate(
            method = method,
            expectedReturnType = java.lang.Boolean.TYPE,
            allowValue = true,
            denyValue = false,
            gateLabel = gateLabel,
            denyDetail = denyDetail,
            fallbackToUid = fallbackToUid,
            respectInterceptingScope = respectInterceptingScope
        )
    }

    /**
     * Attaches an integer access-check interceptor on [method] returning [allowValue] when granted and [denyValue] when denied.
     *
     * 为返回整型决策码的方法 [method] 安装访问校验拦截器，授权时返回 [allowValue]，拒绝时返回 [denyValue]。
     */
    fun hookIntAccessGate(
        method: Method,
        allowValue: Int,
        denyValue: Int,
        gateLabel: String,
        denyDetail: String? = null,
        fallbackToUid: Boolean = true,
        respectInterceptingScope: Boolean = true
    ) {
        val intType = Int::class.javaPrimitiveType ?: return
        hookAccessDecisionGate(
            method = method,
            expectedReturnType = intType,
            allowValue = allowValue,
            denyValue = denyValue,
            gateLabel = gateLabel,
            denyDetail = denyDetail,
            fallbackToUid = fallbackToUid,
            respectInterceptingScope = respectInterceptingScope
        )
    }

    /**
     * Attaches a [ClipData] filter gate on [method] suppressing clipboard payload delivery for denied callers.
     *
     * 为返回 [ClipData] 的数据过滤方法 [method] 安装拦截器，对被拒调用方屏蔽数据返回。
     */
    fun hookClipDataGate(method: Method, gateLabel: String, denyDetail: String? = null) {
        if (method.returnType != ClipData::class.java || !tryRegister(method)) {
            return
        }
        try {
            method.isAccessible = true
            context.module.hook(method)
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    val inputClip = chain.args.firstOrNull { it is ClipData } as? ClipData
                    if (ClipboardAccessScope.isUserGrantActive()) {
                        return@intercept inputClip ?: chain.proceed()
                    }
                    if (!ClipboardAccessScope.isIntercepting()) {
                        val pkg = resolveDeniedCaller(chain.args, fallbackToUid = true)
                        if (pkg != null) {
                            logDeny(pkg, denyDetail)
                            return@intercept null
                        }
                    }
                    chain.proceed()
                }
            context.module.log(Log.INFO, tag, "Hooked $gateLabel: $method")
        } catch (t: Throwable) {
            context.module.log(Log.WARN, tag, "Failed hooking $gateLabel $method", t)
        }
    }

    /**
     * Suppresses vendor privacy prompts or access notifications on [method] when the caller rule is Deny.
     *
     * 当调用方规则为拒绝时，在 [method] 上抑制厂商原生隐私弹窗或访问提示；整型返回时按 [allowIntValue]/[denyIntValue] 响应。
     */
    fun hookPromptGate(
        method: Method,
        gateLabel: String,
        allowIntValue: Int = 0,
        denyIntValue: Int = -1,
        shortCircuitOnUserGrant: Boolean = true
    ) {
        if (!tryRegister(method)) {
            return
        }
        try {
            method.isAccessible = true
            context.module.hook(method)
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (shortCircuitOnUserGrant && ClipboardAccessScope.isUserGrantActive()) {
                        return@intercept defaultPromptResult(method.returnType, true, allowIntValue, denyIntValue)
                    }
                    if (resolveDeniedCaller(chain.args, fallbackToUid = true) != null) {
                        return@intercept defaultPromptResult(method.returnType, false, allowIntValue, denyIntValue)
                    }
                    chain.proceed()
                }
            context.module.log(Log.INFO, tag, "Hooked $gateLabel: $method")
        } catch (t: Throwable) {
            context.module.log(Log.WARN, tag, "Failed hooking $gateLabel $method", t)
        }
    }

    private fun hookAccessDecisionGate(
        method: Method,
        expectedReturnType: Class<*>,
        allowValue: Any,
        denyValue: Any,
        gateLabel: String,
        denyDetail: String?,
        fallbackToUid: Boolean,
        respectInterceptingScope: Boolean
    ) {
        if (method.returnType != expectedReturnType || !tryRegister(method)) {
            return
        }
        try {
            method.isAccessible = true
            context.module.hook(method)
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (ClipboardAccessScope.isUserGrantActive()) {
                        return@intercept allowValue
                    }
                    if (!respectInterceptingScope || !ClipboardAccessScope.isIntercepting()) {
                        val pkg = resolveDeniedCaller(chain.args, fallbackToUid = fallbackToUid)
                        if (pkg != null) {
                            logDeny(pkg, denyDetail)
                            return@intercept denyValue
                        }
                    }
                    chain.proceed()
                }
            context.module.log(Log.INFO, tag, "Hooked $gateLabel: $method")
        } catch (t: Throwable) {
            context.module.log(Log.WARN, tag, "Failed hooking $gateLabel $method", t)
        }
    }

    private fun resolveDeniedCaller(args: List<Any?>, fallbackToUid: Boolean): String? {
        val pkg = ClipboardMethodClassifier.extractPackageArg(args)
            ?: if (fallbackToUid) context.resolveCallingPackage() else null
        if (pkg == null || context.isFrameworkCaller(pkg)) {
            return null
        }
        return if (context.resolvePackageRule(pkg, isWrite = false) == WhitelistRule.RULE_DENY) {
            pkg
        } else {
            null
        }
    }

    private fun logDeny(packageName: String, denyDetail: String?) {
        if (denyDetail != null) {
            context.notifyHookLog(
                packageName = packageName,
                action = "READ",
                allowed = false,
                detail = denyDetail
            )
        }
    }

    private fun defaultPromptResult(
        returnType: Class<*>,
        granted: Boolean,
        allowIntValue: Int,
        denyIntValue: Int
    ): Any? {
        return when (returnType) {
            java.lang.Boolean.TYPE -> granted
            Int::class.javaPrimitiveType -> if (granted) allowIntValue else denyIntValue
            else -> null
        }
    }

    @Synchronized
    private fun tryRegister(method: Method): Boolean = hookedMethods.add(method)
}
