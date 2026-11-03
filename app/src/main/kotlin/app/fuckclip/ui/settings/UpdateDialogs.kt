package app.fuckclip.ui.settings

import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.fuckclip.R
import app.fuckclip.update.UpdateInfo
import org.koin.androidx.compose.koinViewModel

/**
 * Top-level update dialog host rendering checking spinner, new version alert, download progress, and failure dialogs.
 *
 * 顶层更新弹窗宿主，负责统一渲染检测中、新版本提示、下载进度以及失败回退弹窗。
 */
@Composable
fun UpdateDialogHost(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val context = LocalContext.current
    val updateUiState by viewModel.updateUiState.collectAsStateWithLifecycle()

    // Observe one-off events
    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is SettingsEvent.ShowMessage -> {
                    val text = if (event.formatArgs.isEmpty()) {
                        context.getString(event.messageRes)
                    } else {
                        context.getString(event.messageRes, *event.formatArgs.toTypedArray())
                    }
                    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    when (val state = updateUiState) {
        is UpdateUiState.Idle -> {
            // No dialog visible
        }
        is UpdateUiState.Checking -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                title = { Text(stringResource(R.string.about_checking)) },
                text = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text(stringResource(R.string.about_update_cancel))
                    }
                }
            )
        }
        is UpdateUiState.UpdateAvailable -> {
            UpdateAvailableDialog(
                info = state.info,
                isManual = state.isManual,
                onUpdate = {
                    val apk = state.info.apk
                    if (apk != null) {
                        viewModel.startDownload(state.info, apk)
                    } else {
                        runCatching {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(state.info.pageUrl.ifBlank { viewModel.repoUrl })
                            )
                            context.startActivity(intent)
                        }
                        viewModel.dismissUpdateDialog()
                    }
                },
                onSkip = {
                    viewModel.skipVersion(state.info.tag)
                },
                onDismiss = { viewModel.dismissUpdateDialog() }
            )
        }
        is UpdateUiState.Downloading -> {
            DownloadProgressDialog(
                done = state.bytesDownloaded,
                total = state.totalBytes,
                onCancel = { viewModel.cancelDownload() }
            )
        }
        is UpdateUiState.DownloadFailed -> {
            AlertDialog(
                onDismissRequest = { viewModel.dismissUpdateDialog() },
                title = { Text(stringResource(R.string.update_download_failed)) },
                text = {
                    Text(
                        text = state.message ?: stringResource(R.string.update_download_failed_hint)
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.dismissUpdateDialog()
                            runCatching {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(state.info.pageUrl.ifBlank { viewModel.repoUrl })
                                )
                                context.startActivity(intent)
                            }
                        }
                    ) {
                        Text(stringResource(R.string.update_open_browser))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissUpdateDialog() }) {
                        Text(stringResource(R.string.ui_close))
                    }
                }
            )
        }
    }
}

/**
 * Dialog alerting the user to an available application update with release notes.
 *
 * 提示用户存在新可用更新并展示更新日志与版本详情的对话框。
 */
@Composable
internal fun UpdateAvailableDialog(
    info: UpdateInfo,
    isManual: Boolean,
    onUpdate: () -> Unit,
    onSkip: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val message = buildString {
        if (info.title.isNotEmpty() && info.title != info.tag) {
            append(info.title).append("\n\n")
        }
        append(info.notes.ifBlank { context.getString(R.string.update_notes_empty) })
        val apk = info.apk
        if (apk != null && apk.size > 0) {
            append("\n\n").append(
                context.getString(R.string.update_size, Formatter.formatFileSize(context, apk.size))
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_new_version, info.tag)) },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            TextButton(onClick = onUpdate) {
                Text(stringResource(R.string.about_update_now))
            }
        },
        dismissButton = {
            Row {
                if (!isManual) {
                    TextButton(onClick = onSkip) {
                        Text(stringResource(R.string.update_skip))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.about_update_cancel))
                }
            }
        }
    )
}

/**
 * Modal progress dialog displaying current APK download progress and byte metrics.
 *
 * 展示当前 APK 下载进度条与字节数统计的模态进度弹窗。
 */
@Composable
internal fun DownloadProgressDialog(
    done: Long,
    total: Long,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = { /* Modal while downloading */ },
        title = { Text(stringResource(R.string.update_downloading)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (total > 0) {
                    val progress = (done.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    val percent = (progress * 100).toInt()
                    Text(
                        text = stringResource(
                            R.string.update_progress,
                            Formatter.formatFileSize(context, done),
                            Formatter.formatFileSize(context, total),
                            percent
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(
                        text = Formatter.formatFileSize(context, done),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}
