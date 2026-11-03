package app.fuckclip.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowInsetsControllerCompat
import com.materialkolor.PaletteStyle

/**
 * Composition local providing the active [ColorMode] down the composable tree.
 *
 * 向 Composable 组件树向下传递当前生效 [ColorMode] 的局部组合对象。
 */
val LocalColorMode = staticCompositionLocalOf { ColorMode.SYSTEM }

/**
 * Checks whether the current display theme should be rendered in dark mode.
 *
 * 检查当前显示主题是否应以深色模式渲染。
 */
@Composable
@ReadOnlyComposable
fun isInDarkTheme(): Boolean {
    val mode = LocalColorMode.current
    return when {
        mode.isDark -> true
        mode == ColorMode.LIGHT -> false
        else -> isSystemInDarkTheme()
    }
}

/**
 * Dynamic Material 3 theme wrapper supporting Monet dynamic colors, multi-theme presets, AMOLED mode, and style variants.
 *
 * 支持莫奈动态取色、多主题预设、AMOLED 纯黑以及色彩风格变体的动态 Material 3 主题包装器。
 */
@Composable
fun FuckClipTheme(
    colorMode: ColorMode = ColorMode.SYSTEM,
    keyColor: Int = 0,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val darkTheme = colorMode.isDark || (colorMode.isSystem && systemDark)
    val amoledMode = colorMode.isAmoled

    val seedColor = if (keyColor == 0) Color.Unspecified else Color(keyColor)
    val baseScheme = rememberFuckClipColorScheme(
        seedColor = seedColor,
        isDark = darkTheme,
        isAmoled = amoledMode,
        paletteStyle = paletteStyle
    )
    val animatedScheme = baseScheme.animateAsState()

    LaunchedEffect(darkTheme) {
        val window = (context as? Activity)?.window ?: return@LaunchedEffect
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalColorMode provides colorMode) {
        MaterialTheme(
            colorScheme = animatedScheme,
            content = content
        )
    }
}
