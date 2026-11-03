package app.fuckclip.ui.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.fuckclip.R

/**
 * 3-column metric cards rendering rule count, blocked count, and allowed count.
 *
 * 三列指标卡片看板，展示规则数、拦截数与放行数。
 */
@Composable
internal fun MetricsOverviewGrid(
    whitelistCount: Int,
    deniedCount: Int,
    grantedCount: Int,
    onRulesClick: () -> Unit,
    onDeniedClick: () -> Unit,
    onGrantedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricItemCard(
            count = whitelistCount,
            label = stringResource(R.string.dashboard_stat_rules),
            icon = Icons.Default.Checklist,
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = onRulesClick,
            modifier = Modifier.weight(1f)
        )

        MetricItemCard(
            count = deniedCount,
            label = stringResource(R.string.dashboard_stat_denied),
            icon = Icons.Default.Block,
            accentColor = MaterialTheme.colorScheme.error,
            onClick = onDeniedClick,
            modifier = Modifier.weight(1f)
        )

        MetricItemCard(
            count = grantedCount,
            label = stringResource(R.string.dashboard_stat_granted),
            icon = Icons.Default.CheckCircle,
            accentColor = Color(0xFF2E7D32),
            onClick = onGrantedClick,
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Single metric card item with number, label, and icon.
 *
 * 具有数值、标签与图标的单项统计指标卡片。
 */
@Composable
internal fun MetricItemCard(
    count: Int,
    label: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
