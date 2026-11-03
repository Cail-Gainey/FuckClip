package app.fuckclip.ui.whitelist

import android.content.Context
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import app.fuckclip.R
import app.fuckclip.core.model.WhitelistRule
import app.fuckclip.ui.AppInfoCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Card representing a single installed application and its currently configured read/write clipboard rules.
 *
 * 承载单款已安装应用及其当前配置的读写剪贴板规则卡片组件。
 */
@Composable
internal fun AppRuleCard(
    app: AppItem,
    context: Context,
    onClick: () -> Unit
) {
    var appBitmap by remember(app.packageName) { mutableStateOf(AppInfoCache.getIcon(app.packageName)) }

    LaunchedEffect(app.packageName) {
        if (appBitmap == null) {
            withContext(Dispatchers.IO) {
                val bitmap = try {
                    context.packageManager.getApplicationIcon(app.packageName).toBitmap(96, 96)
                } catch (_: Throwable) {
                    null
                }
                if (bitmap != null) {
                    AppInfoCache.putIcon(app.packageName, bitmap)
                    appBitmap = bitmap
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (app.isConfigured) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            } else {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                appBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                } ?: run {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Label and Package
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Badges
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (app.isConfigured) {
                    RuleBadge(isWrite = false, rule = app.readRule)
                    RuleBadge(isWrite = true, rule = app.writeRule)
                } else {
                    DefaultRuleBadge()
                }
            }
        }
    }
}

/**
 * Neutral badge marking an application that still follows the default Ask behaviour without any saved rule.
 *
 * 标记尚未保存任何规则、仍处于默认询问状态应用的灰度徽标。
 */
@Composable
internal fun DefaultRuleBadge() {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = stringResource(
                R.string.ui_rule_badge,
                stringResource(R.string.ui_rule_default),
                stringResource(R.string.ui_rule_ask)
            ),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}

/**
 * Directional badge displaying the current policy (Ask, Allow, Deny) for either read or write access.
 *
 * 显示读取或写入通道当前权限规则（询问、允许、拒绝）的方向徽标。
 */
@Composable
internal fun RuleBadge(isWrite: Boolean, rule: Int) {
    val direction = if (isWrite) stringResource(R.string.ui_rule_write) else stringResource(R.string.ui_rule_read)
    val label = when (rule) {
        WhitelistRule.RULE_ALLOW -> stringResource(R.string.ui_rule_allow)
        WhitelistRule.RULE_DENY -> stringResource(R.string.ui_rule_deny)
        else -> stringResource(R.string.ui_rule_ask)
    }
    val (bg, fg) = when (rule) {
        WhitelistRule.RULE_ALLOW -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        WhitelistRule.RULE_DENY -> Pair(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer
        )
        else -> Pair(
            MaterialTheme.colorScheme.surfaceContainerHighest,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = stringResource(R.string.ui_rule_badge, direction, label),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = fg,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
        )
    }
}
