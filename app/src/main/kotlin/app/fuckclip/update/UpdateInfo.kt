package app.fuckclip.update

/**
 * Representation of a downloadable APK release asset from GitHub.
 *
 * 来自 GitHub Release 的可下载 APK 资产描述模型。
 */
data class ApkAsset(
    /**
     * File name of the APK asset.
     *
     * APK 资产的文件名称。
     */
    val name: String,

    /**
     * Direct browser download URL for the asset.
     *
     * 资产的直接下载链接 URL。
     */
    val url: String,

    /**
     * Total size in bytes of the asset, or 0 if unknown.
     *
     * 资产的总字节数大小，未知时为 0。
     */
    val size: Long,

    /**
     * Hexadecimal SHA-256 digest string for file verification, if provided.
     *
     * 用于文件完整性校验的十六进制 SHA-256 摘要（若有）。
     */
    val sha256: String?
)

/**
 * Release metadata summary retrieved from GitHub Releases API.
 *
 * 从 GitHub Releases API 检索到的最新版本发布元数据摘要。
 */
data class UpdateInfo(
    /**
     * Git release tag name (e.g., "v1.2.0").
     *
     * Git 版本标签名称（如 "v1.2.0"）。
     */
    val tag: String,

    /**
     * Release title header string.
     *
     * 版本发布标题。
     */
    val title: String,

    /**
     * Markdown release notes and change log description.
     *
     * Markdown 格式的更新日志与更新说明文本。
     */
    val notes: String,

    /**
     * Web page URL of the release on GitHub.
     *
     * 该版本在 GitHub 上的网页链接。
     */
    val pageUrl: String,

    /**
     * Compatible APK asset matching the target device architecture, if available.
     *
     * 匹配当前设备 CPU 架构的兼容 APK 资产（若存在）。
     */
    val apk: ApkAsset?
)
