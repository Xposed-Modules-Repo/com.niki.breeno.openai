package com.niki914.download.room

import com.niki914.download.protocol.DownloadChunk
import com.niki914.download.protocol.DownloadTask
import com.niki914.download.protocol.TaskWithChunks
import com.niki914.download.protocol.toTaskWithChunks
import com.niki914.download.room.entity.DownloadChunkEntity
import com.niki914.download.room.entity.DownloadTaskEntity
import com.zephyr.log.logE
import com.zephyr.provider.TAG

/**
 * 对数据库操作再封装
 */
internal class DownloadRepository(private val dao: DownloadDao) {
    val printLogs: Boolean = false

    suspend fun findTask(url: String): TaskWithChunks? {
        val data = dao.getTaskWithChunks(url).apply {
            if (printLogs) {
                if (this == null) {
                    logE(TAG, "couldn't find task: $url")
                } else {
                    logE(TAG, "found task: $url")
                    logE(TAG, this.task.toString())
                    logE(TAG, "chunk size: ${this.chunks.size}")
                }
            }
        }

        return data?.toTaskWithChunks()
    }

    suspend fun createNewTask(
        task: DownloadTask,
        chunks: List<DownloadChunk>
    ) {
        val url = task.url

        val taskEntity = DownloadTaskEntity.fromTask(task)
        val chunkEntities = chunks.map { chunk ->
            DownloadChunkEntity.fromChuck(url, chunk)
        }

        if (printLogs) {
            logE(TAG, "insert task: $url")
            logE(TAG, task.toString())
            logE(TAG, "chunk size: ${chunks.size}")
        }

        return dao.insertNewTaskWithChunks(taskEntity, chunkEntities)
    }

    suspend fun updateChunkProgress(url: String, chunkIndex: Int, downloadedBytes: Long) {
        if (printLogs) {
            logE(TAG, "updated chunk[$chunkIndex] progress: $downloadedBytes bytes downloaded")
        }
        return dao.updateChunkProgress(url, chunkIndex, downloadedBytes)
    }

    suspend fun deleteTask(url: String) {
        if (printLogs) {
            logE(TAG, "deleted task by url: $url")
        }
        return dao.deleteTaskByUrl(url)
    }
}