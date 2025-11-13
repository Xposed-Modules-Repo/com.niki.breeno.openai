package com.niki914.download.util

import com.niki914.download.createDownloadChunks
import com.niki914.download.protocol.DownloadChunk
import com.niki914.download.protocol.DownloadChunkState
import com.niki914.download.protocol.DownloadTask
import com.niki914.download.protocol.DownloadTaskState
import com.niki914.download.protocol.TaskWithChunks
import com.niki914.download.room.DownloadRepository
import com.zephyr.log.logD
import com.zephyr.log.logI
import com.zephyr.log.logW
import com.zephyr.provider.TAG
import java.io.File

internal class ResumeHandler(
    private val repo: DownloadRepository,
    private val chunkSizeForEachTask: Long
) {
    data class HandledData(
        val taskState: DownloadTaskState,
        val chunks: List<DownloadChunk>,
        val chunkStates: List<DownloadChunkState> // 必须是此列表，因为它包含进度
    )

    fun isValidResumeTask(
        existingTask: TaskWithChunks?,
        task: DownloadTask
    ): Boolean {
        return existingTask != null &&
                existingTask.task.totalSize == task.totalSize &&
                existingTask.task.targetPath == task.targetPath &&
                File(task.targetPath).exists()
    }

    suspend fun createHandledData(
        existingTask: TaskWithChunks?,
        task: DownloadTask
    ): HandledData {
        val taskState: DownloadTaskState
        val chunks: List<DownloadChunk>
        val chunkStates: List<DownloadChunkState> // 必须是此列表，因为它包含进度

        // 检查是否存在一个*有效*的旧任务 (URL, 路径, 总大小都匹配, 且文件存在)
        val isValidResumeTask = isValidResumeTask(existingTask, task)

        if (isValidResumeTask) {
            logI(TAG, "发现有效的旧下载记录，准备断点续传")
            // 使用数据库中保存的状态
            taskState = existingTask!!.task
            chunkStates = existingTask.chunks // Liat<DownloadChunkState> (带进度)
            chunks = chunkStates.map { // List<DownloadChunk> (用于迭代)
                DownloadChunk(it.index, it.startByte, it.endByte)
            }

            logD(
                TAG,
                "已恢复 ${chunks.size} 个下载块的状态。已下载: ${taskState.totalDownloadedBytes} bytes"
            )
        } else {
            // 如果任务无效 (例如文件总大小变了)，或任务不存在
            if (existingTask != null) {
                logW(TAG, "发现无效的旧下载记录（文件大小或路径不匹配），将删除并重新下载")
                repo.deleteTask(task.url)
            } else {
                logI(TAG, "未发现旧的下载记录")
            }

            logI(TAG, "创建新的下载任务...")

            // 创建下载块及对应的状态追踪器
            chunks = createDownloadChunks(
                task.totalSize,
                chunkSizeForEachTask
            ) // List<DownloadChunk>
            logD(TAG, "文件被分为 ${chunks.size} 个下载块")

            chunkStates = chunks.map { // List<DownloadChunkState>
                DownloadChunkState(it.index, it.startByte, it.endByte, 0L)
            }

            taskState = DownloadTaskState(task.url, task.targetPath, task.totalSize, chunkStates)

            // 存入数据库
            repo.createNewTask(task, chunks)
        }

        return HandledData(
            taskState,
            chunks,
            chunkStates
        )
    }
}