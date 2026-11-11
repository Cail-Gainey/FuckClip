package app.fuckclip.hook

/**
 * Categories of clipboard service entry points recognized by the interceptor.
 *
 * 拦截器可识别的剪贴板服务入口方法类别。
 */
internal enum class ClipboardMethodKind {
    /**
     * Standard primary clipboard read entry point.
     *
     * 标准主剪贴板读取入口。
     */
    READ,

    /**
     * Standard primary clipboard write entry point.
     *
     * 标准主剪贴板写入入口。
     */
    WRITE,

    /**
     * Vendor-specific clipboard read entry point returning ClipData.
     *
     * 返回 ClipData 的厂商专有剪贴板读取入口。
     */
    VENDOR_READ,

    /**
     * Clipboard content existence probe entry point.
     *
     * 剪贴板内容存在性探测入口。
     */
    HAS_CLIP,

    /**
     * Clipboard metadata description probe entry point.
     *
     * 剪贴板元数据描述探测入口。
     */
    CLIP_DESCRIPTION,

    /**
     * Method that is not a clipboard interception entry point.
     *
     * 非剪贴板拦截目标方法。
     */
    NONE
}
