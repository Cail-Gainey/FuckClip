package app.fuckclip.ui.whitelist

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.fuckclip.core.model.WhitelistRule
import app.fuckclip.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Filter mode options for displaying application rules.
 *
 * 应用规则列表的过滤模式选项。
 */
enum class RuleFilterMode {
    /**
     * Show all applications without filtering by rule type.
     *
     * 显示所有应用，不按规则类型过滤。
     */
    ALL,

    /**
     * Show only applications that have customized permission rules.
     *
     * 仅显示已配置自定义权限规则的应用。
     */
    CONFIGURED,

    /**
     * Show applications with allow permission rules.
     *
     * 仅显示配置了允许放行规则的应用。
     */
    ALLOW,

    /**
     * Show applications with deny permission rules.
     *
     * 仅显示配置了拒绝拦截规则的应用。
     */
    DENY
}

/**
 * Presentation item representing an installed application and its clipboard permissions.
 *
 * 展示已安装应用程序及其剪贴板权限状态的数据项。
 */
data class AppItem(
    /**
     * Package name of the application.
     *
     * 应用的软件包名。
     */
    val packageName: String,

    /**
     * Human-readable display label of the application.
     *
     * 应用的可读显示名称。
     */
    val label: String,

    /**
     * Pre-computed lowercase search key combining the display label and package name.
     *
     * 预先计算的小写检索键，由显示名称与包名组合而成。
     */
    val searchIndex: String,

    /**
     * Whether this application is a system component or pre-installed app.
     *
     * 该应用是否属于系统组件或预装应用。
     */
    val isSystem: Boolean,

    /**
     * Active permission rule for clipboard read operations.
     *
     * 剪贴板读取操作的当前生效规则。
     */
    val readRule: Int = WhitelistRule.RULE_REQUEST,

    /**
     * Active permission rule for clipboard write operations.
     *
     * 剪贴板写入操作的当前生效规则。
     */
    val writeRule: Int = WhitelistRule.RULE_REQUEST,

    /**
     * Whether custom permission rules have been saved for this application.
     *
     * 该应用是否已保存自定义配置规则。
     */
    val isConfigured: Boolean = false
) {
    /**
     * Whether the application is permitted to read clipboard without prompting.
     *
     * 该应用是否已被加入允许直接读取的白名单。
     */
    val isWhitelisted: Boolean
        get() = readRule == WhitelistRule.RULE_ALLOW
}

/**
 * ViewModel managing installed app listing, rule configurations, and search filtering.
 *
 * 管理已安装应用列表、规则配置以及搜索过滤操作的 ViewModel。
 */
