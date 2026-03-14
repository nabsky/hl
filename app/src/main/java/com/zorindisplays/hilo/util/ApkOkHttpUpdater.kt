package com.zorindisplays.hilo.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApkOkHttpUpdater(private val context: Context) {

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private val client = OkHttpClient.Builder()
        .retryOnConnectionFailure(true)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var currentCall: Call? = null

    fun downloadAndInstall(
        url: String,
        fileName: String,
        onProgress: (percent: Int) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        cancelCurrent()

        val targetDir = appContext.getExternalFilesDir(null)
        if (targetDir == null) {
            postError(onError, "Downloads directory unavailable")
            return
        }

        val outFile = File(targetDir, uniqueFileName(fileName))
        if (outFile.exists()) {
            outFile.delete()
        }

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val call = client.newCall(request)
        currentCall = call

        Thread {
            try {
                call.execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IOException("HTTP ${response.code}")
                    }

                    val body = response.body ?: throw IOException("Empty response body")
                    val totalBytes = body.contentLength()

                    body.byteStream().use { input ->
                        outFile.outputStream().use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var downloadedBytes = 0L
                            var lastPercent = -1

                            while (true) {
                                if (call.isCanceled()) {
                                    throw IOException("Download cancelled")
                                }

                                val read = input.read(buffer)
                                if (read == -1) break

                                output.write(buffer, 0, read)
                                downloadedBytes += read

                                if (totalBytes > 0) {
                                    val percent =
                                        ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                                    if (percent != lastPercent) {
                                        lastPercent = percent
                                        postProgress(onProgress, percent)
                                    }
                                }
                            }

                            output.flush()
                        }
                    }
                }

                currentCall = null
                postProgress(onProgress, 100)

                mainHandler.post {
                    try {
                        installDownloadedApk(outFile)
                    } catch (t: Throwable) {
                        onError(t.message ?: "Failed to start installer")
                    }
                }
            } catch (t: Throwable) {
                if (outFile.exists()) {
                    outFile.delete()
                }

                currentCall = null

                val message = when {
                    t.message.isNullOrBlank() -> "Download failed"
                    else -> t.message!!
                }

                postError(onError, message)
            }
        }.start()
    }

    fun cancelCurrent() {
        currentCall?.cancel()
        currentCall = null
    }

    private fun installDownloadedApk(file: File) {
        val contentUri = FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            clipData = ClipData.newRawUri("", contentUri)
        }

        val resInfoList = appContext.packageManager.queryIntentActivities(
            intent,
            android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
        )

        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            appContext.grantUriPermission(
                packageName,
                contentUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        try {
            appContext.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            throw IllegalStateException("No package installer found")
        }
    }

    private fun uniqueFileName(original: String): String {
        val dot = original.lastIndexOf('.')
        return if (dot > 0) {
            val name = original.substring(0, dot)
            val ext = original.substring(dot)
            "${name}_${System.currentTimeMillis()}$ext"
        } else {
            "${original}_${System.currentTimeMillis()}"
        }
    }

    private fun postProgress(onProgress: (Int) -> Unit, percent: Int) {
        mainHandler.post { onProgress(percent) }
    }

    private fun postError(onError: (String) -> Unit, message: String) {
        mainHandler.post { onError(message) }
    }
}