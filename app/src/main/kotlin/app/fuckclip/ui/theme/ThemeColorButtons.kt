package app.fuckclip.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.fuckclip.R
import com.materialkolor.PaletteStyle

/**
 * Swatch row for picking between dynamic Monet and preset palette seed colors.
 *
 * 供用户选择莫奈动态壁纸取色或预设种子调色板的色块组件行。
 */
@Composable
internal fun ThemePaletteSelector(
    selectedKeyColor: Int,
    currentPaletteStyle: PaletteStyle,
    onKeyColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.theme_palette),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            // Item 0: Wallpaper Monet (keyColor == 0)
            item {
                MonetColorButton(
                    isSelected = selectedKeyColor == 0,
                    onClick = { onKeyColorSelected(0) }
                )
            }

            // Preset Colors
            items(PresetColorOptions) { colorInt ->
                PresetColorButton(
                    color = Color(colorInt),
                    isSelected = selectedKeyColor == colorInt,
                    paletteStyle = currentPaletteStyle,
                    onClick = { onKeyColorSelected(colorInt) }
                )
            }
        }
    }
}

/**
 * Wallpaper Monet button allowing dynamic wallpaper extraction.
 *
 * 允许提取动态壁纸色彩的莫奈色彩按钮。
 */
@Composable
internal fun MonetColorButton(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.08f else 1.0f, label = "monet_scale")
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .size(68.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = stringResource(R.string.theme_monet),
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.theme_seed_monet),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }
    }
}

/**
 * Preset color button with split primary/tertiary container arcs.
 *
 * 具有 Primary/Tertiary 双弧分割圆形的预设色彩按钮。
 */
@Composable
internal fun PresetColorButton(
    color: Color,
    isSelected: Boolean,
    paletteStyle: PaletteStyle,
    onClick: () -> Unit
) {
    val darkTheme = isInDarkTheme()
    val scheme = rememberFuckClipColorScheme(
        seedColor = color,
        isDark = darkTheme,
        isAmoled = false,
        paletteStyle = paletteStyle
    )

    val scale by animateFloatAsState(targetValue = if (isSelected) 1.08f else 1.0f, label = "color_scale")

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = scheme.surfaceContainer,
        modifier = Modifier
            .size(68.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, scheme.primary) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(40.dp)) {
                drawArc(
                    color = scheme.primaryContainer,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true
                )
                drawArc(
                    color = scheme.tertiaryContainer,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = true
                )
            }

            AnimatedVisibility(
                visible = isSelected,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut(targetScale = 0.8f)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
