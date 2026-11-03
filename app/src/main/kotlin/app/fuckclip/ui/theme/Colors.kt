package app.fuckclip.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Default fallback seed color used when dynamic wallpaper extraction is unavailable.
 *
 * 当动态壁纸取色不可用时使用的默认兜底种子颜色（青绿）。
 */
val FallbackSeedColor: Color = Color(0xFF008577)

/**
 * List of preset key seed colors available for user selection.
 *
 * 供用户选择的预设关键种子颜色列表。
 */
val PresetColorOptions: List<Int> = listOf(
    Color(0xFF008577).toArgb(), // Teal (default)
    Color(0xFF0061A4).toArgb(), // Blue
    Color(0xFF6750A4).toArgb(), // Violet
    Color(0xFF984061).toArgb(), // Rose
    Color(0xFF825500).toArgb(), // Amber
    Color(0xFFF44336).toArgb(), // Red
    Color(0xFFE91E63).toArgb(), // Pink
    Color(0xFF673AB7).toArgb(), // Deep Purple
    Color(0xFF3F51B5).toArgb(), // Indigo
    Color(0xFF2196F3).toArgb(), // Light Blue
    Color(0xFF00BCD4).toArgb(), // Cyan
    Color(0xFF4FAF50).toArgb(), // Green
    Color(0xFFFFEB3B).toArgb(), // Yellow
    Color(0xFFFF9800).toArgb(), // Orange
    Color(0xFF795548).toArgb(), // Brown
    Color(0xFF607D8F).toArgb(), // Blue Grey
)
