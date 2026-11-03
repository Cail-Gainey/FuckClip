package app.fuckclip.ui.theme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.fuckclip.R
import com.materialkolor.PaletteStyle

/**
 * Selection dialog for Material 3 palette style algorithm.
 *
 * Material 3 调色算法风格选择对话框。
 */
@Composable
internal fun PaletteStyleDialog(
    currentStyle: PaletteStyle,
    onStyleSelected: (PaletteStyle) -> Unit,
    onDismiss: () -> Unit
) {
    val styles = listOf(
        PaletteStyle.TonalSpot,
        PaletteStyle.Vibrant,
        PaletteStyle.Expressive,
        PaletteStyle.Neutral,
        PaletteStyle.Monochrome
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.theme_style)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                styles.forEach { style ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStyleSelected(style) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentStyle == style,
                            onClick = { onStyleSelected(style) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = getPaletteStyleLabel(style),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

/**
 * Resolves localized display title for a given [PaletteStyle].
 *
 * 解析指定 [PaletteStyle] 的本地化显示标题。
 */
@Composable
internal fun getPaletteStyleLabel(style: PaletteStyle): String {
    return when (style) {
        PaletteStyle.TonalSpot -> stringResource(R.string.theme_variant_tonal_spot)
        PaletteStyle.Vibrant -> stringResource(R.string.theme_variant_vibrant)
        PaletteStyle.Expressive -> stringResource(R.string.theme_variant_expressive)
        PaletteStyle.Neutral -> stringResource(R.string.theme_variant_neutral)
        PaletteStyle.Monochrome -> stringResource(R.string.theme_variant_monochrome)
        else -> style.name
    }
}
