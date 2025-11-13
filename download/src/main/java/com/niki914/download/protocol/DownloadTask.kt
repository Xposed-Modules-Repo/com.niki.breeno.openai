package com.niki914.download.protocol

data class DownloadTask(
    val url: String,
    val totalSize: Long,
    val targetPath: String,
    val fileName: String
)

data class DownloadChunk(
    val index: Int,
    val startByte: Long,
    val endByte: Long
)