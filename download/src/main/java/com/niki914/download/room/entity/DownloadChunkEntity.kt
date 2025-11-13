package com.niki914.download.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.niki914.download.protocol.DownloadChunk

/**
 * 下载分块实体，代表一个大文件中的一小块下载进度
 */
@Entity(
    tableName = "download_chunks",
    foreignKeys = [
        ForeignKey(
            entity = DownloadTaskEntity::class,
            parentColumns = ["url"],
            childColumns = ["task_url"],
            onDelete = ForeignKey.CASCADE // 当主任务被删除时，其所有分块也应被级联删除
        )
    ]
)
data class DownloadChunkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    // 使用索引来确保通过 task_url 查询分块时的高效性
    @ColumnInfo(name = "task_url", index = true)
    val taskUrl: String,

    @ColumnInfo(name = "chunk_index")
    val chunkIndex: Int,

    @ColumnInfo(name = "start_byte")
    val startByte: Long,

    @ColumnInfo(name = "end_byte")
    val endByte: Long,

    @ColumnInfo(name = "downloaded_bytes")
    var downloadedBytes: Long
) {
    companion object {
        fun fromChuck(
            taskUrl: String,
            chunk: DownloadChunk
        ): DownloadChunkEntity {
            return DownloadChunkEntity(
                taskUrl = taskUrl,
                chunkIndex = chunk.index,
                startByte = chunk.startByte,
                endByte = chunk.endByte,
                downloadedBytes = 0
            )
        }
    }
}