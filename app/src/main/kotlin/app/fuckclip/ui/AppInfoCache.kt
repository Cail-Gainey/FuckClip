package app.fuckclip.ui

import android.graphics.Bitmap
import android.util.LruCache

/**
 * Total bitmap memory budget for cached application icons in bytes.
 *
 * 应用图标内存缓存的总字节预算上限。
 */
private const val ICON_CACHE_BYTES = 4 * 1024 * 1024

/**
 * Maximum number of cached application labels.
 *
 * 缓存应用显示名称的最大条目数量。
 */
private const val LABEL_CACHE_ENTRIES = 200

/**
 * Shared in-memory cache for application icons and labels used by rule and history lists.
 *
 * 供应用规则列表与使用记录列表共用的应用图标与显示名称内存缓存。
 */
internal object AppInfoCache {
    private val iconCache = object : LruCache<String, Bitmap>(ICON_CACHE_BYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }
    private val labelCache = LruCache<String, String>(LABEL_CACHE_ENTRIES)

    /**
     * Retrieves the cached application icon [Bitmap] for [pkg].
     *
     * 获取指定包名 [pkg] 缓存的应用图标 [Bitmap]。
     */
    fun getIcon(pkg: String): Bitmap? = iconCache.get(pkg)

    /**
     * Caches the application icon [bitmap] for [pkg].
     *
     * 缓存指定包名 [pkg] 的应用图标 [bitmap]。
     */
    fun putIcon(pkg: String, bitmap: Bitmap) {
        iconCache.put(pkg, bitmap)
    }

    /**
     * Retrieves the cached human-readable application label for [pkg].
     *
     * 获取指定包名 [pkg] 缓存的可读应用名称。
     */
    fun getLabel(pkg: String): String? = labelCache.get(pkg)

    /**
     * Caches the human-readable application [label] for [pkg].
     *
     * 缓存指定包名 [pkg] 的可读应用名称 [label]。
     */
    fun putLabel(pkg: String, label: String) {
        labelCache.put(pkg, label)
    }
}
