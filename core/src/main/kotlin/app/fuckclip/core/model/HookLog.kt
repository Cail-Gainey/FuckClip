package app.fuckclip.core.model

import kotlinx.serialization.Serializable

/**
 * Data structure representing a clipboard hook interception event.
 *
 * 剪贴板钩子拦截事件的数据结构。
 */
@Serializable
data class HookLog(
    /**
     * Unique identifier for this log event.
     *
     * 该日志事件的唯一标识。
     */
    val id: String,

    /**
     * Package name of the calling application.
     *
     * 调用方应用的包名。
     */
    val packageName: String,

    /**
     * Action type of the operation such as READ or WRITE.
     *
     * 操作的动作类型，如 READ 或 WRITE。
     */
    val action: String,

    /**
     * Whether the clipboard access was allowed.
     *
     * 剪贴板访问是否被放行。
     */
    val allowed: Boolean,

    /**
     * Additional details or reason for the decision.
     *
     * 决策的附加细节或原因说明。
     */
    val detail: String = "",

    /**
     * Timestamp in milliseconds when the event occurred.
     *
     * 事件发生的毫秒时间戳。
     */
    val timestamp: Long = System.currentTimeMillis(),

    /**
     * Clipboard text content involved in the operation, if any.
     *
     * 本次操作涉及的剪贴板文本内容（若有）。
     */
    val content: String? = null,

    /**
     * Human-readable label or name of the application at log time.
     *
     * 记录时该应用的可读显示名称或标签。
     */
    val appName: String? = null,

    /**
     * Number of clipboard events aggregated into this record within a short burst.
     *
     * 短时间内被聚合进该条记录的剪贴板事件次数。
     */
    val count: Int = 1
)
