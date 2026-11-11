package app.fuckclip.hook

import android.content.ClipData
import android.content.ClipDescription
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/**
 * Classifies reflective clipboard service methods and extracts caller package arguments.
 *
 * 对剪贴板服务反射方法进行分类并提取调用方包名参数。
 */
internal object ClipboardMethodClassifier {
    private val STANDARD_READ_NAMES = setOf("getPrimaryClip")
    private val HAS_CLIP_NAMES = setOf("hasPrimaryClip", "hasClipboardText")

    /**
     * Returns the [ClipboardMethodKind] of [method]; vendor read entries are only reported when [includeVendorReads] is true.
     *
     * 返回 [method] 的入口类别；仅当 [includeVendorReads] 为 true 时才识别厂商专有读取入口。
     */
    fun classify(method: Method, includeVendorReads: Boolean): ClipboardMethodKind {
        val modifiers = method.modifiers
        if (!Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers) || Modifier.isAbstract(modifiers) ||
            method.isSynthetic || method.isBridge
        ) {
            return ClipboardMethodKind.NONE
        }

        val name = method.name
        val params = method.parameterTypes
        val returnType = method.returnType
        val leadingString = params.isNotEmpty() && params[0] == String::class.java

        return when {
            name in STANDARD_READ_NAMES && returnType == ClipData::class.java && leadingString ->
                ClipboardMethodKind.READ

            name == "setPrimaryClip" && returnType == Void.TYPE && params.size >= 2 &&
                params[0] == ClipData::class.java && params[1] == String::class.java ->
                ClipboardMethodKind.WRITE

            name in HAS_CLIP_NAMES && returnType == java.lang.Boolean.TYPE && leadingString ->
                ClipboardMethodKind.HAS_CLIP

            name == "getPrimaryClipDescription" && returnType == ClipDescription::class.java && leadingString ->
                ClipboardMethodKind.CLIP_DESCRIPTION

            includeVendorReads && name !in STANDARD_READ_NAMES && returnType == ClipData::class.java &&
                name.contains("Clip") && params.none { it == ClipData::class.java } ->
                ClipboardMethodKind.VENDOR_READ

            else -> ClipboardMethodKind.NONE
        }
    }

    /**
     * Extracts the calling package name from [args], checking [preferredIndex] first and scanning remaining arguments.
     *
     * 从方法参数列表 [args] 中提取调用方包名，优先检查 [preferredIndex] 位置，其次扫描其余字符串参数。
     */
    fun extractPackageArg(args: List<Any?>, preferredIndex: Int = 0): String? {
        val preferred = args.getOrNull(preferredIndex) as? String
        if (!preferred.isNullOrBlank() && (preferred == "android" || preferred.contains('.'))) {
            return preferred
        }
        for (arg in args) {
            val candidate = arg as? String ?: continue
            if (candidate.isNotBlank() && (candidate == "android" || candidate.contains('.'))) {
                return candidate
            }
        }
        return null
    }
}
