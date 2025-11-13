package com.niki914.download.util

import com.niki914.download.protocol.DownloadChunk
import com.zephyr.log.logD
import com.zephyr.provider.TAG
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


internal class ChunkDownloadRequestMaker(
    private val client: OkHttpClient
) {

    /**
     * 下载单个数据块。
     * 职责变更：只负责发起请求并返回 Response，调用方必须负责关闭 Response。
     */
    fun request(
        url: String,
        chunk: DownloadChunk
    ): Response {
        val request = Request.Builder()
            .url(url)
            .header("Range", "bytes=${chunk.startByte}-${chunk.endByte}")
            .build()

        // 日志由调用方 (MTD) 负责，或在这里只打印最简单的日志
        logD(TAG, "    Chunk#${chunk.index}: Request, Header Range: ${request.header("Range")}")

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            response.close() // 失败时必须关闭
            throw IOException("Request failed: [${response.code} / ${response.message}] for chunk ${chunk.index}")
        }

        // 直接返回 Response，I/O 循环和校验逻辑已移至 MultiThreadDownloader
        return response
    }
}