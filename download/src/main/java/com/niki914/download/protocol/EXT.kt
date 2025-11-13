package com.niki914.download.protocol

import com.niki914.download.room.entity.DBTaskWithChunks
import com.niki914.download.room.entity.DownloadChunkEntity
import com.niki914.download.room.entity.DownloadTaskEntity

fun DownloadTaskEntity.toTaskState(chunks: List<DownloadChunkState>): DownloadTaskState {
    return DownloadTaskState(
        url = url,
        targetPath = targetPath,
        totalSize = totalSize,
        chunks = chunks
    )
}

fun DownloadChunkEntity.toChunkState(): DownloadChunkState {
    return DownloadChunkState(
        index = chunkIndex,
        startByte = startByte,
        endByte = endByte,
        downloadedBytes = downloadedBytes
    )
}

fun DBTaskWithChunks.toTaskWithChunks(): TaskWithChunks {
    val chunkStates = chunks.map { it.toChunkState() }
    return TaskWithChunks(
        task = task.toTaskState(chunkStates),
        chunks = chunkStates
    )
}