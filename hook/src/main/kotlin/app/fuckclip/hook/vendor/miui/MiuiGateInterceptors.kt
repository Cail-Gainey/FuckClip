package app.fuckclip.hook.vendor.miui

import app.fuckclip.hook.ClipboardHookContext
import app.fuckclip.hook.vendor.VendorGateSupport
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Installs interception gates on Xiaomi MIUI and HyperOS clipboard stub and framework service methods.
 *
 * 在小米 MIUI 与澎湃 HyperOS 剪贴板 Stub 及系统服务方法上安装厂商权限拦截闸门。
 */
internal class MiuiGateInterceptors(
    /**
     * Runtime hook context providing Xposed hooking and policy evaluation services.
     *
     * 提供 Xposed 挂钩与权限规则评估能力的运行时上下文。
     */
    context: ClipboardHookContext
) {
    private val gateSupport = VendorGateSupport(context, TAG)

    /**
     * Attaches MIUI and HyperOS permission, user-choice, clip-data, and provider wake-path gates across [stubClass].
     *
     * 遍历 [stubClass] 类层级并挂载 MIUI 与 HyperOS 的权限决策、弹窗等待、数据修改与唤醒路径闸门。
     */
    fun installStubGates(stubClass: Class<*>) {
        var current: Class<*>? = stubClass
        while (current != null && current != Any::class.java) {
            for (method in current.declaredMethods) {
                if (isSkippable(method)) {
                    continue
                }
                when (method.name) {
                    METHOD_CLIPBOARD_ACCESS_RESULT ->
                        gateSupport.hookIntAccessGate(
                            method = method,
                            allowValue = MIUI_ACCESS_ALLOW,
                            denyValue = MIUI_ACCESS_DENY,
                            gateLabel = "MIUI access result gate",
                            denyDetail = "Denied by rule (MIUI access check)"
                        )

                    METHOD_WAIT_USER_CHOICE ->
                        gateSupport.hookPromptGate(
                            method = method,
                            gateLabel = "MIUI waitUserChoice gate",
                            allowIntValue = MIUI_ACCESS_ALLOW,
                            denyIntValue = MIUI_ACCESS_DENY
                        )

                    METHOD_MODIFY_CLIP_DATA ->
                        gateSupport.hookClipDataGate(
                            method = method,
                            gateLabel = "MIUI modifyClipDataIfNeed gate"
                        )

                    METHOD_CHECK_PROVIDER_WAKE_PATH ->
                        gateSupport.hookBooleanAccessGate(
                            method = method,
                            gateLabel = "MIUI provider wake path gate",
                            denyDetail = "Denied by rule (MIUI provider wake path)",
                            respectInterceptingScope = false
                        )
                }
            }
            current = current.superclass
        }
    }

    /**
     * Attaches framework-level `clipboardAccessAllowed` permission gates across [serviceClass].
     *
     * 遍历 [serviceClass] 类层级并挂载系统服务层的 `clipboardAccessAllowed` 权限校验闸门。
     */
    fun installServiceGates(serviceClass: Class<*>) {
        var current: Class<*>? = serviceClass
        while (current != null && current != Any::class.java) {
            for (method in current.declaredMethods) {
                if (method.name == METHOD_CLIPBOARD_ACCESS_ALLOWED && !Modifier.isAbstract(method.modifiers)) {
                    gateSupport.hookBooleanAccessGate(
                        method = method,
                        gateLabel = "ClipboardService.clipboardAccessAllowed gate",
                        fallbackToUid = false
                    )
                }
            }
            current = current.superclass
        }
    }

    /**
     * Clears tracked gate method state on module hot reload.
     *
     * 在模块热重载时清空已挂载的闸门方法状态。
     */
    fun reset() {
        gateSupport.reset()
    }

    private fun isSkippable(method: Method): Boolean =
        Modifier.isAbstract(method.modifiers) || method.isSynthetic || method.isBridge

    internal companion object {
        private const val TAG = "MiuiGateInterceptors"

        /**
         * Method name of the MIUI clipboard access decision gate.
         *
         * MIUI 剪贴板访问决策校验方法名。
         */
        const val METHOD_CLIPBOARD_ACCESS_RESULT = "clipboardAccessResult"

        /**
         * Method name of the MIUI synchronous user choice prompt gate.
         *
         * MIUI 同步等待用户弹窗选择的方法名。
         */
        const val METHOD_WAIT_USER_CHOICE = "waitUserChoice"

        /**
         * Method name of the MIUI clipboard data filtering gate.
         *
         * MIUI 按需修改或过滤剪贴板数据的方法名。
         */
        const val METHOD_MODIFY_CLIP_DATA = "modifyClipDataIfNeed"

        /**
         * Method name of the MIUI clipboard provider wake-path check gate.
         *
         * MIUI 剪贴板 ContentProvider 唤醒路径校验方法名。
         */
        const val METHOD_CHECK_PROVIDER_WAKE_PATH = "checkProviderWakePathForClipboard"

        /**
         * Method name of the clipboard access permission check in `ClipboardService`.
         *
         * `ClipboardService` 中剪贴板访问权限检查的方法名。
         */
        const val METHOD_CLIPBOARD_ACCESS_ALLOWED = "clipboardAccessAllowed"

        /**
         * Decision code returned by MIUI clipboard access checks when access is allowed.
         *
         * MIUI 剪贴板访问校验表示允许放行的返回码。
         */
        const val MIUI_ACCESS_ALLOW = 0

        /**
         * Decision code returned by MIUI clipboard access checks when access is denied.
         *
         * MIUI 剪贴板访问校验表示拒绝拦截的返回码。
         */
        const val MIUI_ACCESS_DENY = 1
    }
}
