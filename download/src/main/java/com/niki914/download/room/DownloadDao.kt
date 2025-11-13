package com.niki914.download.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.niki914.download.room.entity.DownloadChunkEntity
import com.niki914.download.room.entity.DownloadTaskEntity
import com.niki914.download.room.entity.DBTaskWithChunks

/**
 * DAO for data access object
 *
 * 专门处理增删改查之类的数据库操作, 是与数据库交互的主要接口
 */
@Dao
interface DownloadDao {

    /**
     * 插入一个新的下载任务。如果已存在，则忽略。
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTask(task: DownloadTaskEntity)

    /**
     * 插入一个下载分块列表
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChunks(chunks: List<DownloadChunkEntity>)

    /**
     * 使用事务来确保任务和其所有分块被原子性地插入。
     * 这是开始一个新下载时应该调用的方法。
     */
    @Transaction
    suspend fun insertNewTaskWithChunks(
        task: DownloadTaskEntity,
        chunks: List<DownloadChunkEntity>
    ) {
        insertTask(task)
        insertChunks(chunks)
    }

    /**
     * 根据 URL 和分块索引，更新指定分块的下载进度。
     * 这是下载过程中最常被调用的方法。
     * @param taskUrl 任务的 URL
     * @param chunkIndex 分块的索引
     * @param downloadedBytes 新的已下载字节数
     */
    @Query("UPDATE download_chunks SET downloaded_bytes = :downloadedBytes WHERE task_url = :taskUrl AND chunk_index = :chunkIndex")
    suspend fun updateChunkProgress(taskUrl: String, chunkIndex: Int, downloadedBytes: Long)

    /**
     * 根据 URL 查找一个完整的下载任务，包括其所有的分块信息。
     * 这是恢复下载时首先要调用的方法。
     */
    @Transaction
    @Query("SELECT * FROM download_tasks WHERE url = :url")
    suspend fun getTaskWithChunks(url: String): DBTaskWithChunks?

    /**
     * 根据 URL 删除一个下载任务及其所有关联的分块（因为设置了级联删除）。
     * 当下载完成、失败或需要重新开始时调用。
     */
    @Query("DELETE FROM download_tasks WHERE url = :url")
    suspend fun deleteTaskByUrl(url: String)
}