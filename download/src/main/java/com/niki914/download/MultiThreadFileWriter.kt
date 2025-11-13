package com.niki914.download

import com.zephyr.log.logD
import com.zephyr.log.logE
import com.zephyr.provider.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile

/**
 * 为单个下载任务管理文件 I/O 操作
 *
 * 这个类在每个下载任务都应该创建一个独立的实例
 * 它在内部维护一个 RandomAccessFile 实例, 并通过 Mutex 保证并发写入的安全性
 * 实现了 Closeable 接口, 必须在使用完毕后调用 close() 方法释放资源
 *
 * @param filePath 要写入的文件的绝对路径
 */
class MultiThreadFileWriter(private val filePath: String) : Closeable {

    private var randomAccessFile: RandomAccessFile? = null
    private val fileMutex = Mutex() // 用于保护文件访问的互斥锁

    /**
     * 打开文件并预分配指定大小的空间
     *
     * 这个方法应该在任何写入操作之前被调用
     */
    suspend fun openAndAllocate(totalSize: Long): IOResult = withContext(Dispatchers.IO) {
        if (totalSize < 0) {
            return@withContext IOResult.error<IllegalArgumentException>("文件大小不能为负数")
        }
        try {
            // 确保父目录存在
            val file = File(filePath)
            file.parentFile?.mkdirs()

            // 以 "rw" 模式打开文件
            val raf = RandomAccessFile(file, "rw")
            randomAccessFile = raf

            // 预分配空间
            raf.setLength(totalSize)

            IOResult.success("文件已成功打开并预分配 $totalSize 字节空间")
        } catch (e: Exception) {
            IOResult.Error(e)
        }
    }

    /**
     * 将数据从字节数组的一部分写入文件的指定偏移量。
     * 此方法经过优化，避免了不必要的数组复制。
     *
     * @param offset 文件写入的起始偏移量。
     * @param data 包含要写入数据的字节数组（缓冲区）。
     * @param count 要从 `data` 数组中写入的字节数。
     * @return IOResult 包含操作结果或错误信息。
     */
    suspend fun write(offset: Long, data: ByteArray, count: Int): IOResult =
        withContext(Dispatchers.IO) {
            // 检查文件是否打开
            val raf = randomAccessFile
                ?: return@withContext IOResult.error<IllegalStateException>("文件未打开，请先调用 openAndAllocate()")

            if (offset < 0) {
                return@withContext IOResult.error<IllegalArgumentException>("偏移量不能为负数")
            }
            if (count <= 0) {
                return@withContext IOResult.success("无可写入数据 (count 为 $count)，未执行写入操作")
            }

            // 使用互斥锁确保并发安全
            fileMutex.withLock {
                try {
                    val fileLength = raf.length()

                    // 检查写入起始偏移量是否超出文件边界
                    if (offset >= fileLength) {
                        return@withLock IOResult.success("写入起始偏移量 $offset 超出文件边界 (总长度: $fileLength)，未写入任何数据")
                    }

                    // 1. 计算从 offset 开始，文件剩余的可写入空间
                    val remainingSpace = fileLength - offset

                    // 2. 确定本次实际要写入的字节数（取 "计划写入数" 和 "剩余空间" 中的较小值）
                    val bytesToWrite = minOf(count.toLong(), remainingSpace).toInt()

                    if (bytesToWrite <= 0) {
                        return@withLock IOResult.success("偏移量 $offset 处无剩余可写入空间，未写入任何数据")
                    }

                    // 3. 定位并直接写入字节数组的指定部分，无需创建新数组
                    raf.seek(offset)
                    raf.write(data, 0, bytesToWrite)

                    // 4. 返回成功结果
                    IOResult.success(
                        "成功在偏移量 $offset 处写入 $bytesToWrite 字节。" +
                                if (bytesToWrite < count) " (原数据 $count 字节，超出文件预分配空间的部分已截断)" else ""
                    )

                } catch (e: Exception) {
                    // 捕获并返回写入过程中可能发生的其他异常
                    IOResult.Error(e)
                }
            }
        }


    /**
     * 关闭文件句柄, 释放资源
     *
     * 在下载完成、失败或取消后必须调用此方法
     */
    override fun close() {
        try {
            randomAccessFile?.close()
            randomAccessFile = null
            logD(TAG, "MultiThreadFileWriter 实例关闭")
        } catch (e: Exception) {
            logE(TAG, "关闭文件句柄时发生错误:")
            logE(TAG, e.stackTraceToString())
        }
    }

    companion object {

        /**
         * 安全地删除文件或目录
         */
        suspend fun delete(filePath: String): IOResult = withContext(Dispatchers.IO) {
            if (filePath.isBlank()) {
                return@withContext IOResult.success("文件路径为空, 无需操作")
            }
            try {
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext IOResult.success("文件不存在, 无需删除")
                }

                if (file.deleteRecursively()) {
                    IOResult.success("文件已成功删除: $filePath")
                } else {
                    IOResult.error<Exception>("删除文件失败: $filePath")
                }
            } catch (e: Exception) {
                IOResult.Error(e)
            }
        }
    }
}