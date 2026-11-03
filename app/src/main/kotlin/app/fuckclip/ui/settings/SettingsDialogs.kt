package app.fuckclip.ui.settings

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
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.fuckclip.R
import kotlin.math.roundToInt

/**
 * Dialog for selecting the application language from supported locales.
 *
 * 供用户从受支持语言列表中选择应用语言的弹窗组件。
 */
@Composable
internal fun LanguageSelectionDialog(
    supportedLanguages: List<Pair<String, String>>,
    selectedTag: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            Column {
                supportedLanguages.forEach { (tag, name) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(tag) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (tag == selectedTag),
                            onClick = { onSelect(tag) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = name, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ui_close))
            }
        }
    )
}

/**
 * Dialog for configuring rogue application throttling parameters with sliders.
 *
 * 通过滑块组件配置毒瘤应用限流与冷却参数的设置对话框。
 */
@Composable
internal fun ThrottleConfigDialog(
    initialCooldown: Int,
    initialBurst: Int,
    initialFrequency: Int,
    initialTolerance: Int,
    onSave: (cooldown: Int, burst: Int, frequency: Int, tolerance: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var cooldown by remember { mutableFloatStateOf(initialCooldown.toFloat()) }
    var burst by remember { mutableFloatStateOf(initialBurst.toFloat()) }
    var frequency by remember { mutableFloatStateOf(initialFrequency.toFloat()) }
    var tolerance by remember { mutableFloatStateOf(initialTolerance.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_rogue_throttle_config_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Deny Cooldown
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.home_rogue_throttle_cooldown_label),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.home_rogue_throttle_seconds, cooldown.roundToInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = cooldown,
                        onValueChange = { cooldown = it },
                        valueRange = 5f..120f,
                        steps = 22
                    )
                }

                // Grant Burst Window
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.home_rogue_throttle_grant_burst_label),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.home_rogue_throttle_seconds, burst.roundToInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = burst,
                        onValueChange = { burst = it },
                        valueRange = 1f..30f,
                        steps = 28
                    )
                }

                // Max Read Frequency
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.home_rogue_throttle_frequency_label),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.home_rogue_throttle_times, frequency.roundToInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = frequency,
                        onValueChange = { frequency = it },
                        valueRange = 2f..20f,
                        steps = 17
                    )
                }

                // Reads Tolerated Before Silent Throttling
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.home_rogue_throttle_tolerance_label),
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = stringResource(R.string.home_rogue_throttle_tolerance_times, tolerance.roundToInt()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = tolerance,
                        onValueChange = { tolerance = it },
                        valueRange = 1f..10f,
                        steps = 8
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(cooldown.roundToInt(), burst.roundToInt(), frequency.roundToInt(), tolerance.roundToInt())
                }
            ) {
                Text(stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
