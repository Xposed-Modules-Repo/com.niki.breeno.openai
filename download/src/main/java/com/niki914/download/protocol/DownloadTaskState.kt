package com.niki914.download.protocol

/**
 * 描述整个下载任务的完整状态
 *
 * @param url 下载地址
 * @param targetPath 目标存储路径
 * @param totalSize 文件总大小
 * @param chunks 所有下载块的状态列表
 */
data class DownloadTaskState(
    val url: String,
    val targetPath: String,
    val totalSize: Long,
    val chunks: List<DownloadChunkState>
) {
    /**
     * 计算当前已下载的总字节数
     */
    val totalDownloadedBytes: Long
        get() = chunks.sumOf { it.downloadedBytes }

    /**
     * 计算当前已下载的百分比
     */
    val totalDownloadedPercent: Int
        get() = (totalDownloadedBytes * 100 / totalSize).toInt()
}

/**
 * 用来描述每个下载块的状态
 *
 * @param index 块索引
 * @param startByte 起始字节
 * @param endByte 结束字节
 * @param downloadedBytes 已下载的字节数，关键字段！
 */
data class DownloadChunkState(
    val index: Int,
    val startByte: Long,
    val endByte: Long,
    var downloadedBytes: Long = 0L
) {
    val totalBytes: Long get() = endByte - startByte + 1
    val isCompleted: Boolean get() = downloadedBytes >= totalBytes

    /**
     * 计算当前已下载的百分比
     */
    val downloadedPercent: Int
        get() = (downloadedBytes * 100 / totalBytes).toInt()
}