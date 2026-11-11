package app.fuckclip.hook

import android.content.ClipData
import android.content.Context
import android.util.Log
import android.util.SparseArray

private const val MAX_CAPTURED_TEXT_CHARS = 8_192
private const val TRUNCATION_MARKER = "…"

/**
 * Reads clipboard content directly from internal service data structures, bypassing background restrictions and paste notices.
 *
 * 直接从系统剪贴板服务内部数据结构读取当前内容，绕过后台限制与粘贴提示。
 */
internal object ClipboardReader {
    private const val TAG = "ClipboardReader"

    /**
     * Resets any cached reflection states.
     *
     * 重置反射读取缓存状态。
     */
    fun reset() {
        // No static failed caches so subsequent attempts always re-evaluate cleanly
    }

    /**
     * Extracts plain text from [clipData] using [context], capping the length at [MAX_CAPTURED_TEXT_CHARS].
     *
     * 使用 [context] 从 [clipData] 中解析提取纯文本内容，长度限制在 [MAX_CAPTURED_TEXT_CHARS] 以内。
     */
    fun extractClipText(clipData: ClipData?, context: Context?): String? {
        if (clipData == null || clipData.itemCount <= 0) return null
        val sb = StringBuilder()
        for (i in 0 until clipData.itemCount) {
            val item = clipData.getItemAt(i) ?: continue
            val text = item.text?.toString()
                ?: if (context != null) {
                    try {
                        item.coerceToText(context)?.toString()
                    } catch (_: Throwable) {
                        null
                    }
                } else null
                ?: item.htmlText
                ?: item.uri?.toString()
                ?: item.intent?.toUri(0)
            if (!text.isNullOrEmpty()) {
                if (sb.isNotEmpty()) sb.append("\n")
                val remaining = MAX_CAPTURED_TEXT_CHARS - sb.length
                if (remaining <= 0) break
                if (text.length > remaining) {
                    sb.append(text, 0, remaining)
                    sb.append(TRUNCATION_MARKER)
                    break
                }
                sb.append(text)
            }
        }
        return sb.toString().ifEmpty { null }
    }

    /**
     * Attempts to read the primary [ClipData] for the specified [userId] directly from [service].
     *
     * 尝试直接从剪贴板服务实例 [service] 中读取指定 [userId] 的主剪贴板内容。
     */
    fun readPrimaryClip(service: Any?, userId: Int): ClipData? {
        val instance = service ?: return null
        return try {
            readFromInstance(instance, userId)
        } catch (t: Throwable) {
            Log.w(TAG, "readPrimaryClip failed on ${instance.javaClass.name}", t)
            null
        }
    }

    private fun readFromInstance(service: Any, userId: Int): ClipData? {
        // Step 1: Resolve target instance (outer class if service is an inner class like ClipboardImpl)
        val target = findOuterInstance(service) ?: service

        // Step 2: Try getClipboardLocked / getClipboard candidate methods on target
        val clipFromMethod = tryInvokeClipboardMethod(target, userId)
        if (clipFromMethod != null) {
            return clipFromMethod
        }

        // Step 3: Try mClipboards / clipboards field on target
        val clipFromField = tryReadClipboardsField(target, userId)
        if (clipFromField != null) {
            return clipFromField
        }

        // Step 4: Check if target itself has a direct ClipData field
        val directClip = findDirectClipData(target)
        if (directClip != null) {
            return directClip
        }

        return null
    }

