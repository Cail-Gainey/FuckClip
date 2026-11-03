package app.fuckclip.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme

/**
 * Overrides background and surface roles with pure black when AMOLED mode is requested.
 *
 * 当请求 AMOLED 模式时将背景和各级表面颜色覆盖为纯黑色。
 */
fun ColorScheme.amoledBackground(amoled: Boolean): ColorScheme =
    if (!amoled) this
    else copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color.Black,
        surfaceContainer = Color.Black,
        surfaceContainerHigh = Color.Black,
        surfaceContainerHighest = Color.Black,
    )

/**
 * Derives a dynamic Material 3 [ColorScheme] matching the selected seed color, dark mode, and palette style.
 *
 * 根据所选种子颜色、深色模式及调色风格派生匹配的动态 Material 3 [ColorScheme]。
 */
@Composable
fun rememberFuckClipColorScheme(
    seedColor: Color,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle = PaletteStyle.TonalSpot
): ColorScheme {
    val context = LocalContext.current
    val seed = when {
        seedColor != Color.Unspecified -> seedColor
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamicScheme = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            dynamicScheme.primary
        }
        else -> FallbackSeedColor
    }
    return rememberDynamicColorScheme(
        seedColor = seed,
        isDark = isDark,
        isAmoled = isAmoled,
        style = paletteStyle
    ).amoledBackground(isAmoled)
}

/**
 * Smoothly animates all colors within a [ColorScheme] using spring physics.
 *
 * 使用弹簧动画规范平滑过渡 [ColorScheme] 内的所有色彩值。
 */
@Composable
fun ColorScheme.animateAsState(): ColorScheme {
    @Composable
    fun animateColor(color: Color): Color = animateColorAsState(
        targetValue = color,
        animationSpec = spring(),
        label = "theme_color_transition"
    ).value

    return copy(
        primary = animateColor(primary),
        onPrimary = animateColor(onPrimary),
        primaryContainer = animateColor(primaryContainer),
        onPrimaryContainer = animateColor(onPrimaryContainer),
        inversePrimary = animateColor(inversePrimary),
        secondary = animateColor(secondary),
        onSecondary = animateColor(onSecondary),
        secondaryContainer = animateColor(secondaryContainer),
        onSecondaryContainer = animateColor(onSecondaryContainer),
        tertiary = animateColor(tertiary),
        onTertiary = animateColor(onTertiary),
        tertiaryContainer = animateColor(tertiaryContainer),
        onTertiaryContainer = animateColor(onTertiaryContainer),
        background = animateColor(background),
        onBackground = animateColor(onBackground),
        surface = animateColor(surface),
        onSurface = animateColor(onSurface),
        surfaceVariant = animateColor(surfaceVariant),
        onSurfaceVariant = animateColor(onSurfaceVariant),
        surfaceTint = animateColor(surfaceTint),
        inverseSurface = animateColor(inverseSurface),
        inverseOnSurface = animateColor(inverseOnSurface),
        error = animateColor(error),
        onError = animateColor(onError),
        errorContainer = animateColor(errorContainer),
        onErrorContainer = animateColor(onErrorContainer),
        outline = animateColor(outline),
        outlineVariant = animateColor(outlineVariant),
        scrim = animateColor(scrim),
        surfaceBright = animateColor(surfaceBright),
        surfaceDim = animateColor(surfaceDim),
        surfaceContainer = animateColor(surfaceContainer),
        surfaceContainerHigh = animateColor(surfaceContainerHigh),
        surfaceContainerHighest = animateColor(surfaceContainerHighest),
        surfaceContainerLow = animateColor(surfaceContainerLow),
        surfaceContainerLowest = animateColor(surfaceContainerLowest)
    )
}
