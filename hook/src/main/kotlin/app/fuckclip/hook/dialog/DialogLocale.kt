package app.fuckclip.hook.dialog

import android.content.res.Resources
import android.os.Build
import java.util.Locale

/**
 * Enumeration of supported locale categories for the clipboard request dialog.
 *
 * 剪贴板权限申请弹窗支持的语言区域枚举。
 */
internal enum class DialogLocale {
    /**
     * English locale category.
     *
     * 英语区域语言。
     */
    EN,

    /**
     * Simplified Chinese locale category.
     *
     * 简体中文区域语言。
     */
    ZH_CN,

    /**
     * Traditional Chinese locale category.
     *
     * 繁体中文区域语言。
     */
    ZH_TW;

    companion object {
        /**
         * Resolves the appropriate [DialogLocale] from a user-specified language tag or system configuration.
         *
         * 根据用户指定的语言标签或系统当前配置解析适用的 [DialogLocale]。
         *
         * @param preferredLanguage BCP 47 language tag (e.g. "zh-TW", "zh-CN", "en") or empty for follow system.
         */
        fun resolve(preferredLanguage: String): DialogLocale {
            val tag = preferredLanguage.trim().lowercase(Locale.ROOT)
            if (tag.isNotEmpty()) {
                return when {
                    tag.startsWith("zh-tw") || tag.startsWith("zh-hk") ||
                        tag.startsWith("zh-mo") || tag.contains("hant") -> ZH_TW
                    tag.startsWith("zh") -> ZH_CN
                    tag.startsWith("en") -> EN
                    else -> EN
                }
            }

            val systemLocale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Resources.getSystem().configuration.locales.get(0) ?: Locale.getDefault()
            } else {
                @Suppress("DEPRECATION")
                Resources.getSystem().configuration.locale ?: Locale.getDefault()
            }

            val lang = systemLocale.language.lowercase(Locale.ROOT)
            val country = systemLocale.country.lowercase(Locale.ROOT)
            val script = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                systemLocale.script.lowercase(Locale.ROOT)
            } else ""

            return if (lang == "zh") {
                if (script.contains("hant") || country in listOf("tw", "hk", "mo")) {
                    ZH_TW
                } else {
                    ZH_CN
                }
            } else {
                EN
            }
        }
    }
}
