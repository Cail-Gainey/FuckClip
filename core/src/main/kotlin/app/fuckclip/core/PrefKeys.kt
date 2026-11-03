package app.fuckclip.core

/**
 * Constants for remote preference storage keys and group names.
 *
 * 远程配置存储的键名与分组常量。
 */
object PrefKeys {
    /**
     * Remote preferences group name shared between module and framework.
     *
     * 模块与框架共享的远程配置分组名称。
     */
    const val PREFS_GROUP: String = "fuckclip_preferences"

    /**
     * Preference key indicating whether the clipboard bypass feature is globally enabled.
     *
     * 标记剪贴板放行功能是否全局启用的配置键。
     */
    const val KEY_MASTER_ENABLED: String = "master_enabled"

    /**
     * Preference key for storing serialized whitelist of permitted package names.
     *
     * 存储已放行应用包名序列化白名单的配置键。
     */
    const val KEY_WHITELIST: String = "whitelist_packages"

    /**
     * Preference key indicating whether rogue app frequent read throttling is globally enabled.
     *
     * 标记是否全局启用毒瘤应用频繁读取限流与防轰炸保护的配置键。
     */
    const val KEY_ROGUE_THROTTLE_ENABLED: String = "rogue_throttle_enabled"

    /**
     * Preference key specifying the cooldown duration in seconds following a denied or dismissed prompt.
     *
     * 指定用户拒绝或关闭授权弹窗后的静默冷却时长（秒）的配置键。
     */
    const val KEY_DENY_COOLDOWN_SECONDS: String = "deny_cooldown_seconds"

    /**
     * Preference key specifying the temporary burst allowance window in seconds following a one-time grant.
     *
     * 指定用户单次允许后快速连读免弹窗通行窗口时长（秒）的配置键。
     */
    const val KEY_GRANT_BURST_SECONDS: String = "grant_burst_seconds"

    /**
     * Preference key specifying maximum allowed read attempts within sliding window before rate limiting.
     *
     * 指定滑动时间窗口内触发限流截断的最大读取尝试次数阈值的配置键。
     */
    const val KEY_MAX_READ_FREQUENCY: String = "max_read_frequency"

    /**
     * Preference key specifying how many reads within a short window are tolerated before silent throttling.
     *
     * 指定短时间内允许连续放行的读取次数上限（超出后才触发静默限流）的配置键。
     */
    const val KEY_READ_TOLERANCE_COUNT: String = "read_tolerance_count"

    /**
     * Preference key for storing user-selected language BCP 47 tag (e.g. "", "zh-CN", "zh-TW", "en").
     *
     * 存储用户在应用内所选语言的 BCP 47 标签（例如："" 跟随系统、"zh-CN"、"zh-TW"、"en"）的配置键。
     */
    const val KEY_SELECTED_LANGUAGE: String = "selected_language"

    /**
     * Preference key for storing theme color mode (System, Light, Dark, AMOLED).
     *
     * 存储主题色彩模式（跟随系统、浅色、深色、纯黑）的配置键。
     */
    const val KEY_THEME_COLOR_MODE: String = "theme_color_mode"

    /**
     * Preference key for storing theme seed key color (0 for dynamic Monet, non-zero for custom ARGB).
     *
     * 存储主题种子主色（0 为 Monet 动态壁纸取色，非 0 为自定义 ARGB）的配置键。
     */
    const val KEY_THEME_KEY_COLOR: String = "theme_key_color"
}
