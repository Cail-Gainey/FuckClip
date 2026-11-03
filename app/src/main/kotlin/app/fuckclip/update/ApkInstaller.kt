package app.fuckclip.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import androidx.core.content.pm.PackageInfoCompat
import app.fuckclip.BuildConfig
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.CancellationException

/**
 * Downloads update APK assets and invokes the system package installer.
 *
 * 下载更新 APK 资产并调起系统安装器的安装协调工具。
 */
object ApkInstaller {
    private const val TAG = "ApkInstaller"
    private const val DIR = "updates"

    /**
     * Possible results when requesting package installation.
     *
     * 请求执行应用包安装时的可能执行结果。
     */
    enum class InstallResult {
        /**
         * Installation intent successfully started.
         *
         * 安装意图已成功启动。
         */
        STARTED,

        /**
         * Needs unknown app install permission before proceeding.
         *
         * 需要授予未知来源应用安装权限。
         */
        NEED_PERMISSION,

        /**
         * Failed to launch package installer.
         *
         * 启动应用包安装器失败。
         */
        FAILED
    }

    /**
     * Downloads the specified [apk] asset to local cache, returning the verified file.
     *
     * 将指定的 [apk] 资产下载到本地缓存，并返回校验通过的文件。
     *
     * @param context Application context.
     * @param apk Target APK asset to download.
     * @param isCancelled Callback returning whether the download was cancelled.
     * @param onProgress Callback invoked with (bytesDone, totalBytes).
     * @return Downloaded and verified [File].
     */
    @Throws(Exception::class)
    fun download(
        context: Context,
        apk: ApkAsset,
        isCancelled: () -> Boolean,
        onProgress: (done: Long, total: Long) -> Unit
    ): File {
        val dir = File(context.cacheDir, DIR).apply { mkdirs() }
        val target = File(dir, File(apk.name).name)
        val part = File(dir, target.name + ".part")

        dir.listFiles()?.forEach { if (it != target) it.delete() }

        if (target.exists()) {
            if (isValid(context, target, apk)) {
                onProgress(target.length(), target.length())
                return target
            }
            target.delete()
        }

        val connection = URL(apk.url).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "FuckClip-App/${BuildConfig.VERSION_NAME}")

            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP response code: $code")
            }

            val total = connection.contentLengthLong.takeIf { it > 0 } ?: apk.size
            var done = 0L
            val buffer = ByteArray(32 * 1024)
            connection.inputStream.use { input ->
                part.outputStream().use { output ->
                    while (true) {
                        if (isCancelled()) throw CancellationException("download cancelled")
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        done += n
                        onProgress(done, total)
                    }
                }
            }
        } catch (t: Throwable) {
            part.delete()
            throw t
        } finally {
            connection.disconnect()
        }

        if (!isValid(context, part, apk)) {
            part.delete()
            throw IOException("downloaded APK failed verification: ${apk.name}")
        }
        if (!part.renameTo(target)) {
            part.delete()
            throw IOException("cannot move ${part.name} to ${target.name}")
        }
        return target
    }

    /**
     * Starts the system package installer for the downloaded [file].
     *
     * 为已下载的 [file] 启动系统应用安装器。
     *
     * @param context Context used to start activities.
     * @param file Target APK file to install.
     * @return [InstallResult] indicating if installation was initiated or needs permission.
     */
    fun install(context: Context, file: File): InstallResult {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                context.startActivity(
                    Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                return InstallResult.NEED_PERMISSION
            }

            val uri = FileProvider.getUriForFile(
                context, "${BuildConfig.APPLICATION_ID}.fileprovider", file
            )
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return InstallResult.STARTED
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start package installer", t)
            return InstallResult.FAILED
        }
    }

    private fun isValid(context: Context, file: File, apk: ApkAsset): Boolean {
        if (apk.size > 0 && file.length() != apk.size) return false
        if (apk.sha256 != null && sha256(file) != apk.sha256) return false

        return try {
            val pm = context.packageManager
            val archive = pm.getPackageArchiveInfo(file.path, 0) ?: return false
            val installed = pm.getPackageInfo(context.packageName, 0)
            archive.packageName == context.packageName &&
                PackageInfoCompat.getLongVersionCode(archive) >=
                PackageInfoCompat.getLongVersionCode(installed)
        } catch (t: Throwable) {
            Log.e(TAG, "Cannot verify package archive: ${file.name}", t)
            false
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(32 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
