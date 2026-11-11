package app.fuckclip.hook

import android.util.Log
import app.fuckclip.core.model.WhitelistRule
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method

/**
 * Hides clipboard existence and metadata from applications whose read rule is Deny.
 *
 * 对读取规则为“拒绝”的应用隐藏剪贴板存在性与元数据。
 */
internal class ClipboardMetadataHook(
    /**
     * Runtime hook context providing module access, rule resolution, and caller identification.
     *
     * 提供模块访问、规则解析与调用方识别能力的运行时钩子上下文。
     */
    private val context: ClipboardHookContext
) {
    /**
     * Installs the metadata guard on [method] according to its [kind].
     *
     * 根据入口类别 [kind] 为 [method] 安装元数据防护拦截。
     */
    fun hookMethod(method: Method, kind: ClipboardMethodKind) {
        try {
            method.isAccessible = true
            context.module.hook(method)
                .setPriority(XposedInterface.PRIORITY_DEFAULT)
                .setExceptionMode(XposedInterface.ExceptionMode.PROTECTIVE)
                .intercept { chain ->
                    if (shouldHide(chain.args.firstOrNull() as? String)) {
                        if (kind == ClipboardMethodKind.HAS_CLIP) false else null
                    } else {
                        chain.proceed()
                    }
                }
            context.module.log(Log.INFO, TAG, "Hooked clipboard metadata method: $method")
        } catch (t: Throwable) {
            context.module.log(Log.ERROR, TAG, "Failed to hook metadata method $method", t)
        }
    }

    private fun shouldHide(argPackage: String?): Boolean {
        if (ClipboardAccessScope.isIntercepting() || ClipboardAccessScope.isUserGrantActive()) {
            return false
        }
        val packageName = argPackage ?: context.resolveCallingPackage() ?: return false
        if (context.isFrameworkCaller(packageName)) {
            return false
        }
        return context.resolvePackageRule(packageName, isWrite = false) == WhitelistRule.RULE_DENY
    }

    private companion object {
        const val TAG = "ClipboardMetadataHook"
    }
}
