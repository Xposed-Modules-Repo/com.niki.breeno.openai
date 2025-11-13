package com.niki914.download.room.entity

import androidx.room.Embedded
import androidx.room.Relation

/**
 * 一个方便查询的包装类，包含一个下载任务及其所有的下载分块
 *
 * 在 Dao 里面声明为返回值，调用的时候 Room 会查询两个表（DownloadTaskEntity, DownloadChunkEntity），构建出这个 TaskWithChunks 数据类再返回
 */
data class DBTaskWithChunks(
    @Embedded
    val task: DownloadTaskEntity,

    @Relation(
        parentColumn = "url",
        entityColumn = "task_url"
    )
    val chunks: List<DownloadChunkEntity>
)