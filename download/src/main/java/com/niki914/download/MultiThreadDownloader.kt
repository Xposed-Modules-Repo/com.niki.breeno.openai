package com.niki914.download

import com.niki914.download.protocol.DownloadChunk
import com.niki914.download.protocol.DownloadChunkState
import com.niki914.download.protocol.DownloadTask
import com.niki914.download.protocol.DownloadTaskState
import com.niki914.download.room.DownloadDao
import com.niki914.download.room.DownloadRepository
import com.niki914.download.util.ChunkDownloadRequestMaker
import com.niki914.download.util.KB
import com.niki914.download.util.MB
import com.niki914.download.util.ResumeHandler
import com.niki914.download.util.Throttle
import com.zephyr.log.logD
import com.zephyr.log.logE
import com.zephyr.log.logI
import com.zephyr.log.logW
import com.zephyr.provider.TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import okhttp3.OkHttpClient
import java.io.File
import java.io.IOException
import java.io.InputStream

private val PROGRESS_THROTTLE_BYTES = 256.KB
private val DB_UPDATE_THROTTLE_BYTES = 256.KB
private val FILE_CHECK_THROTTLE_BYTES = 1.MB
private val semaphore = Semaphore(2) // maxConcurrentTasks

private suspend fun MultiThreadDownloader.acquireSemaphore(me: String) {
    semaphore.acquire()
    logI(TAG, "    $me: acquired semaphore")
}

private fun MultiThreadDownloader.releaseSemaphore(me: String) {
    logI(TAG, "    $me: released semaphore")
    semaphore.release()
}

/**
 * 多线程下载器
 * 使用 OkHttp 和协程实现对单个文件进行分块并发下载。
 *
 * @param client OkHttpClient 实例，建议使用单例以复用连接池。
 * @param chunkSizeForEachTask 每个下载任务（线程）负责的数据块大小，单位为字节。
 */
