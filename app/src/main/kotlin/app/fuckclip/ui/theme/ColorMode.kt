package app.fuckclip.ui.theme

/**
 * Display color mode options supported across the user interface.
 *
 * 用户界面支持的显示色彩模式选项。
 */
enum class ColorMode(
    /**
     * Integer representation of the color mode.
     *
     * 色彩模式的整型代码表示。
     */
    val value: Int
) {
    /**
     * Follow system dark/light mode preference.
     *
     * 跟随系统深浅色模式偏好。
     */
    SYSTEM(0),

    /**
     * Force light theme appearance.
     *
     * 强制浅色主题外观。
     */
    LIGHT(1),

    /**
     * Force dark theme appearance.
     *
     * 强制深色主题外观。
     */
    DARK(2),

    /**
     * Force AMOLED pure black appearance in dark mode.
     *
     * 强制深色纯黑（AMOLED）外观。
     */
    DARK_AMOLED(3);

    /**
     * Whether this mode follows the system dark theme configuration.
     *
     * 该模式是否跟随系统深色主题配置。
     */
    val isSystem: Boolean get() = this == SYSTEM

    /**
     * Whether this mode forces a dark or pure black theme.
     *
     * 该模式是否强制为深色或纯黑主题。
     */
    val isDark: Boolean get() = this == DARK || this == DARK_AMOLED

    /**
     * Whether this mode activates pure black background colors.
     *
     * 该模式是否启用纯黑背景色。
     */
    val isAmoled: Boolean get() = this == DARK_AMOLED

    companion object {
        /**
         * Resolves a [ColorMode] from its integer value, defaulting to [SYSTEM].
         *
         * 根据整型数值解析 [ColorMode]，默认回退至 [SYSTEM]。
         */
        fun fromValue(value: Int): ColorMode = entries.find { it.value == value } ?: SYSTEM
    }
}
