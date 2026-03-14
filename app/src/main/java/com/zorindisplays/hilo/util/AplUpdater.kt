package com.zorindisplays.hilo.util

import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import java.io.File

class ApkUpdater(private val context: Context) {

    private val appContext = context.applicationContext

    fun downloadAndInstall(
        url: String,
        fileName: String,
        onProgress: (percent: Int) -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Software Update")
            .setDescription("Downloading update...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)
            .setMimeType("application/vnd.android.package-archive")
            .setDestinationInExternalFilesDir(
                appContext,
                Environment.DIRECTORY_DOWNLOADS,
                fileName
            )

        val dm = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = dm.enqueue(request)

        val handler = Handler(Looper.getMainLooper())
        val poll = object : Runnable {
            override fun run() {
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor: Cursor = dm.query(query)

                cursor.use {
                    if (!it.moveToFirst()) {
                        onError("Download not found")
                        return
                    }

                    val status =
                        it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))

                    when (status) {
                        DownloadManager.STATUS_RUNNING,
                        DownloadManager.STATUS_PAUSED,
                        DownloadManager.STATUS_PENDING -> {
                            val total =
                                it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                            val downloaded =
                                it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))

                            if (total > 0) {
                                val percent = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                                onProgress(percent)
                            }

                            handler.postDelayed(this, 300)
                        }

                        DownloadManager.STATUS_SUCCESSFUL -> {
                            onProgress(100)

                            val localUri =
                                it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))
                            if (localUri.isNullOrBlank()) {
                                onError("Downloaded file uri is empty")
                                return
                            }

                            installDownloadedApk(localUri)
                        }

                        DownloadManager.STATUS_FAILED -> {
                            val reason =
                                it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                            onError("Download failed: reason=$reason")
                        }
                    }
                }
            }
        }

        handler.post(poll)
    }

    private fun installDownloadedApk(localUri: String) {
        val sourceUri = Uri.parse(localUri)
        val file = File(requireNotNull(sourceUri.path))

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
            clipData = android.content.ClipData.newRawUri("", contentUri)
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
}