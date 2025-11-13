package com.niki914.download.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.niki914.download.protocol.DownloadTask

/**
 * 下载任务实体，代表一个独立的下载任务
 */
@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "url")
    val url: String,

    @ColumnInfo(name = "target_path")
    val targetPath: String,

    @ColumnInfo(name = "file_name")
    val fileName: String,

    @ColumnInfo(name = "total_size")
    val totalSize: Long,

//    // 文件的 ETag，用于校验文件在服务器上是否被修改
//    @ColumnInfo(name = "e_tag")
//    val eTag: String?,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        fun fromTask(task: DownloadTask): DownloadTaskEntity {
            return DownloadTaskEntity(
                url = task.url,
                targetPath = task.targetPath,
                fileName = task.fileName,
                totalSize = task.totalSize,
//                eTag = "t o d o"
            )
        }
    }
}