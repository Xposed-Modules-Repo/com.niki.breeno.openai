package com.niki914.net.request

import com.google.gson.Gson
import com.niki914.core.logD
import com.niki914.core.logE
import com.niki914.core.logV
import com.niki914.net.beans.StreamNetResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import okhttp3.Response
import java.io.BufferedReader

/**
 * 流式请求 flow
 *
 * 扩展函数直接作用于 okhttp3.Response
 * 发送状态: Start, Data, CaughtError, Complete
 *
 * TODO 使用 Okhttp 自带的 SSE 实现
 */
inline fun <reified T> Response.requestStream(): Flow<StreamNetResult<T>> {
    logE("请求URL: ${this.request.url}") // 直接访问

    if (!isSuccessful) {
        return flowOf(
            StreamNetResult.Start,
            StreamNetResult.Complete(
                Exception("请求失败\n状态码: ${code}\n请求结果:`${body?.string()}`")
            ),
        )
    }

    // 这部分逻辑不需要改变，因为 body() 返回 ResponseBody，其 byteStream() 返回的是标准 Java InputStream
    val reader = try {
        body?.byteStream()?.bufferedReader()
    } catch (t: Throwable) {
        null
    } ?: return flowOf(
        StreamNetResult.Start,
        StreamNetResult.Complete(Exception("返回体/比特流为空")),
    )

    // 2. 使用 flow builder 构建核心数据流
    return flow {
        parseSseStream<T>(reader) // parseSseStream 内部逻辑不变
    }.onStart { emit(StreamNetResult.Start) }
        .catch { t ->
            when (t) {
                is SSEExceptionWithRawData -> {
                    emit(StreamNetResult.RawData(t.message ?: ""))
                }
            }
            emit(StreamNetResult.CaughtError(t))
//            emit(StreamNetResult.Complete(t))
        }
        .onCompletion { cause ->
            runCatching {
                reader.close()
            }
            emit(StreamNetResult.Complete(cause))
        }
}

/**
 * 职责: 读取 BufferedReader, 解析 SSE 格式, 并通过 FlowCollector 发射 Data 或 Error
 */
suspend inline fun <reified T> FlowCollector<StreamNetResult<T>>.parseSseStream(reader: BufferedReader) =
    apply {
        logD("start process lines of reader: $reader")
        reader.useLines { lines ->
            lines.forEach { line ->
                logV("line: $line")
                when {
                    line.isEmpty() || line.startsWith(":") -> return@forEach

                    line.startsWith("data:") -> {
                        val dataString = line.substring(5).trim()
                        if (dataString == "[DONE]") {
                            return@useLines
                        }

                        try {
                            val streamData = dataString.toJsonClass<T>()
                            emit(StreamNetResult.Data(streamData))
                        } catch (e: Exception) {
                            logD("无法解析: $dataString")
                            throw SSEExceptionWithRawData(dataString)
                        }
                    }

                    else ->
                        emit(StreamNetResult.RawData(line))
                }
            }
        }
//        emit(StreamNetResult.Complete(null))
    }

val gson by lazy { Gson() }

class SSEExceptionWithRawData(msg: String) : Exception(msg)

inline fun <reified T> String.toJsonClass(): T? {
    return try {
        gson.fromJson(this, T::class.java)
    } catch (_: Exception) {
        null
    }
}
