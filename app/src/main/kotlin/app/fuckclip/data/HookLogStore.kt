package app.fuckclip.data

import app.fuckclip.core.model.HookLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val MAX_LOG_RECORDS = 100

/** Time window in milliseconds within which repeated events of one app collapse into a single record. */
private const val MERGE_WINDOW_MS = 5_000L

/**
 * In-memory buffer storing recent clipboard hook interception logs.
 *
 * 维护内存中最近剪贴板拦截日志环形缓冲的存储类。
 */
internal class HookLogStore {
    private val _hookLogs = MutableStateFlow<List<HookLog>>(emptyList())

    /**
     * Observable stream of recent hook interception events.
     *
     * 最近发生的 Hook 拦截事件列表可观察流。
     */
    val hookLogs: StateFlow<List<HookLog>> = _hookLogs.asStateFlow()

    /**
     * Records a new hook interception [log] into the recent log list.
     *
     * 将新的 Hook 拦截 [log] 记录存入最近日志列表中。
     */
    fun recordHookLog(log: HookLog) {
        val current = _hookLogs.value.toMutableList()
        val head = current.firstOrNull()
        if (head != null && head.mergesWith(log)) {
            current[0] = head.copy(
                count = head.count + log.count,
                detail = log.detail,
                timestamp = log.timestamp,
                content = log.content ?: head.content
            )
            _hookLogs.value = current
            return
        }
        current.add(0, log)
        if (current.size > MAX_LOG_RECORDS) {
            current.removeAt(current.lastIndex)
        }
        _hookLogs.value = current
    }

    /**
     * Whether [other] belongs to the same short burst as this record and can be aggregated into it.
     *
     * 判断 [other] 是否属于本条记录的同一短时突发，从而可被聚合进同一张卡片。
     */
    private fun HookLog.mergesWith(other: HookLog): Boolean =
        packageName == other.packageName &&
            action.equals(other.action, ignoreCase = true) &&
            allowed == other.allowed &&
            other.timestamp - timestamp <= MERGE_WINDOW_MS

    /**
     * Clears all recorded hook interception logs.
     *
     * 清空所有已记录的 Hook 拦截日志。
     */
    fun clearHookLogs() {
        _hookLogs.value = emptyList()
    }
}
