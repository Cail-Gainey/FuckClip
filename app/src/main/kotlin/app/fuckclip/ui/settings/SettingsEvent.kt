package app.fuckclip.ui.settings

/**
 * Transient UI event emitted by [SettingsViewModel] for snackbars or toasts.
 *
 * 由 [SettingsViewModel] 发射的短暂 UI 事件，用于展示提示信息。
 */
sealed interface SettingsEvent {
    /**
     * Displays a text message identified by string resource.
     *
     * 展示由字符串资源指定的提示消息。
     */
    data class ShowMessage(
        /**
         * String resource ID for the message text.
         *
         * 消息文本的字符串资源 ID。
         */
        val messageRes: Int,

        /**
         * Optional format arguments for the string resource.
         *
         * 字符串资源的可选格式化参数。
         */
        val formatArgs: List<Any> = emptyList()
    ) : SettingsEvent
}
