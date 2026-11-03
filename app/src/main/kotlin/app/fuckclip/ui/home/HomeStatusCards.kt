package app.fuckclip.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.fuckclip.R

/**
 * Core hero status banner rendering module activation state, framework description, and active status pill.
 *
 * 核心状态横幅，渲染模块激活状态、框架详情以及运行状态徽标。
 */
@Composable
internal fun HeroStatusCard(
    isActivated: Boolean,
    masterEnabled: Boolean,
    frameworkDesc: String,
    modifier: Modifier = Modifier
) {
    val isFullyActive = isActivated && masterEnabled
    val targetContainerColor = when {
        isFullyActive -> MaterialTheme.colorScheme.primaryContainer
        !isActivated -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val containerColor by animateColorAsState(targetValue = targetContainerColor, animationSpec = spring(), label = "hero_color")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when {
                            isFullyActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            !isActivated -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        isFullyActive -> Icons.Default.Shield
                        !isActivated -> Icons.Default.Error
                        else -> Icons.Default.Security
                    },
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = when {
                        isFullyActive -> MaterialTheme.colorScheme.primary
                        !isActivated -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = when {
                            isFullyActive -> stringResource(R.string.dashboard_status_active)
                            !isActivated -> stringResource(R.string.dashboard_status_inactive)
                            else -> stringResource(R.string.dashboard_status_paused)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isFullyActive -> MaterialTheme.colorScheme.onPrimaryContainer
                            !isActivated -> MaterialTheme.colorScheme.onErrorContainer
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = when {
                            isFullyActive -> MaterialTheme.colorScheme.primary
                            !isActivated -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline
                        }
                    ) {
                        Text(
                            text = when {
                                isFullyActive -> stringResource(R.string.dashboard_status_pill)
                                !isActivated -> stringResource(R.string.dashboard_status_pill_inactive)
                                else -> stringResource(R.string.dashboard_status_pill_paused)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isFullyActive -> MaterialTheme.colorScheme.onPrimary
                                !isActivated -> MaterialTheme.colorScheme.onError
                                else -> MaterialTheme.colorScheme.surface
                            },
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isActivated) {
                        "$frameworkDesc · " + stringResource(R.string.dashboard_status_desc)
                    } else {
                        stringResource(R.string.dashboard_status_desc_inactive)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        isFullyActive -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                        !isActivated -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

/**
 * Master protection control card housing the global toggle switch.
 *
 * 全局防护控制卡片，包含总开关与功能描述。
 */
@Composable
internal fun MasterSwitchCard(
    masterEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_master_switch),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.settings_master_switch_summary),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Switch(
                checked = masterEnabled,
                onCheckedChange = onToggle
            )
        }
    }
}
