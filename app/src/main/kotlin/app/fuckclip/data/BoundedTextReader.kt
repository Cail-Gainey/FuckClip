package app.fuckclip.data

import java.io.IOException
import java.io.InputStream

/**
 * Maximum number of characters accepted from a user-provided rule backup.
 *
 * 允许从用户提供的规则备份中读取的最大字符数。
 */
internal const val MAX_BACKUP_CHARS: Int = 1 shl 20

/**
 * Reads UTF-8 text from [inputStream] up to [maxChars], throwing [IOException] if the content exceeds the limit.
 *
 * 从 [inputStream] 读取不超过 [maxChars] 字符数的 UTF-8 文本，超出上限时抛出 [IOException]。
 */
internal fun readBoundedText(inputStream: InputStream, maxChars: Int = MAX_BACKUP_CHARS): String {
    val builder = StringBuilder()
    val reader = inputStream.bufferedReader(Charsets.UTF_8)
    val buffer = CharArray(8 * 1024)
    while (true) {
        val read = reader.read(buffer)
        if (read < 0) break
        if (builder.length + read > maxChars) {
            throw IOException("Input exceeds the supported size of $maxChars characters")
        }
        builder.append(buffer, 0, read)
    }
    return builder.toString()
}
