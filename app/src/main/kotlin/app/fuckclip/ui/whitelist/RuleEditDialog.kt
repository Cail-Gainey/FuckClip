package app.fuckclip.ui.whitelist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.fuckclip.R
import app.fuckclip.core.model.WhitelistRule

/**
 * Dialog allowing users to edit independent read and write rules for an application.
 *
 * 允许用户针对指定应用独立配置剪贴板读取与写入规则的对话框组件。
 */
@Composable
internal fun RuleEditDialog(
    app: AppItem,
    onDismiss: () -> Unit,
    onSave: (readRule: Int, writeRule: Int) -> Unit,
    onReset: () -> Unit
) {
    var selectedReadRule by remember { mutableIntStateOf(app.readRule) }
    var selectedWriteRule by remember { mutableIntStateOf(app.writeRule) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = app.label, style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.ui_rule_section_read),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                RuleSelectorRow(
                    selectedRule = selectedReadRule,
                    onSelect = { selectedReadRule = it }
                )

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = stringResource(R.string.ui_rule_section_write),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))
                RuleSelectorRow(
                    selectedRule = selectedWriteRule,
                    onSelect = { selectedWriteRule = it }
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(selectedReadRule, selectedWriteRule) }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            Row {
                if (app.isConfigured) {
                    TextButton(onClick = onReset) {
                        Text(stringResource(R.string.action_reset_default))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
    )
}

/**
 * Segmented row of filter chips allowing selection among Ask, Allow, and Deny rules.
 *
 * 供用户在询问、允许、拒绝三种规则之间进行单选的标签行组件。
 */
@Composable
internal fun RuleSelectorRow(
    selectedRule: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val options = listOf(
            Pair(WhitelistRule.RULE_REQUEST, stringResource(R.string.ui_rule_ask)),
            Pair(WhitelistRule.RULE_ALLOW, stringResource(R.string.ui_rule_allow)),
            Pair(WhitelistRule.RULE_DENY, stringResource(R.string.ui_rule_deny))
        )

        options.forEach { (rule, title) ->
            FilterChip(
                selected = selectedRule == rule,
                onClick = { onSelect(rule) },
                label = { Text(title) },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = when (rule) {
                        WhitelistRule.RULE_ALLOW -> MaterialTheme.colorScheme.primaryContainer
                        WhitelistRule.RULE_DENY -> MaterialTheme.colorScheme.errorContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    }
                )
            )
        }
    }
}
