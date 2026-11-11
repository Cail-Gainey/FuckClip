package app.fuckclip.hook.vendor.coloros

import app.fuckclip.hook.ClipboardHookContext
import app.fuckclip.hook.vendor.VendorGateSupport
import java.lang.reflect.Modifier

/**
 * Installs interception gates on OPPO ColorOS, OnePlus OxygenOS, and Realme UI clipboard extension methods.
 *
 * 在 OPPO ColorOS、一加 OxygenOS 与 Realme UI 剪贴板扩展方法上安装拦截闸门。
 */
internal class ColorOsGateInterceptors(
    /**
     * Runtime hook context providing Xposed hooking and policy evaluation services.
     *
     * 提供 Xposed 挂钩与权限规则评估能力的运行时上下文。
     */
    context: ClipboardHookContext
) {
    private val gateSupport = VendorGateSupport(context, TAG)

    /**
     * Traverses [oplusClass] and installs ColorOS clip-data, access decision, and notification gates.
     *
     * 遍历 [oplusClass] 类层级并挂载 ColorOS 的数据过滤、访问校验与通知拦截闸门。
     */
    fun installGates(oplusClass: Class<*>) {
        var current: Class<*>? = oplusClass
        while (current != null && current != Any::class.java) {
            for (method in current.declaredMethods) {
                if (Modifier.isAbstract(method.modifiers) || method.isSynthetic || method.isBridge) {
                    continue
                }
                when (method.name) {
                    in OPLUS_CLIP_DATA_GATE_METHODS ->
                        gateSupport.hookClipDataGate(
                            method = method,
                            gateLabel = "ColorOS ClipData gate",
                            denyDetail = "Denied by rule (ColorOS clipboard gate)"
                        )

                    in OPLUS_ACCESS_GATE_METHODS ->
                        gateSupport.hookBooleanAccessGate(
                            method = method,
                            gateLabel = "ColorOS access gate",
                            denyDetail = "Denied by rule (ColorOS access check)"
                        )

                    in OPLUS_NOTIFICATION_METHODS ->
                        gateSupport.hookPromptGate(
                            method = method,
                            gateLabel = "ColorOS notification gate",
                            shortCircuitOnUserGrant = false
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
        private const val TAG = "ColorOsGateInterceptors"

        /**
         * Method names of ColorOS clipboard data filtering and anti-peeping gates.
         *
         * ColorOS 剪贴板数据过滤与智能防窥闸门的方法名称集合。
         */
        val OPLUS_CLIP_DATA_GATE_METHODS = setOf(
            "adjustClipData"
        )

        /**
         * Method names of ColorOS clipboard access permission gates.
         *
         * ColorOS 剪贴板访问权限校验闸门的方法名称集合。
         */
        val OPLUS_ACCESS_GATE_METHODS = setOf(
            "hookClipboardAccessAllowed",
            "isAllowedToReadClipboard",
            "checkClipboardAccessAllowed"
        )

        /**
         * Method names of ColorOS clipboard access notification triggers.
         *
         * ColorOS 剪贴板读取通知触发方法名称集合。
         */
        val OPLUS_NOTIFICATION_METHODS = setOf(
            "showAccessNotification",
            "showAccessNotificationLocked",
            "hookShowAccessNotification"
        )
    }
}