class MultiThreadDownloader(
    private val client: OkHttpClient,
    dao: DownloadDao,
    private val chunkSizeForEachTask: Long = 2.MB // 默认2MB
) {

    private val objName = this.toString().takeLast(8)
    private val repo = DownloadRepository(dao)
    private val downloadRequestMaker = ChunkDownloadRequestMaker(client)

    private val resumeHandler = ResumeHandler(repo, chunkSizeForEachTask)

    // 重载
    suspend fun download(
        task: DownloadTask,
        onStateChanged: (state: DownloadTaskState) -> Unit = { }
    ): IOResult {
        MultiThreadFileWriter(task.targetPath).use { // 修复内存泄露
            return download(
                task = task,
                multiThreadFileWriter = it,
                onStateChanged = onStateChanged
            )
        }
    }

    suspend fun download(
        task: DownloadTask,
        multiThreadFileWriter: MultiThreadFileWriter,
        onStateChanged: (state: DownloadTaskState) -> Unit = { }
    ): IOResult {
        try {
            // 获取文件总大小 (从传入的 task 中获取)
            val totalSize = task.totalSize
            if (totalSize <= 0) {
                logE(TAG, "无法获取文件大小或文件为空. Total size: $totalSize")
                return IOResult.error<IOException>("无法获取文件大小或文件为空")
            }
            logI(TAG, "开始下载任务: ${task.url}. 文件总大小: $totalSize bytes.")

            logI(TAG, "检查数据库中是否存在旧的下载记录 for url: ${task.url}")
            val existingTask = repo.findTask(task.url)

            val handledData = resumeHandler.createHandledData(existingTask, task)

            val taskState: DownloadTaskState = handledData.taskState
            val chunks: List<DownloadChunk> = handledData.chunks
            val chunkStates: List<DownloadChunkState> = handledData.chunkStates // 必须是此列表，因为它包含进度

            // 打开文件并预分配空间
            val result = multiThreadFileWriter.openAndAllocate(totalSize)

            if (result is IOResult.Error)
                return result

            // 首次回调，报告总大小和(可能的)已恢复的状态
            onStateChanged(taskState)

            // 使用 coroutineScope 来确保所有子协程完成后再继续
            coroutineScope {
                val jobs = chunks.map { chunk ->
                    // 为每个块启动一个下载协程
                    launchChunkDownloadJob(
                        task = task,
                        taskState = taskState,
                        chunk = chunk,
                        chunkStates = chunkStates,
                        multiThreadFileWriter = multiThreadFileWriter,
                        onStateChanged = onStateChanged
                    )
                }

                jobs.joinAll() // 等待所有下载任务完成
            }

            // 8. 校验最终下载大小
            val finalDownloadedBytes = taskState.totalDownloadedBytes
            return checkFinalDownloadedBytes(task, finalDownloadedBytes)
        } catch (e: Exception) {
            logE(TAG, "下载任务异常中止:")
            logE(TAG, e.stackTraceToString())
            return IOResult.Error(e)
        }
    }

    private fun CoroutineScope.launchChunkDownloadJob(
        task: DownloadTask,
        taskState: DownloadTaskState,
        chunk: DownloadChunk,
        chunkStates: List<DownloadChunkState>,
        multiThreadFileWriter: MultiThreadFileWriter,
        onStateChanged: (state: DownloadTaskState) -> Unit = { }
    ) = launch(Dispatchers.IO) {
        // 日志前缀
        val logPrefix = "${objName}\$Chunk#${chunk.index}"
        // **获取此块的当前状态 (包含已下载进度)**
        val currentChunkState = chunkStates[chunk.index]

        // **[断点续传] 检查此块是否已完成**
        if (currentChunkState.isCompleted) {
            logD(TAG, "    $logPrefix: 块已下载完成，跳过")
            return@launch // 此协程结束
        }

        acquireSemaphore(logPrefix) // 请求一个许可，如果达到上限则挂起
        try {
            // **[断点续传] 计算续传点**
            val resumeOffset = currentChunkState.downloadedBytes
            val newStartByte = chunk.startByte + resumeOffset

            logD(
                TAG,
                "    $logPrefix: 开始下载, 范围: $newStartByte-${chunk.endByte} (已下载: $resumeOffset bytes)"
            )

            // 创建节流阀
            val dbThrottle = Throttle(DB_UPDATE_THROTTLE_BYTES) {
                // 总是读取 currentChunkState 中的“当前总和”
                repo.updateChunkProgress(
                    task.url,
                    chunk.index,
                    currentChunkState.downloadedBytes
                )
            }

            val uiThrottle =
                Throttle(PROGRESS_THROTTLE_BYTES) { // 触发 UI 回调
                    val currentStateSnapshot =
                        taskState.copy(chunks = chunkStates.map { it.copy() })
                    onStateChanged(currentStateSnapshot)
                }

            val targetFile = File(task.targetPath)
            val fileExistThrottle = Throttle(FILE_CHECK_THROTTLE_BYTES) {
                if (!targetFile.exists()) {
                    throw IOException("文件在下载过程中被外部删除: ${task.targetPath}")
                }
            }

            // **[断点续传] 字节计数器，从已保存的进度开始**
            var actualDownloadedBytesForChunk = currentChunkState.downloadedBytes

            // **[断点续传] 创建一个临时的 DownloadChunk，只用于本次 HTTP 请求**
            // 它描述了 *需要下载的剩余部分*
            val requestChunk = chunk.copy(startByte = newStartByte)

            // **[断点续传] 安全检查: 如果 newStartByte 已经超过 endByte
            // (理论上被 isCompleted 捕获, 但以防万一)
            if (requestChunk.startByte > chunk.endByte) {
                logW(
                    TAG,
                    "    $logPrefix: 启动点 ($newStartByte) 已超过结束点 (${chunk.endByte})，但未标记为完成。强制标记为完成。"
                )
                currentChunkState.downloadedBytes = currentChunkState.totalBytes
                // 确保数据库状态也是最终精确值
                repo.updateChunkProgress(
                    task.url,
                    chunk.index,
                    currentChunkState.totalBytes
                )
                return@launch
            }

            // 调用 downloadChunk，它返回 Response
            downloadRequestMaker.request(task.url, requestChunk).use { response ->
                val byteStream = response.body
                    ?.byteStream()

                checkNotNull(byteStream) {
                    "Download failed: Response body was null for chunk ${chunk.index}."
                }

                byteStream.use { inputStream ->
                    handleInputStream(
                        requestChunk, // **[断点续传] 传入 requestChunk**
                        multiThreadFileWriter,
                        inputStream
                    ) { bytesWritten ->
                        // bytesWritten 是 *本次* 写入的字节数
                        actualDownloadedBytesForChunk += bytesWritten

                        // 更新内存中的状态
                        currentChunkState.downloadedBytes += bytesWritten

                        // 累加到各个节流阀
                        dbThrottle.add(bytesWritten)
                        uiThrottle.add(bytesWritten)
                        fileExistThrottle.add(bytesWritten)
                    }

                    // 确保最后一次文件检查
                    fileExistThrottle.flush()
                }
            }

            // I/O 循环结束，校验大小
            // 期望的大小是 *整个块* 的大小
            val expectedDownloadedBytesForChunk =
                chunk.endByte - chunk.startByte + 1

            check(actualDownloadedBytesForChunk == expectedDownloadedBytesForChunk) {
                "$logPrefix downloaded. Expected size: $expectedDownloadedBytesForChunk but got: $actualDownloadedBytesForChunk"
            }

            // 确保内存状态是最终精确值 (理论上 currentChunkState.downloadedBytes 已经等于 actualDownloadedBytesForChunk)
            currentChunkState.downloadedBytes = actualDownloadedBytesForChunk

            // 确保数据库状态也是最终精确值
            uiThrottle.flush() // 先清空可能剩余的节流
            dbThrottle.flush() // 先清空可能剩余的节流
            repo.updateChunkProgress(
                task.url,
                chunk.index,
                actualDownloadedBytesForChunk // 写入最终的、经验证的值
            )

            logD(
                TAG,
                "    $logPrefix: 下载完成. 最终大小: $actualDownloadedBytesForChunk"
            )
        } catch (e: Exception) {
            logE(
                TAG,
                "    $logPrefix: 下载失败. 错误: ${e.message}"
            )
            throw e // 重新抛出异常以中断整个下载任务
        } finally {
            releaseSemaphore(logPrefix) // 释放许可
        }
    }

    private suspend fun handleInputStream(
        chunk: DownloadChunk,
        multiThreadFileWriter: MultiThreadFileWriter,
        inputStream: InputStream,
        onBytesWritten: suspend (count: Long) -> Unit
    ) {
        val buffer = ByteArray(8192) // 8KB 缓冲区
        var bytesRead: Int
        var currentOffset = chunk.startByte

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            when (val writeResult = multiThreadFileWriter.write(
                currentOffset,
                buffer,
                bytesRead
            )) {
                is IOResult.Error -> throw writeResult.cause
                else -> {
                    val bytesWritten = bytesRead.toLong()

                    currentOffset += bytesWritten
                    onBytesWritten(bytesWritten)
                }
            }
        }
    }

    private fun checkFinalDownloadedBytes(
        task: DownloadTask,
        downloadedSize: Long
    ): IOResult {
        val totalSize = task.totalSize

        return if (downloadedSize == totalSize) {
            logI(TAG, "下载成功: ${task.url}")
            IOResult.Success("下载完成")
        } else {
            val errorMessage = "下载大小不匹配: 期望 ${totalSize}, 实际 $downloadedSize"
            logE(TAG, errorMessage)
            IOResult.error<IOException>(errorMessage)
        }
    }
}