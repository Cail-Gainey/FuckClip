package app.fuckclip.data

import android.content.SharedPreferences
import app.fuckclip.core.PrefKeys
import app.fuckclip.ui.theme.ColorMode
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages theme appearance preferences including color modes, key colors, and navigation style.
 *
 * 管理主题外观偏好，包括色彩模式、关键主色、调色板风格与导航栏样式。
 */
internal class ThemeSettingsStore(
    private val getPreferences: () -> SharedPreferences
) {
    private val _themeColorMode = MutableStateFlow(ColorMode.SYSTEM)

    /**
     * Observable stream representing the display color mode (System, Light, Dark, AMOLED).
     *
     * 代表显示色彩模式（跟随系统、浅色、深色、纯黑）的可观察流。
     */
    val themeColorMode: StateFlow<ColorMode> = _themeColorMode.asStateFlow()

    private val _themeKeyColor = MutableStateFlow(0)

    /**
     * Observable stream representing the key seed color (0 for dynamic Monet, non-zero for custom ARGB).
     *
     * 代表主题关键种子颜色的可观察流（0 为莫奈动态壁纸取色，非 0 为自定义 ARGB）。
     */
    val themeKeyColor: StateFlow<Int> = _themeKeyColor.asStateFlow()

    private val _themePaletteStyle = MutableStateFlow(PaletteStyle.TonalSpot)

    /**
     * Observable stream representing the Material 3 palette style algorithm.
     *
     * 代表 Material 3 调色板风格算法的可观察流。
     */
    val themePaletteStyle: StateFlow<PaletteStyle> = _themePaletteStyle.asStateFlow()

    private val _isFloatingNavEnabled = MutableStateFlow(false)

    /**
     * Observable stream representing whether the floating navigation bar style is enabled.
     *
     * 代表是否启用悬浮底栏样式的可观察流。
     */
    val isFloatingNavEnabled: StateFlow<Boolean> = _isFloatingNavEnabled.asStateFlow()

    /**
     * Loads initial theme preferences from local storage.
     *
     * 从本地偏好配置中加载初始主题设置。
     */
    fun loadInitial(prefs: SharedPreferences) {
        val modeVal = prefs.getInt(PrefKeys.KEY_THEME_COLOR_MODE, ColorMode.SYSTEM.value)
        _themeColorMode.value = ColorMode.fromValue(modeVal)
        _themeKeyColor.value = prefs.getInt(PrefKeys.KEY_THEME_KEY_COLOR, 0)
        val styleStr = prefs.getString(KEY_THEME_PALETTE_STYLE, PaletteStyle.TonalSpot.name) ?: PaletteStyle.TonalSpot.name
        _themePaletteStyle.value = runCatching { PaletteStyle.valueOf(styleStr) }.getOrDefault(PaletteStyle.TonalSpot)
        _isFloatingNavEnabled.value = prefs.getBoolean(KEY_THEME_FLOATING_NAV, false)
    }

    /**
     * Synchronizes theme preferences with remote configuration.
     *
     * 与远程偏好配置同步主题设置。
     */
    fun sync(remotePrefs: SharedPreferences) {
        val modeVal = remotePrefs.getInt(PrefKeys.KEY_THEME_COLOR_MODE, _themeColorMode.value.value)
        _themeColorMode.value = ColorMode.fromValue(modeVal)
        _themeKeyColor.value = remotePrefs.getInt(PrefKeys.KEY_THEME_KEY_COLOR, _themeKeyColor.value)
        val styleStr = remotePrefs.getString(KEY_THEME_PALETTE_STYLE, _themePaletteStyle.value.name) ?: _themePaletteStyle.value.name
        _themePaletteStyle.value = runCatching { PaletteStyle.valueOf(styleStr) }.getOrDefault(PaletteStyle.TonalSpot)
        _isFloatingNavEnabled.value = remotePrefs.getBoolean(KEY_THEME_FLOATING_NAV, _isFloatingNavEnabled.value)
    }

    /**
     * Updates the active [ColorMode] and persists it to preferences.
     *
     * 更新当前生效的 [ColorMode] 并持久化到配置中。
     */
    fun setThemeColorMode(mode: ColorMode) {
        _themeColorMode.value = mode
        getPreferences().edit().putInt(PrefKeys.KEY_THEME_COLOR_MODE, mode.value).apply()
    }

    /**
     * Updates the active seed key color and persists it to preferences.
     *
     * 更新当前生效的种子关键颜色并持久化到配置中。
     */
    fun setThemeKeyColor(color: Int) {
        _themeKeyColor.value = color
        getPreferences().edit().putInt(PrefKeys.KEY_THEME_KEY_COLOR, color).apply()
    }

    /**
     * Updates the active [PaletteStyle] and persists it to preferences.
     *
     * 更新当前生效的 [PaletteStyle] 调色风格并持久化到配置中。
     */
    fun setThemePaletteStyle(style: PaletteStyle) {
        _themePaletteStyle.value = style
        getPreferences().edit().putString(KEY_THEME_PALETTE_STYLE, style.name).apply()
    }

    /**
     * Toggles the floating navigation bar appearance and persists it to preferences.
     *
     * 切换悬浮底栏外观并持久化到配置中。
     */
    fun setFloatingNavEnabled(enabled: Boolean) {
        _isFloatingNavEnabled.value = enabled
        getPreferences().edit().putBoolean(KEY_THEME_FLOATING_NAV, enabled).apply()
    }

    companion object {
        /**
         * Preference key for active Material 3 palette style algorithm name.
         *
         * 生效的 Material 3 调色板算法风格名称的配置键。
         */
        const val KEY_THEME_PALETTE_STYLE = "theme_palette_style"

        /**
         * Preference key for floating bottom navigation bar appearance toggle.
         *
         * 悬浮底部导航栏外观开关的配置键。
         */
        const val KEY_THEME_FLOATING_NAV = "theme_floating_nav"
    }
}
