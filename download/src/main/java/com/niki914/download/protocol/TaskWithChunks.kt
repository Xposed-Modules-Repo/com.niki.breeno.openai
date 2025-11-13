package com.niki914.download.protocol

data class TaskWithChunks(
    val task: DownloadTaskState,
    val chunks: List<DownloadChunkState>
)