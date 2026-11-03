package app.fuckclip.i18n

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import app.fuckclip.R
import org.xmlpull.v1.XmlPullParser
import java.util.Locale

/**
 * Utility for managing application language preferences and locale configurations.
 *
 * 管理应用语言偏好与区域设置配置的工具类。
 */
object AppLanguage {
    @Volatile
    private var pendingLocales: LocaleListCompat? = null

    /**
     * Resolves the currently selected locale list for the given [context].
     *
     * 获取指定 [context] 当前选定的语言区域列表。
     */
    fun selectedLocales(context: Context): LocaleListCompat =
        if (Build.VERSION.SDK_INT >= 33) {
            LocaleManagerCompat.getApplicationLocales(context)
        } else {
            pendingLocales ?: AppCompatDelegate.getApplicationLocales().takeUnless { it.isEmpty }
            ?: LocaleManagerCompat.getApplicationLocales(context)
        }

    /**
     * Updates the application language according to the specified BCP 47 language [tag].
     *
     * 根据指定的 BCP 47 语言 [tag] 更新应用语言设置。
     */
    fun select(tag: String) {
        val locales = LocaleListCompat.forLanguageTags(tag)
        pendingLocales = locales
        AppCompatDelegate.setApplicationLocales(locales)
    }

    /**
     * Wraps the provided [context] with the active localized configuration.
     *
     * 使用当前激活的本地化配置包装指定的 [context]。
     */
    fun localizedContext(context: Context): Context {
        val selected = selectedLocales(context)
        val locales = selected.takeUnless { it.isEmpty } ?: LocaleManagerCompat.getSystemLocales(context)
        val first = locales[0] ?: Locale.ENGLISH
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= 24) {
            config.setLocales(LocaleList.forLanguageTags(locales.toLanguageTags()))
        } else {
            @Suppress("DEPRECATION")
            config.locale = first
        }
        config.setLayoutDirection(first)
        return context.createConfigurationContext(config)
    }

    /**
     * Reads all supported language tags defined in the application's locale config using [context].
     *
     * 使用 [context] 读取应用语言配置文件中声明的所有受支持语言标签。
     */
    fun supportedTags(context: Context): List<String> = buildList {
        context.resources.getXml(R.xml.locales_config).use { parser ->
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                    parser.getAttributeValue("http://schemas.android.com/apk/res/android", "name")
                        ?.let { add(it) }
                }
                parser.next()
            }
        }
    }

    /**
     * Returns the human-readable native display name for the given language [tag].
     *
     * 获取指定语言 [tag] 的本地化人类可读显示名称。
     */
    fun nativeName(tag: String): String = when (tag.lowercase(Locale.ROOT)) {
        "en" -> "English"
        "zh-cn", "zh-hans", "zh-hans-cn" -> "简体中文"
        "zh-tw", "zh-hk", "zh-mo", "zh-hant", "zh-hant-tw", "zh-hant-hk" -> "繁體中文"
        else -> Locale.forLanguageTag(tag).let {
            it.getDisplayName(it).replaceFirstChar { c ->
                if (c.isLowerCase()) c.titlecase(it) else c.toString()
            }
        }
    }
}
