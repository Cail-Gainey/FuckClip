package app.fuckclip.ui.home

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fuckclip.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.io.BufferedReader
import java.io.FileReader
import java.util.Locale

/**
 * Data model holding device hardware specification and Android runtime environment information.
 *
 * 承载设备硬件规格与 Android 运行时环境信息的数据模型。
 */
data class DeviceInfo(
    /**
     * Device manufacturer, commercial model name, and board code name.
     *
     * 设备制造商、商业机型名称及主板代号。
     */
    val model: String,

    /**
     * Android operating system release version and API SDK level.
     *
     * Android 操作系统发行版本号与 API SDK 级别。
     */
    val androidVersion: String,

    /**
     * Linux kernel version string read from system environment.
     *
     * 从系统环境读取的 Linux 内核版本字符串。
     */
    val kernelVersion: String,

    /**
     * Primary CPU architecture ABI string supported by the device.
     *
     * 设备支持的首选 CPU 架构指令集 ABI 字符串。
     */
    val abi: String,

    /**
     * System build display identifier or build ID.
     *
     * 系统构建显示标识符或内部构建号。
     */
    val buildDisplay: String
)

/**
 * ViewModel managing dashboard statistics, activation status, master toggle, and device environment details.
 *
 * 管理首页仪表盘统计指标、激活状态、总开关以及设备环境详情的 ViewModel。
 */
class HomeViewModel(
    private val repository: SettingsRepository
) : ViewModel() {

    /**
     * Module activation state observed from the repository.
     *
     * 从仓库中观察到的模块激活状态。
     */
    val isActivated: StateFlow<Boolean> = repository.isActivated

    /**
     * Formatted description of the active Xposed framework name and version.
     *
     * 格式化后的激活 Xposed 框架名称与版本描述。
     */
    val frameworkDescription: StateFlow<String> = combine(
        repository.isActivated,
        repository.frameworkName,
        repository.frameworkVersion
    ) { activated, name, version ->
        if (activated && !name.isNullOrBlank()) {
            "$name ${version.orEmpty()}".trim()
        } else if (activated) {
            "Active"
        } else {
            "Not Activated"
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Checking…")

    /**
     * Master toggle indicating whether clipboard protection is enabled.
     *
     * 标识剪贴板放行保护功能是否启用的全局总开关。
     */
    val masterEnabled: StateFlow<Boolean> = repository.masterEnabled

    /**
     * Total number of allowed applications in the whitelist.
     *
     * 白名单中已放行应用的总数量。
     */
    val whitelistCount: StateFlow<Int> = repository.whitelist
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Total number of recent hook interception events recorded.
     *
     * 已记录的最近 Hook 拦截事件总数量。
     */
    val logCount: StateFlow<Int> = repository.hookLogs
        .map { logs -> logs.sumOf { it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Total number of blocked (denied) interception events recorded.
     *
     * 已记录的被拒绝（拦截）事件总数量。
     */
    val deniedCount: StateFlow<Int> = repository.hookLogs
        .map { logs -> logs.filter { !it.allowed }.sumOf { it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Total number of permitted (granted) interception events recorded.
     *
     * 已记录的被允许（放行）事件总数量。
     */
    val grantedCount: StateFlow<Int> = repository.hookLogs
        .map { logs -> logs.filter { it.allowed }.sumOf { it.count } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Observable stream providing cached device hardware and kernel version details.
     *
     * 提供已缓存的设备硬件规格与内核版本详情的可观察流。
     */
    val deviceInfo: StateFlow<DeviceInfo> = flow {
        emit(loadDeviceInfo())
    }.flowOn(Dispatchers.IO).stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        DeviceInfo(
            model = "Loading…",
            androidVersion = "Android",
            kernelVersion = "Loading…",
            abi = "Unknown",
            buildDisplay = "Unknown"
        )
    )

    /**
     * Updates the global master toggle state.
     *
     * 更新全局总开关状态。
     */
    fun setMasterEnabled(enabled: Boolean) {
        repository.setMasterEnabled(enabled)
    }

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
     * Updates whether rogue application frequent read throttling is enabled.
     *
     * 更新是否启用毒瘤应用频繁读取限流保护状态。
     */
    fun setRogueThrottleEnabled(enabled: Boolean) {
        repository.setRogueThrottleEnabled(enabled)
    }

    /**
     * Updates the prompt denial cooldown duration in seconds.
     *
     * 更新授权弹窗被拒绝后的静默冷却时长（秒）。
     */
    fun setDenyCooldownSeconds(seconds: Int) {
        repository.setDenyCooldownSeconds(seconds)
    }

    /**
     * Updates the one-time grant burst window duration in seconds.
     *
     * 更新单次允许后的临时有效快速放行窗口时长（秒）。
     */
    fun setGrantBurstSeconds(seconds: Int) {
        repository.setGrantBurstSeconds(seconds)
    }

    /**
     * Updates the maximum allowed read frequency attempts per sliding window.
     *
     * 更新滑动时间窗口内触发限流截断的最大读取尝试次数。
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

    private fun loadDeviceInfo(): DeviceInfo {
        val manufacturer = try { Build.MANUFACTURER } catch (_: Throwable) { null }
        val model = try { Build.MODEL } catch (_: Throwable) { null }
        val device = try { Build.DEVICE } catch (_: Throwable) { null }
        var deviceModel = if (model != null && manufacturer != null &&
            model.lowercase(Locale.getDefault()).startsWith(manufacturer.lowercase(Locale.getDefault()))
        ) {
            model
        } else {
            "${manufacturer ?: ""} ${model ?: ""}"
        }.trim()
        if (!device.isNullOrEmpty()) {
            deviceModel += " ($device)"
        }

        val kernel = readKernelVersion()
        val release = try { Build.VERSION.RELEASE } catch (_: Throwable) { null } ?: ""
        val sdk = try { Build.VERSION.SDK_INT } catch (_: Throwable) { 0 }
        val abis = try { Build.SUPPORTED_ABIS } catch (_: Throwable) { null }
        val display = try { Build.DISPLAY ?: Build.ID } catch (_: Throwable) { null } ?: "Unknown"

        return DeviceInfo(
            model = deviceModel.ifBlank { "Android Device" },
            androidVersion = if (release.isNotBlank()) "Android $release (API $sdk)" else "Android (API $sdk)",
            kernelVersion = kernel,
            abi = abis?.firstOrNull() ?: "Unknown",
            buildDisplay = display
        )
    }

    private fun readKernelVersion(): String {
        try {
            BufferedReader(FileReader("/proc/version")).use { reader ->
                val line = reader.readLine()
                if (!line.isNullOrBlank()) {
                    return line.trim()
                }
            }
        } catch (_: Throwable) {}
        return System.getProperty("os.version") ?: "Unknown"
    }
}
