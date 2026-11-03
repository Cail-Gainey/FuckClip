package app.fuckclip.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fuckclip.BuildConfig
import app.fuckclip.R
import app.fuckclip.data.SettingsRepository
import app.fuckclip.data.readBoundedText
import app.fuckclip.i18n.AppLanguage
import app.fuckclip.update.ApkAsset
import app.fuckclip.update.ApkInstaller
import app.fuckclip.update.UpdateChecker
import app.fuckclip.update.UpdateInfo
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel managing overall application settings, protection configurations, data cleanup, and updates.
 *
 * 管理应用全局设置、防护参数配置、数据清理以及更新流程的 ViewModel。
 */
class SettingsViewModel(
    private val context: Context,
    private val repository: SettingsRepository
) : ViewModel() {

    /**
     * Application release version name string.
     *
     * 应用发布版本名称字符串。
     */
    val versionName: String

    /**
     * Application internal version code number.
     *
     * 应用内部构建版本代码编号。
     */
    val versionCode: Long

    /**
     * Official GitHub repository URL.
     *
     * 官方 GitHub 开源仓库 URL。
     */
    val repoUrl: String = UpdateChecker.REPO_URL

    /**
     * Open-source license agreement URL.
     *
     * 开源许可证协议页面 URL。
     */
    val licenseUrl: String = "${UpdateChecker.REPO_URL}/blob/master/LICENSE"

    // Settings observable streams

    /**
     * Observable stream representing the global protection master toggle.
     *
     * 代表全局防护总开关的可观察流。
     */
    val masterEnabled: StateFlow<Boolean> = repository.masterEnabled

    /**
     * Observable stream indicating whether rogue application frequent read throttling is enabled.
     *
     * 标识是否启用毒瘤应用频繁读取限流与防轰炸保护的可观察流。
     */
    val rogueThrottleEnabled: StateFlow<Boolean> = repository.rogueThrottleEnabled

    /**
     * Observable stream of prompt denial cooldown duration in seconds.
     *
     * 授权弹窗被拒绝或关闭后的静默冷却时长（秒）可观察流。
     */
    val denyCooldownSeconds: StateFlow<Int> = repository.denyCooldownSeconds

    /**
     * Observable stream of burst allowance window duration in seconds following a one-time grant.
     *
     * 单次允许后快速连读免弹窗通行窗口时长（秒）可观察流。
     */
    val grantBurstSeconds: StateFlow<Int> = repository.grantBurstSeconds

    /**
     * Observable stream of maximum allowed read attempts per sliding window.
     *
     * 滑动时间窗口内触发限流截断的最大读取尝试次数阈值可观察流。
     */
    val maxReadFrequency: StateFlow<Int> = repository.maxReadFrequency

    /**
     * Observable stream of reads tolerated within a short window before silent throttling kicks in.
     *
     * 短时间内允许连续放行、超出后才触发静默限流的读取次数上限可观察流。
     */
    val readToleranceCount: StateFlow<Int> = repository.readToleranceCount

    /**
     * Observable stream indicating whether system applications should be shown in app lists.
     *
     * 标识应用列表中是否显示系统组件/应用的可观察流。
     */
    val showSystemApps: StateFlow<Boolean> = repository.showSystemApps

    /**
     * Observable stream indicating whether automatic update checking on app launch is enabled.
     *
     * 标识应用启动时是否自动检查更新的可观察流。
     */
    val isAutoUpdateCheckEnabled: StateFlow<Boolean> = repository.isAutoUpdateCheckEnabled

    // Language selection
    private val _selectedLanguageTag = MutableStateFlow("")

    /**
     * Observable stream representing the user-selected language BCP 47 tag.
     *
     * 代表用户所选语言 BCP 47 标签的可观察流。
     */
    val selectedLanguageTag: StateFlow<String> = _selectedLanguageTag.asStateFlow()

    /**
     * List of supported language tags paired with their native display names.
     *
     * 受支持语言标签及其本地化原生显示名称的键值对列表。
     */
    val supportedLanguages: List<Pair<String, String>>

    // Update state machine
    private val _updateUiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)

    /**
     * Observable stream representing the current update checking and download lifecycle state.
     *
     * 代表当前更新检测与下载全生命周期状态的可观察流。
     */
    val updateUiState: StateFlow<UpdateUiState> = _updateUiState.asStateFlow()

    private var downloadJob: Job? = null

    // Event channel
    private val _eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)

    /**
     * Stream of one-off UI events such as toasts and messages.
     *
     * 一次性短暂 UI 提示事件流。
     */
    val eventFlow = _eventChannel.receiveAsFlow()

    init {
        val packageInfo = try {
            context.packageManager?.getPackageInfo(context.packageName, 0)
        } catch (_: Throwable) {
            null
        }
        versionName = packageInfo?.versionName ?: BuildConfig.VERSION_NAME
        versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo?.longVersionCode ?: BuildConfig.VERSION_CODE.toLong()
        } else {
            @Suppress("DEPRECATION")
            packageInfo?.versionCode?.toLong() ?: BuildConfig.VERSION_CODE.toLong()
        }

        val followSystemLabel = runCatching {
            context.getString(R.string.language_follow_system)
        }.getOrDefault("Follow system")

        supportedLanguages = listOf("" to followSystemLabel) + (runCatching {
            AppLanguage.supportedTags(context).map { tag ->
                tag to AppLanguage.nativeName(tag)
            }
        }.getOrNull() ?: emptyList())

        val savedTag = repository.selectedLanguageTag.value
        if (savedTag.isNotEmpty()) {
            _selectedLanguageTag.value = savedTag
        } else {
            val currentLocales = runCatching { AppLanguage.selectedLocales(context) }.getOrNull()
            _selectedLanguageTag.value = if (currentLocales == null || currentLocales.isEmpty) "" else currentLocales[0]?.toLanguageTag() ?: ""
        }
    }

    /**
     * Checks for updates silently on app launch if enabled and 24 hours have elapsed.
     *
     * 应用启动时静默检查更新（若开关打开且距上次检查已超过 24 小时）。
     */
    fun checkOnLaunch() {
        if (repository.isAutoUpdateCheckEnabled.value && repository.isUpdateCheckDue()) {
            checkForUpdates(manual = false)
        }
    }

    /**
     * Triggers checking for updates against GitHub Releases.
     *
     * 触发针对 GitHub Releases 的更新检测。
     *
     * @param manual Whether this check was requested directly by the user.
     */
    fun checkForUpdates(manual: Boolean = true) {
        if (_updateUiState.value is UpdateUiState.Checking || _updateUiState.value is UpdateUiState.Downloading) {
            return
        }

        if (manual) {
            _updateUiState.value = UpdateUiState.Checking
        }

        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { UpdateChecker.fetchLatest() }
            }

            result.onSuccess { info ->
                repository.lastUpdateCheck = System.currentTimeMillis()
                val newer = UpdateChecker.isNewerVersion(info.tag, versionName)
                if (newer) {
                    if (manual || repository.ignoredVersion != info.tag) {
                        _updateUiState.value = UpdateUiState.UpdateAvailable(info, isManual = manual)
                    } else {
                        _updateUiState.value = UpdateUiState.Idle
                    }
                } else {
                    _updateUiState.value = UpdateUiState.Idle
                    if (manual) {
                        _eventChannel.send(
                            SettingsEvent.ShowMessage(R.string.about_latest, listOf(versionName))
                        )
                    }
                }
            }.onFailure { error ->
                _updateUiState.value = UpdateUiState.Idle
                if (manual) {
                    _eventChannel.send(SettingsEvent.ShowMessage(R.string.about_check_failed))
                }
            }
        }
    }

    /**
     * Starts downloading the selected APK asset with throttled progress reporting.
     *
     * 开始下载选定的 APK 资产包，并进行限频进度回报。
     *
     * @param info Metadata of the release.
     * @param apk Target APK asset to download.
     */
    fun startDownload(info: UpdateInfo, apk: ApkAsset) {
        downloadJob?.cancel()
        _updateUiState.value = UpdateUiState.Downloading(info, apk, 0L, apk.size)

        downloadJob = viewModelScope.launch {
            var lastPost = 0L
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    ApkInstaller.download(context, apk, isCancelled = { !isActive }) { done, total ->
                        val now = System.currentTimeMillis()
                        if (now - lastPost >= 100 || done == total) {
                            lastPost = now
                            _updateUiState.value = UpdateUiState.Downloading(info, apk, done, total)
                        }
                    }
                }
            }

            result.onSuccess { file ->
                val installResult = withContext(Dispatchers.Main) {
                    ApkInstaller.install(context, file)
                }
                _updateUiState.value = UpdateUiState.Idle
                when (installResult) {
                    ApkInstaller.InstallResult.STARTED -> Unit
                    ApkInstaller.InstallResult.NEED_PERMISSION -> {
                        _eventChannel.send(SettingsEvent.ShowMessage(R.string.update_need_install_permission))
                    }
                    ApkInstaller.InstallResult.FAILED -> {
                        _eventChannel.send(SettingsEvent.ShowMessage(R.string.update_install_failed))
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException) {
                    _updateUiState.value = UpdateUiState.Idle
                    return@onFailure
                }
                _updateUiState.value = UpdateUiState.DownloadFailed(info, error.localizedMessage)
            }
        }
    }

    /**
     * Cancels an ongoing APK download and resets state to idle.
     *
     * 取消进行中的 APK 下载并将状态重置为空闲。
     */
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        _updateUiState.value = UpdateUiState.Idle
    }

    /**
     * Skips the specified version tag for future automatic check alerts.
     *
     * 跳过指定的版本 Tag，未来的自动检查将不再为该版本弹窗。
     */
    fun skipVersion(tag: String) {
        repository.ignoredVersion = tag
        _updateUiState.value = UpdateUiState.Idle
    }

    /**
     * Dismisses the active update dialog.
     *
     * 关闭当前显示的更新相关弹窗。
     */
    fun dismissUpdateDialog() {
        _updateUiState.value = UpdateUiState.Idle
    }

    /**
     * Toggles whether the master clipboard protection interception is active.
     *
     * 切换全局剪贴板防护总开关。
     */
    fun setMasterEnabled(enabled: Boolean) {
        repository.setMasterEnabled(enabled)
    }

    /**
     * Toggles whether rogue application frequent read throttling is active.
     *
     * 切换毒瘤应用频繁读取限流与防轰炸保护开关。
     */
    fun setRogueThrottleEnabled(enabled: Boolean) {
        repository.setRogueThrottleEnabled(enabled)
    }

    /**
     * Updates the deny cooldown seconds.
     *
     * 更新拒绝弹窗后的静默冷却秒数。
     */
    fun setDenyCooldownSeconds(seconds: Int) {
        repository.setDenyCooldownSeconds(seconds)
    }

    /**
     * Updates the grant burst window duration in seconds.
     *
     * 更新单次允许后的快速连读免弹窗通行窗口秒数。
     */
    fun setGrantBurstSeconds(seconds: Int) {
        repository.setGrantBurstSeconds(seconds)
    }

    /**
     * Updates the maximum read frequency threshold.
     *
     * 更新高频读取限流阈值次数。
     */
    fun setMaxReadFrequency(frequency: Int) {
        repository.setMaxReadFrequency(frequency)
    }

    /**
     * Updates how many reads within a short window are tolerated before silent throttling applies.
     *
     * 更新短时间内允许连续放行的读取次数上限（超出后才触发静默限流）。
     */
    fun setReadToleranceCount(count: Int) {
        repository.setReadToleranceCount(count)
    }

    /**
     * Toggles whether system apps should be listed in application whitelist views.
     *
     * 切换应用列表是否包含系统组件。
     */
    fun setShowSystemApps(show: Boolean) {
        repository.setShowSystemApps(show)
    }

    /**
     * Toggles whether updates should be checked automatically on application launch.
     *
     * 切换启动时是否自动检查更新。
     */
    fun setAutoUpdateCheckEnabled(enabled: Boolean) {
        repository.setAutoUpdateCheckEnabled(enabled)
    }

    /**
     * Clears all activity records.
     *
     * 清空全部剪贴板拦截与访问历史记录。
     */
    fun clearHistory() {
        repository.clearHookLogs()
    }

    /**
     * Switches the application locale to [tag].
     *
     * 将应用语言切换为指定的 [tag]。
     */
    fun selectLanguage(tag: String) {
        _selectedLanguageTag.value = tag
        repository.setSelectedLanguageTag(tag)
        AppLanguage.select(tag)
    }

    /**
     * Exports the whitelist backup as a JSON string.
     *
     * 将白名单备份导出为 JSON 字符串。
     */
    fun exportRules(): String = repository.exportWhitelist()

    /**
     * Imports whitelist rules from [jsonString], returning the number of rules imported.
     *
     * 从 [jsonString] 中导入白名单规则，并返回成功导入的规则总数。
     */
    fun importRules(jsonString: String): Result<Int> = repository.importWhitelist(jsonString)

    /**
     * Exports the whitelist backup JSON string to the file specified by [uri].
     *
     * 将白名单规则备份 JSON 字符串导出写入至 [uri] 指定的外部存储文件中。
     */
    fun exportRulesToFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = runCatching {
                val json = repository.exportWhitelist()
                context.contentResolver?.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(json.toByteArray(Charsets.UTF_8))
                    outputStream.flush()
                } ?: throw IOException("Unable to open output stream for URI: $uri")
                true
            }.isSuccess

            if (success) {
                _eventChannel.send(SettingsEvent.ShowMessage(R.string.settings_export_rules_success))
            } else {
                _eventChannel.send(SettingsEvent.ShowMessage(R.string.settings_export_rules_failed))
            }
        }
    }

    /**
     * Imports whitelist backup rules from the file specified by [uri] and hot-syncs them immediately.
     *
     * 从 [uri] 指定的外部 JSON 备份文件中读取并恢复白名单规则，并立即热同步生效。
     */
    fun importRulesFromFile(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                val content = context.contentResolver?.openInputStream(uri)?.use { inputStream ->
                    readBoundedText(inputStream)
                } ?: throw IOException("Unable to open input stream for URI: $uri")
                repository.importWhitelist(content)
            }.getOrElse { Result.failure(it) }

            result.fold(
                onSuccess = { count ->
                    _eventChannel.send(
                        SettingsEvent.ShowMessage(
                            R.string.whitelist_import_success,
                            listOf(count)
                        )
                    )
                },
                onFailure = {
                    _eventChannel.send(SettingsEvent.ShowMessage(R.string.whitelist_import_failed))
                }
            )
        }
    }
}
