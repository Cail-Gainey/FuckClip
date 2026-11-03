package app.fuckclip.data

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages application update configuration, version skipping, and system app listing toggles.
 *
 * 管理应用更新检查配置、版本跳过以及系统应用显示开关。
 */
internal class UpdateSettingsStore(
    private val getPreferences: () -> SharedPreferences
) {
    private val _isAutoUpdateCheckEnabled = MutableStateFlow(true)

    /**
     * Observable stream indicating whether automatic update checking on app launch is enabled.
     *
     * 标识应用启动时是否自动检查更新的可观察流。
     */
    val isAutoUpdateCheckEnabled: StateFlow<Boolean> = _isAutoUpdateCheckEnabled.asStateFlow()

    private val _showSystemApps = MutableStateFlow(false)

    /**
     * Observable stream indicating whether system applications should be shown in app lists.
     *
     * 标识应用列表中是否显示系统组件/应用的可观察流。
     */
    val showSystemApps: StateFlow<Boolean> = _showSystemApps.asStateFlow()

    /**
     * Timestamp in milliseconds when the last update check completed successfully.
     *
     * 上次成功完成更新检查的时间戳（毫秒）。
     */
    var lastUpdateCheck: Long
        get() = getPreferences().getLong(KEY_LAST_UPDATE_CHECK, 0L)
        set(value) = getPreferences().edit().putLong(KEY_LAST_UPDATE_CHECK, value).apply()

    /**
     * Release tag skipped by the user for automatic update alerts.
     *
     * 用户选择跳过的版本 Tag，启动自动检查将不再为该版本弹窗。
     */
    var ignoredVersion: String?
        get() = getPreferences().getString(KEY_IGNORED_VERSION, null)
        set(value) = getPreferences().edit().putString(KEY_IGNORED_VERSION, value).apply()

    /**
     * Loads initial update preferences from local storage.
     *
     * 从本地偏好配置中加载初始更新设置。
     */
    fun loadInitial(prefs: SharedPreferences) {
        _isAutoUpdateCheckEnabled.value = prefs.getBoolean(KEY_AUTO_UPDATE_CHECK, true)
        _showSystemApps.value = prefs.getBoolean(KEY_SHOW_SYSTEM_APPS, false)
    }

    /**
     * Synchronizes update preferences with remote configuration.
     *
     * 与远程偏好配置同步更新设置。
     */
    fun sync(remotePrefs: SharedPreferences) {
        _isAutoUpdateCheckEnabled.value = remotePrefs.getBoolean(KEY_AUTO_UPDATE_CHECK, _isAutoUpdateCheckEnabled.value)
        _showSystemApps.value = remotePrefs.getBoolean(KEY_SHOW_SYSTEM_APPS, _showSystemApps.value)
    }

    /**
     * Checks whether a new automatic update check is due based on the daily interval.
     *
     * 根据每日（24 小时）间隔判断当前是否应触发启动自动更新检查。
     */
    fun isUpdateCheckDue(now: Long = System.currentTimeMillis()): Boolean =
        now - lastUpdateCheck >= UPDATE_CHECK_INTERVAL_MS

    /**
     * Toggles whether updates should be checked automatically on launch.
     *
     * 切换启动时是否自动检查更新并持久化。
     */
    fun setAutoUpdateCheckEnabled(enabled: Boolean) {
        _isAutoUpdateCheckEnabled.value = enabled
        getPreferences().edit().putBoolean(KEY_AUTO_UPDATE_CHECK, enabled).apply()
    }

    /**
     * Toggles whether system apps should be listed in whitelist views.
     *
     * 切换白名单列表是否显示系统组件并持久化。
     */
    fun setShowSystemApps(show: Boolean) {
        _showSystemApps.value = show
        getPreferences().edit().putBoolean(KEY_SHOW_SYSTEM_APPS, show).apply()
    }

    companion object {
        /**
         * Preference key for automatic update checking toggle.
         *
         * 启动时自动检查更新开关的配置键。
         */
        const val KEY_AUTO_UPDATE_CHECK = "auto_update_check"

        /**
         * Preference key for timestamp of last successful update check.
         *
         * 上次成功完成更新检查时间戳的配置键。
         */
        const val KEY_LAST_UPDATE_CHECK = "last_update_check"

        /**
         * Preference key for user-ignored release version tag.
         *
         * 用户选择跳过的版本 Tag 的配置键。
         */
        const val KEY_IGNORED_VERSION = "ignored_version"

        /**
         * Preference key for whether system applications are shown in app lists.
         *
         * 应用列表中是否显示系统组件的配置键。
         */
        const val KEY_SHOW_SYSTEM_APPS = "show_system_apps"

        /**
         * Interval in milliseconds between automatic update checks (24 hours).
         *
         * 自动更新检查的时间间隔（毫秒，默认 24 小时）。
         */
        const val UPDATE_CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000
    }
}
