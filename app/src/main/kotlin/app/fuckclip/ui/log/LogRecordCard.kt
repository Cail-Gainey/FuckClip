package app.fuckclip.ui.log

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
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import app.fuckclip.R
import app.fuckclip.core.model.HookLog
import app.fuckclip.ui.AppInfoCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Card representing a single clipboard interception or access record with app icon, name, action badge, and content preview.
 *
 * 承载单条剪贴板拦截或放行访问记录的卡片，展示应用图标、应用名、读写操作徽章以及文本内容预览。
 */
@Composable
internal fun LogRecordCard(
    log: HookLog,
    context: Context,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var appBitmap by remember(log.packageName) {
        mutableStateOf(AppInfoCache.getIcon(log.packageName))
    }
    var appLabel by remember(log.packageName, log.appName) {
        mutableStateOf(log.appName ?: AppInfoCache.getLabel(log.packageName))
    }

    LaunchedEffect(log.packageName) {
        if (appBitmap == null || appLabel == null) {
            withContext(Dispatchers.IO) {
                val pm = context.packageManager
                if (appLabel == null) {
                    val resolvedLabel = try {
                        pm.getApplicationInfo(log.packageName, 0).loadLabel(pm).toString()
                    } catch (_: Throwable) {
                        log.packageName
                    }
                    AppInfoCache.putLabel(log.packageName, resolvedLabel)
                    appLabel = resolvedLabel
                }
                if (appBitmap == null) {
                    val bitmap = try {
                        pm.getApplicationIcon(log.packageName).toBitmap(96, 96)
                    } catch (_: Throwable) {
                        null
                    }
                    if (bitmap != null) {
                        AppInfoCache.putIcon(log.packageName, bitmap)
                        appBitmap = bitmap
                    }
                }
            }
        }
    }

    val isWrite = log.action.equals("WRITE", ignoreCase = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Icon + App Name / Package + Action Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    appBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(34.dp)
                        )
                    } ?: Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // App Label and Package Name
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appLabel ?: log.packageName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = log.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Direction and Action Badge Pill (区分读取与写入)
                ActionDirectionBadge(
                    isWrite = isWrite,
                    allowed = log.allowed
                )

                if (log.count > 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                    CountSuperscriptBadge(count = log.count)
                }
            }

            // Clipboard Content Preview
            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    val content = log.content
                    if (!content.isNullOrBlank()) {
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.log_empty_content),
                            style = MaterialTheme.typography.bodySmall,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }

            // Footer: Detail and Timestamp
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = log.detail.ifEmpty { log.action },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/**
 * Superscript badge showing how many repeated accesses were aggregated into one record.
 *
 * 展示单条记录聚合了多少次重复访问的数量上标徽标。
 */
@Composable
internal fun CountSuperscriptBadge(count: Int, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
    ) {
        Text(
            text = stringResource(R.string.ui_log_count_badge, count),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

/**
 * Directional action badge pill distinguishing read vs write and allowed vs denied.
 *
 * 区分剪贴板读取与写入，并展现已放行与已拦截状态的徽标胶囊组件。
 */
@Composable
internal fun ActionDirectionBadge(
    isWrite: Boolean,
    allowed: Boolean,
    modifier: Modifier = Modifier
) {
    val directionText = stringResource(if (isWrite) R.string.ui_rule_write else R.string.ui_rule_read)
    val actionText = stringResource(if (allowed) R.string.ui_action_allowed else R.string.ui_action_denied)
    val badgeText = stringResource(R.string.ui_rule_badge, directionText, actionText)

    val containerColor: Color
    val contentColor: Color

    if (allowed) {
        if (isWrite) {
            containerColor = MaterialTheme.colorScheme.secondaryContainer
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            containerColor = MaterialTheme.colorScheme.primaryContainer
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        }
    } else {
        containerColor = MaterialTheme.colorScheme.errorContainer
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isWrite) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = contentColor
            )
        }
    }
}