class WhitelistViewModel(
    private val context: Context,
    private val repository: SettingsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    /**
     * Observable stream representing the current search query string.
     *
     * 代表当前搜索关键字字符串的可观察流。
     */
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /**
     * Observable stream indicating whether system applications should be displayed.
     *
     * 标识是否显示系统应用的可观察流。
     */
    val showSystemApps: StateFlow<Boolean> = repository.showSystemApps

    private val _filterMode = MutableStateFlow(RuleFilterMode.ALL)

    /**
     * Observable stream representing the active [RuleFilterMode].
     *
     * 代表当前生效 [RuleFilterMode] 过滤模式的可观察流。
     */
    val filterMode: StateFlow<RuleFilterMode> = _filterMode.asStateFlow()

    private val _installedApps = MutableStateFlow<List<AppItem>>(emptyList())
    private val _isLoading = MutableStateFlow(true)

    /**
     * Observable stream indicating whether installed applications are actively being loaded.
     *
     * 标识是否正在加载已安装应用列表的可观察流。
     */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoading.value = true
            val apps = withContext(Dispatchers.IO) {
                val pm = context.packageManager
                val list = pm.getInstalledApplications(0)
                list.map { info ->
                    val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val label = try {
                        info.loadLabel(pm).toString()
                    } catch (_: Throwable) {
                        info.packageName
                    }
                    AppItem(
                        packageName = info.packageName,
                        label = label,
                        searchIndex = "${label.lowercase()} ${info.packageName.lowercase()}",
                        isSystem = isSystem
                    )
                }.sortedBy { it.searchIndex }
            }
            _installedApps.value = apps
            _isLoading.value = false
        }
    }

    /**
     * Filtered list of applications matching current search query, filter mode, and system app visibility.
     *
     * 匹配当前搜索关键字、过滤模式与系统应用显示偏好的过滤应用列表。
     *
     * Rule merging, filtering and sorting run on a background dispatcher with a pre-computed search
     * key per row, so typing in the search field never re-lowercases the whole installed app list on
     * the main thread.
     *
     * 规则合并、过滤与排序均在后台调度器上执行，且每行都带有预计算的检索键，
     * 因此在搜索框输入时不会在主线程上对整个应用列表反复执行小写转换。
     */
    val displayedApps: StateFlow<List<AppItem>> = combine(
        _installedApps,
        repository.whitelist,
        _searchQuery,
        repository.showSystemApps,
        _filterMode
    ) { apps, whitelist, query, showSystem, filter ->
        val ruleMap = whitelist.associateBy { it.packageName }
        val trimmedQuery = query.trim().lowercase()

        apps.asSequence()
            .map { item ->
                val rule = ruleMap[item.packageName]
                if (rule != null) {
                    item.copy(
                        readRule = rule.readRule,
                        writeRule = rule.writeRule,
                        isConfigured = true
                    )
                } else {
                    item.copy(
                        readRule = WhitelistRule.RULE_REQUEST,
                        writeRule = WhitelistRule.RULE_REQUEST,
                        isConfigured = false
                    )
                }
            }
            .filter { item ->
                if (!showSystem && item.isSystem && !item.isConfigured) {
                    return@filter false
                }
                when (filter) {
                    RuleFilterMode.ALL -> true
                    RuleFilterMode.CONFIGURED -> item.isConfigured
                    RuleFilterMode.ALLOW -> item.readRule == WhitelistRule.RULE_ALLOW || item.writeRule == WhitelistRule.RULE_ALLOW
                    RuleFilterMode.DENY -> item.readRule == WhitelistRule.RULE_DENY || item.writeRule == WhitelistRule.RULE_DENY
                }
            }
            .filter { item -> trimmedQuery.isEmpty() || item.searchIndex.contains(trimmedQuery) }
            .sortedWith(
                compareByDescending<AppItem> { it.isConfigured }
                    .thenBy { it.searchIndex }
            )
            .toList()
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Updates the search filtering query string.
     *
     * 更新搜索过滤关键字字符串。
     */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /**
     * Toggles whether built-in system applications are visible in the list.
     *
     * 切换列表中是否显示内置系统应用。
     */
    fun toggleShowSystemApps() {
        repository.setShowSystemApps(!showSystemApps.value)
    }

    /**
     * Updates the current rule filter mode.
     *
     * 更新当前规则列表过滤模式。
     */
    fun setFilterMode(mode: RuleFilterMode) {
        _filterMode.value = mode
    }

    /**
     * Configures the permission rules for the specified [packageName].
     *
     * 为指定 [packageName] 配置读取与写入权限规则。
     */
    fun setPackageRule(packageName: String, readRule: Int, writeRule: Int) {
        repository.setPackageRule(packageName, readRule, writeRule)
    }

    /**
     * Resets the permission rule for [packageName] to default.
     *
     * 将指定 [packageName] 的权限规则重置为默认值。
     */
    fun resetPackageRule(packageName: String) {
        repository.removePackageRule(packageName)
    }

    /**
     * Toggles the whitelisted status for the specified [packageName].
     *
     * 切换指定 [packageName] 的白名单放行状态。
     */
    fun togglePackage(packageName: String) {
        repository.togglePackage(packageName)
    }

    /**
     * Exports the whitelist backup as a JSON string.
     *
     * 将白名单备份导出为 JSON 字符串。
     */
    fun exportBackup(): String = repository.exportWhitelist()

    /**
     * Imports whitelist backup from [json], returning count of rules imported.
     *
     * 从指定的 [json] 导入白名单备份，并返回成功导入的规则总数。
     */
    fun importBackup(json: String): Result<Int> = repository.importWhitelist(json)
}
