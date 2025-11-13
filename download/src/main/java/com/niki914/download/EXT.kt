package com.niki914.download

import android.os.Environment
import com.niki914.download.protocol.DownloadChunk
import com.niki914.download.protocol.DownloadTask
import java.io.File
import kotlin.math.ceil


/**
 * 根据文件总大小和设定的块大小，创建下载任务列表。
 */
internal fun createDownloadChunks(
    totalSize: Long,
    chunkSizeForEachTask: Long
): List<DownloadChunk> {
    if (chunkSizeForEachTask <= 0) {
        // 如果块大小不合法，则只创建一个块下载整个文件
        return listOf(DownloadChunk(0, 0, totalSize - 1))
    }

    val chunkCount = ceil(totalSize.toDouble() / chunkSizeForEachTask).toInt()
    val chunks = mutableListOf<DownloadChunk>()

    for (i in 0 until chunkCount) {
        val startByte = i * chunkSizeForEachTask
        val endByte = if (i == chunkCount - 1) {
            totalSize - 1 // 最后一个块到文件末尾
        } else {
            startByte + chunkSizeForEachTask - 1
        }
        chunks.add(DownloadChunk(i, startByte, endByte))
    }

    return chunks
}

/**
 * 获取应用在 Download 目录下的专属文件路径
 *
 * /storage/emulated/0/Download/$subDir/$fileName
 *
 * @param subDir 子目录名称
 * @param fileName 文件名
 * @return 文件的绝对路径
 */
fun getDownloadPath(subDir: String, fileName: String): String {
    val downloadDir =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val appDir = File(downloadDir, subDir)
    if (!appDir.exists()) {
        appDir.mkdirs()
    }
    return File(appDir, fileName).absolutePath
}

suspend fun MultiThreadDownloader.download(
    task: DownloadTask,
    onPercentProgress: (progress: Int) -> Unit = {}
): IOResult {
    MultiThreadFileWriter(task.targetPath).use {
        return download(
            task = task,
            downloadHandler = it,
            onPercentProgress = onPercentProgress
        )
    }
}

suspend fun MultiThreadDownloader.download(
    task: DownloadTask,
    downloadHandler: MultiThreadFileWriter,
    onPercentProgress: (progress: Int) -> Unit
): IOResult {
    var lastPercent = 0 // 存储上一次报告的百分比，避免重复回调

    return download(
        task = task,
        multiThreadFileWriter = downloadHandler,
        onStateChanged = { state ->
            val currentPercent = state.totalDownloadedPercent
            if (currentPercent != lastPercent) {
                lastPercent = currentPercent
                onPercentProgress(currentPercent)
            }
        })
}

suspend fun MultiThreadDownloader.download(
    task: DownloadTask,
    onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
): IOResult {
    MultiThreadFileWriter(task.targetPath).use {
        return download(
            task = task,
            multiThreadFileWriter = it,
            onProgress = onProgress
        )
    }
}

suspend fun MultiThreadDownloader.download(
    task: DownloadTask,
    multiThreadFileWriter: MultiThreadFileWriter,
    onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }
): IOResult {
    return download(
        task = task,
        multiThreadFileWriter = multiThreadFileWriter,
        onStateChanged = { state ->
            val downloaded = state.totalDownloadedBytes
            val total = state.totalSize
            onProgress(downloaded, total)
        })
}