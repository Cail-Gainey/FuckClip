package app.fuckclip.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.fuckclip.R

/**
 * Interactive card displaying rogue application frequent read restriction toggle and status pill.
 *
 * 展示毒瘤应用频繁读取限制开关、状态胶囊与配置入口的交互卡片。
 */
@Composable
internal fun RogueThrottleCard(
    enabled: Boolean,
    denyCooldownSeconds: Int,
    maxReadFrequency: Int,
    readTolerance: Int,
    onToggle: (Boolean) -> Unit,
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = if (enabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.home_rogue_throttle_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.home_rogue_throttle_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle
                )
            }

            AnimatedVisibility(visible = enabled) {
                Column {
                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 0.8.dp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onConfigure)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(
                                    R.string.home_rogue_throttle_status,
                                    denyCooldownSeconds,
                                    readTolerance,
                                    maxReadFrequency
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Configuration dialog allowing users to adjust deny cooldown, temporary grant window, and rate limit frequency.
 *
 * 允许用户调节拒绝冷却时长、单次允许连读窗口以及读取频率限制阈值的设置对话框。
 */
@Composable
internal fun RogueThrottleConfigDialog(
    denyCooldownSeconds: Int,
    grantBurstSeconds: Int,
    maxReadFrequency: Int,
    readTolerance: Int,
    onSelectDenyCooldown: (Int) -> Unit,
    onSelectGrantBurst: (Int) -> Unit,
    onSelectMaxFrequency: (Int) -> Unit,
    onSelectReadTolerance: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val cooldownOptions = listOf(15, 30, 60, 300)
    val grantBurstOptions = listOf(3, 5, 10)
    val frequencyOptions = listOf(3, 5, 10)
    val toleranceOptions = listOf(1, 2, 3, 5, 10)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.home_rogue_throttle_config_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Deny Cooldown
                Column {
                    Text(
                        text = stringResource(R.string.home_rogue_throttle_cooldown_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.home_rogue_throttle_cooldown_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        cooldownOptions.forEach { sec ->
                            val selected = sec == denyCooldownSeconds
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectDenyCooldown(sec) },
                                label = {
                                    Text(
                                        text = stringResource(R.string.home_rogue_throttle_seconds, sec),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Section 2: Grant Burst Window
                Column {
                    Text(
                        text = stringResource(R.string.home_rogue_throttle_grant_burst_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.home_rogue_throttle_grant_burst_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        grantBurstOptions.forEach { sec ->
                            val selected = sec == grantBurstSeconds
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectGrantBurst(sec) },
                                label = {
                                    Text(
                                        text = stringResource(R.string.home_rogue_throttle_seconds, sec),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Section 3: Read Rate Limit Frequency
                Column {
                    Text(
                        text = stringResource(R.string.home_rogue_throttle_frequency_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.home_rogue_throttle_frequency_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        frequencyOptions.forEach { count ->
                            val selected = count == maxReadFrequency
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectMaxFrequency(count) },
                                label = {
                                    Text(
                                        text = stringResource(R.string.home_rogue_throttle_times, count),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Section 4: Reads tolerated before silent throttling starts
                Column {
                    Text(
                        text = stringResource(R.string.home_rogue_throttle_tolerance_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.home_rogue_throttle_tolerance_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        toleranceOptions.forEach { count ->
                            val selected = count == readTolerance
                            FilterChip(
                                selected = selected,
                                onClick = { onSelectReadTolerance(count) },
                                label = {
                                    Text(
                                        text = stringResource(R.string.home_rogue_throttle_tolerance_times, count),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ui_close))
            }
        }
    )
}
