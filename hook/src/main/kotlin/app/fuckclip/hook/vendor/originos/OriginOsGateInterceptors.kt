package app.fuckclip.hook.vendor.originos

import app.fuckclip.hook.ClipboardHookContext
import app.fuckclip.hook.vendor.VendorGateSupport
import java.lang.reflect.Modifier

/**
 * Installs interception gates on Vivo OriginOS and Funtouch OS clipboard permission and privacy prompt methods.
 *
 * 在 Vivo OriginOS 与 Funtouch OS 剪贴板权限校验及隐私提示方法上安装拦截闸门。
 */
internal class OriginOsGateInterceptors(
    /**
     * Runtime hook context providing Xposed hooking and policy evaluation services.
     *
     * 提供 Xposed 挂钩与权限规则评估能力的运行时上下文。
     */
    context: ClipboardHookContext
) {
    private val gateSupport = VendorGateSupport(context, TAG)

    /**
     * Traverses [vivoClass] and installs OriginOS access decision and privacy prompt gates.
     *
     * 遍历 [vivoClass] 类层级并挂载 OriginOS 剪贴板访问校验与隐私弹窗拦截闸门。
     */
    fun installGates(vivoClass: Class<*>) {
        var current: Class<*>? = vivoClass
        while (current != null && current != Any::class.java) {
            for (method in current.declaredMethods) {
                if (Modifier.isAbstract(method.modifiers) || method.isSynthetic || method.isBridge) {
                    continue
                }
                when (method.name) {
                    in VIVO_ACCESS_GATE_METHODS ->
                        gateSupport.hookBooleanAccessGate(
                            method = method,
                            gateLabel = "OriginOS access gate",
                            denyDetail = "Denied by rule (OriginOS access check)"
                        )

                    in VIVO_PRIVACY_PROMPT_METHODS ->
                        gateSupport.hookPromptGate(
                            method = method,
                            gateLabel = "OriginOS privacy prompt gate"
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

    internal companion object {
        private const val TAG = "OriginOsGateInterceptors"

        /**
         * Method names of OriginOS clipboard policy and privacy access gates.
         *
         * OriginOS 剪贴板策略与隐私访问校验闸门的方法名称集合。
         */
        val VIVO_ACCESS_GATE_METHODS = setOf(
            "checkPolicyPerm",
            "checkPrivacyAccessAllowed",
            "checkClipboardAccess",
            "isPrivacyAccessAllowed"
        )

        /**
         * Method names of OriginOS native clipboard privacy prompt triggers.
         *
         * OriginOS 原生剪贴板隐私提示弹窗触发方法名称集合。
         */
        val VIVO_PRIVACY_PROMPT_METHODS = setOf(
            "showClipboardPrivacy",
            "showClipboardDialog"
        )
    }
}
