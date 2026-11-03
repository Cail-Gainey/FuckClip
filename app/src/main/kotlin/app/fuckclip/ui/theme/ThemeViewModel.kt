package app.fuckclip.ui.theme

import androidx.lifecycle.ViewModel
import app.fuckclip.data.SettingsRepository
import com.materialkolor.PaletteStyle
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel managing user interface theme preferences and customization state.
 *
 * 管理用户界面主题偏好与外观定制状态的 ViewModel。
 */
class ThemeViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    /**
     * Observable stream representing the display color mode (System, Light, Dark, AMOLED).
     *
     * 代表显示色彩模式（跟随系统、浅色、深色、纯黑）的可观察流。
     */
    val themeColorMode: StateFlow<ColorMode> = repository.themeColorMode

    /**
     * Observable stream representing the key seed color (0 for dynamic Monet, non-zero for custom ARGB).
     *
     * 代表主题关键种子颜色的可观察流（0 为莫奈动态壁纸取色，非 0 为自定义 ARGB）。
     */
    val themeKeyColor: StateFlow<Int> = repository.themeKeyColor

    /**
     * Observable stream representing the Material 3 palette style algorithm.
     *
     * 代表 Material 3 调色板风格算法的可观察流。
     */
    val themePaletteStyle: StateFlow<PaletteStyle> = repository.themePaletteStyle

    /**
     * Observable stream representing whether the floating navigation bar style is enabled.
     *
     * 代表是否启用悬浮底栏样式的可观察流。
     */
    val isFloatingNavEnabled: StateFlow<Boolean> = repository.isFloatingNavEnabled

    /**
     * Updates the active [ColorMode].
     *
     * 更新当前生效的 [ColorMode]。
     */
    fun setThemeColorMode(mode: ColorMode) {
        repository.setThemeColorMode(mode)
    }

    /**
     * Updates the active seed key color.
     *
     * 更新当前生效的种子关键颜色。
     */
    fun setThemeKeyColor(color: Int) {
        repository.setThemeKeyColor(color)
    }

    /**
     * Updates the active [PaletteStyle].
     *
     * 更新当前生效的 [PaletteStyle] 调色风格。
     */
    fun setThemePaletteStyle(style: PaletteStyle) {
        repository.setThemePaletteStyle(style)
    }

    /**
     * Toggles the floating navigation bar appearance.
     *
     * 切换悬浮底栏外观。
     */
    fun setFloatingNavEnabled(enabled: Boolean) {
        repository.setFloatingNavEnabled(enabled)
    }
}
