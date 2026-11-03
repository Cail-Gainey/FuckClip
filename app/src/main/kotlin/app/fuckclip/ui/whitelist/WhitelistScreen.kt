package app.fuckclip.ui.whitelist

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.fuckclip.R
import org.koin.androidx.compose.koinViewModel

/**
 * Whitelist configuration screen allowing users to search and configure Ask, Allow, or Deny rules per application.
 *
 * 允许用户搜索并按应用配置询问、允许或拒绝三种权限规则的权限管理界面。
 */
@Composable
fun WhitelistScreen(
    viewModel: WhitelistViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val apps by viewModel.displayedApps.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val showSystem by viewModel.showSystemApps.collectAsStateWithLifecycle()
    val filterMode by viewModel.filterMode.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var selectedAppForEdit by remember { mutableStateOf<AppItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            placeholder = { Text(stringResource(R.string.options_search)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.action_clear))
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips and actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = filterMode == RuleFilterMode.ALL,
                onClick = { viewModel.setFilterMode(RuleFilterMode.ALL) },
                label = { Text(stringResource(R.string.whitelist_filter_all)) }
            )
            FilterChip(
                selected = filterMode == RuleFilterMode.CONFIGURED,
                onClick = { viewModel.setFilterMode(RuleFilterMode.CONFIGURED) },
                label = { Text(stringResource(R.string.whitelist_filter_configured)) }
            )
            FilterChip(
                selected = filterMode == RuleFilterMode.ALLOW,
                onClick = { viewModel.setFilterMode(RuleFilterMode.ALLOW) },
                label = { Text(stringResource(R.string.ui_rule_allow)) }
            )
            FilterChip(
                selected = filterMode == RuleFilterMode.DENY,
                onClick = { viewModel.setFilterMode(RuleFilterMode.DENY) },
                label = { Text(stringResource(R.string.ui_rule_deny)) }
            )
            FilterChip(
                selected = showSystem,
                onClick = { viewModel.toggleShowSystemApps() },
                label = { Text(stringResource(R.string.options_show_system)) }
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = {
                    val exported = viewModel.exportBackup()
                    clipboardManager.setText(AnnotatedString(exported))
                    Toast.makeText(context, context.getString(R.string.ui_copied_to_clipboard), Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.FileUpload, contentDescription = stringResource(R.string.whitelist_export))
            }
            IconButton(
                onClick = {
                    importJsonText = ""
                    showImportDialog = true
                }
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = stringResource(R.string.whitelist_import))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (apps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.empty_rule_title),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(apps, key = { it.packageName }, contentType = { "app" }) { app ->
                    AppRuleCard(
                        app = app,
                        context = context,
                        onClick = { selectedAppForEdit = app }
                    )
                }
            }
        }
    }

    // App rule editing dialog
    selectedAppForEdit?.let { app ->
        RuleEditDialog(
            app = app,
            onDismiss = { selectedAppForEdit = null },
            onSave = { readRule, writeRule ->
                viewModel.setPackageRule(app.packageName, readRule, writeRule)
                selectedAppForEdit = null
            },
            onReset = {
                viewModel.resetPackageRule(app.packageName)
                selectedAppForEdit = null
            }
        )
    }

    // Import JSON backup dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text(stringResource(R.string.whitelist_import_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.whitelist_import_hint))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        placeholder = { Text("{\n  \"rules\": [...] \n}") }
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val result = viewModel.importBackup(importJsonText)
                        if (result.isSuccess) {
                            Toast.makeText(
                                context,
                                context.getString(R.string.whitelist_import_success, result.getOrNull() ?: 0),
                                Toast.LENGTH_SHORT
                            ).show()
                            showImportDialog = false
                        } else {
                            Toast.makeText(
                                context,
                                context.getString(R.string.whitelist_import_failed),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                ) {
                    Text(stringResource(R.string.whitelist_import_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text(stringResource(R.string.ui_close))
                }
            }
        )
    }
}
