package app.fuckclip.core.model

import kotlinx.serialization.Serializable

/**
 * Data structure representing a package permission rule in the whitelist.
 *
 * 白名单中的应用权限规则数据结构。
 */
@Serializable
data class WhitelistRule(
    /**
     * Package name of the target application.
     *
     * 目标应用的包名。
     */
    val packageName: String,

    /**
     * Permission rule for read operations (0: REQUEST/Ask, 1: ALLOW, 2: DENY).
     *
     * 读取权限规则（0: 询问，1: 允许，2: 拒绝）。
     */
    val readRule: Int = RULE_REQUEST,

    /**
     * Permission rule for write operations (0: REQUEST/Ask, 1: ALLOW, 2: DENY).
     *
     * 写入权限规则（0: 询问，1: 允许，2: 拒绝）。
     */
    val writeRule: Int = RULE_REQUEST,

    /**
     * Timestamp in milliseconds when this rule was added or updated.
     *
     * 此规则添加或更新时的时间戳（毫秒）。
     */
    val addedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Permission rule representing prompting the user via dialog.
         *
         * 弹窗询问用户的权限规则。
         */
        const val RULE_REQUEST: Int = 0

        /**
         * Permission rule allowing clipboard access directly.
         *
         * 直接允许剪贴板访问的权限规则。
         */
        const val RULE_ALLOW: Int = 1

        /**
         * Permission rule denying clipboard access directly.
         *
         * 直接拒绝剪贴板访问的权限规则。
         */
        const val RULE_DENY: Int = 2

        /**
         * Alias for [RULE_ALLOW].
         *
         * [RULE_ALLOW] 的别名。
         */
        const val RULE_GRANT: Int = RULE_ALLOW
    }
}
