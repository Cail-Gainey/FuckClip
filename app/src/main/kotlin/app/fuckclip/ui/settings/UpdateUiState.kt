package app.fuckclip.ui.settings

import app.fuckclip.update.ApkAsset
import app.fuckclip.update.UpdateInfo

/**
 * UI state representing the update checking and download lifecycle.
 *
 * 表示应用更新检测与下载全生命周期的界面状态。
 */
sealed interface UpdateUiState {
    /**
     * Idle state with no active update operations.
     *
     * 空闲状态，当前无活跃的更新操作。
     */
    object Idle : UpdateUiState

    /**
     * Actively querying remote release metadata.
     *
     * 正在联网查询最新发布元数据。
     */
    object Checking : UpdateUiState

    /**
     * A newer release has been found with release [info], indicating whether [isManual] triggered by the user.
     *
     * 检测到可用新版本 [info]，并标识 [isManual] 是否由用户手动触发。
     */
    data class UpdateAvailable(
        val info: UpdateInfo,
        val isManual: Boolean
    ) : UpdateUiState

    /**
     * An APK asset [apk] from release [info] is being downloaded with [bytesDownloaded] and [totalBytes].
     *
     * 正在从版本 [info] 下载 APK 资产包 [apk]，包含当前进度 [bytesDownloaded] 与总量 [totalBytes]。
     */
    data class Downloading(
        val info: UpdateInfo,
        val apk: ApkAsset,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : UpdateUiState

    /**
     * Update download or installation initiation failed for release [info] with error [message].
     *
     * 版本 [info] 的更新包下载或调起安装失败，包含错误说明 [message]。
     */
    data class DownloadFailed(
        val info: UpdateInfo,
        val message: String?
    ) : UpdateUiState
}
