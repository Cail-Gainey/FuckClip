package app.fuckclip.core.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Data container for exporting and importing whitelist configurations.
 *
 * 用于导出和导入白名单配置的数据容器。
 */
@Serializable
data class WhitelistBackup(
    /**
     * Backup schema format version.
     *
     * 备份文件架构的格式版本。
     */
    val version: Int = CURRENT_VERSION,

    /**
     * List of whitelist rules included in this backup.
     *
     * 该备份中包含的白名单规则列表。
     */
    val rules: List<WhitelistRule> = emptyList()
) {
    /**
     * Encodes this backup container into a JSON string formatted for export.
     *
     * 将此备份容器编码为适合导出的 JSON 字符串。
     */
    fun toJson(): String = jsonFormat.encodeToString(this)

    companion object {
        /**
         * Current schema version for whitelist backups.
         *
         * 白名单备份的当前架构版本。
         */
        const val CURRENT_VERSION: Int = 1

        private val jsonFormat = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }

        /**
         * Decodes a JSON string into a [WhitelistBackup] instance.
         *
         * 将 JSON 字符串解码为 [WhitelistBackup] 实例。
         */
        fun fromJson(json: String): WhitelistBackup = jsonFormat.decodeFromString(json)
    }
}