    private fun tryInvokeClipboardMethod(target: Any, userId: Int): ClipData? {
        var c: Class<*>? = target.javaClass
        while (c != null && c != Any::class.java) {
            for (method in c.declaredMethods) {
                val name = method.name
                if (name == "getClipboardLocked" || name == "getClipboard" || name.contains("Clipboard", ignoreCase = true)) {
                    val params = method.parameterTypes
                    if (method.returnType == java.lang.Void.TYPE || method.returnType.isPrimitive) {
                        continue
                    }
                    try {
                        method.isAccessible = true
                        val clipboardObj = when (params.size) {
                            1 -> if (params[0] == Int::class.javaPrimitiveType) method.invoke(target, userId) else null
                            2 -> if (params[0] == Int::class.javaPrimitiveType && params[1] == Int::class.javaPrimitiveType) {
                                method.invoke(target, userId, 0)
                            } else null
                            0 -> method.invoke(target)
                            else -> null
                        }
                        if (clipboardObj != null) {
                            val clip = extractClipFromClipboardObject(clipboardObj)
                            if (clip != null) return clip
                        }
                    } catch (_: Throwable) {}
                }
            }
            c = c.superclass
        }
        return null
    }

    private fun tryReadClipboardsField(target: Any, userId: Int): ClipData? {
        var c: Class<*>? = target.javaClass
        while (c != null && c != Any::class.java) {
            for (field in c.declaredFields) {
                if (field.name.contains("clipboard", ignoreCase = true)) {
                    try {
                        field.isAccessible = true
                        val container = field.get(target) ?: continue
                        val clipboardObj = getFromContainer(container, userId)
                        if (clipboardObj != null) {
                            val clip = extractClipFromClipboardObject(clipboardObj)
                            if (clip != null) return clip
                        }
                    } catch (_: Throwable) {}
                }
            }
            c = c.superclass
        }
        return null
    }

    private fun getFromContainer(container: Any, userId: Int): Any? {
        if (container is SparseArray<*>) {
            return container.get(userId) ?: container.get(0)
        }
        for (m in container.javaClass.methods) {
            if (m.name == "get") {
                val p = m.parameterTypes
                try {
                    m.isAccessible = true
                    if (p.size == 2 && p[0] == Int::class.javaPrimitiveType && p[1] == Int::class.javaPrimitiveType) {
                        val res = m.invoke(container, userId, 0)
                        if (res != null) return res
                    } else if (p.size == 1 && p[0] == Int::class.javaPrimitiveType) {
                        val res = m.invoke(container, userId)
                        if (res != null) return res
                    }
                } catch (_: Throwable) {}
            }
        }
        return null
    }

    private fun extractClipFromClipboardObject(clipboardObj: Any): ClipData? {
        if (clipboardObj is ClipData) return clipboardObj
        var c: Class<*>? = clipboardObj.javaClass
        while (c != null && c != Any::class.java) {
            for (field in c.declaredFields) {
                if (field.type == ClipData::class.java || field.name == "primaryClip" || field.name == "mPrimaryClip") {
                    try {
                        field.isAccessible = true
                        val clip = field.get(clipboardObj) as? ClipData
                        if (clip != null) return clip
                    } catch (_: Throwable) {}
                }
            }
            c = c.superclass
        }
        return null
    }

    private fun findDirectClipData(target: Any): ClipData? {
        var c: Class<*>? = target.javaClass
        while (c != null && c != Any::class.java) {
            for (field in c.declaredFields) {
                if (field.type == ClipData::class.java) {
                    try {
                        field.isAccessible = true
                        val clip = field.get(target) as? ClipData
                        if (clip != null) return clip
                    } catch (_: Throwable) {}
                }
            }
            c = c.superclass
        }
        return null
    }

    private fun findOuterInstance(service: Any): Any? {
        var c: Class<*>? = service.javaClass
        while (c != null && c != Any::class.java) {
            for (field in c.declaredFields) {
                if (field.name.startsWith("this$0") ||
                    field.name == "mService" ||
                    field.name == "mClipboardService" ||
                    field.type.name.endsWith("ClipboardService")
                ) {
                    try {
                        field.isAccessible = true
                        val outer = field.get(service)
                        if (outer != null && outer !== service) return outer
                    } catch (_: Throwable) {}
                }
            }
            c = c.superclass
        }
        return null
    }
}
