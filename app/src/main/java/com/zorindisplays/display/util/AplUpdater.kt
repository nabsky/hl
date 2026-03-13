package com.zorindisplays.display.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

class ApkUpdater(private val context: Context) {

    private val client = OkHttpClient()

    suspend fun downloadAndInstall(
        url: String,
        fileName: String,
        onProgress: (percent: Int) -> Unit = {}
    ) = withContext(Dispatchers.IO) {

        val dir = context.getExternalFilesDir(null)
            ?: throw IllegalStateException("No external files dir")
        val outFile = File(dir, fileName)
        if (outFile.exists()) outFile.delete()

        val req = Request.Builder().url(url).build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) {
                throw IOException("HTTP ${resp.code}")
            }

            val body = resp.body ?: throw IOException("Empty body")
            val total = body.contentLength().coerceAtLeast(1L)

            body.byteStream().use { input ->
                outFile.outputStream().use { output ->
                    val buf = ByteArray(8 * 1024)
                    var sum = 0L
                    var last = -1
                    while (true) {
                        val read = input.read(buf)
                        if (read <= 0) break
                        output.write(buf, 0, read)
                        sum += read
                        val p = ((sum * 100) / total).toInt().coerceIn(0, 100)
                        if (p != last) {
                            last = p
                            onProgress(p)
                        }
                    }
                    output.flush()
                }
            }
        }

        withContext(Dispatchers.Main) {
            installApk(outFile)
        }
    }

    private fun installApk(file: File) {
        val intent = Intent(Intent.ACTION_VIEW).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        val apkUri: Uri =
            if (Build.VERSION.SDK_INT >= 24) {
                FileProvider.getUriForFile(
                    context,
                    context.packageName + ".provider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

        intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        context.startActivity(intent)
    }
}