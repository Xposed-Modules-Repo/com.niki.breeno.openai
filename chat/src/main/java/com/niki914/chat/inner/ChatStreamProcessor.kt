package com.niki914.chat.inner

import com.niki914.chat.beans.ChatCompletionRequest
import com.niki914.chat.beans.ChatCompletionResponse
import com.niki914.chat.beans.ChatEvent
import com.niki914.core.logD
import com.niki914.core.logE
import com.niki914.net.beans.StreamNetResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow

/**
 * 在 Streaming 的基础上再转换一层 flow
 */
internal class ChatStreamProcessor {
    private val service by lazy { ChatApiService() }

    fun preConnect() = service.preConnect()

    suspend fun streaming(apiKey: String, requestBody: ChatCompletionRequest): Flow<ChatEvent> {
        return service.chat(
            apiKey, requestBody
        ).convertToChatStream()
    }

    /**
     * 封装大模型流式请求，处理ToolCall并发出统一的ChatStreamResult。
     */
    private fun Flow<StreamNetResult<ChatCompletionResponse>>.convertToChatStream(): Flow<ChatEvent> =
        flow {
            val toolCallHandler = ToolCallHandler.bindTo(this) // 自动发送工具意图

            val rawDataSB = StringBuilder()

            catch { t ->
                logE("Stream error: ", t)
            }.collect { result ->
                logD(result.toString())
                when (result) {
                    is StreamNetResult.Start -> {
                        emit(ChatEvent.Started)
                    }

                    is StreamNetResult.Data -> {
                        val delta = result.data?.choices?.getOrNull(0)?.delta
                        delta?.content?.let {
                            if (it.isNotEmpty()) {
                                emit(ChatEvent.Content(it))
                            }
                        }
                        delta?.toolCalls?.forEach { toolCallDelta ->
                            toolCallDelta?.let {
                                toolCallHandler.handle(it)
                            }
                        }
                    }

                    is StreamNetResult.Complete -> {
                        result.throwable?.let {
                            logE("Stream error: ${it.stackTraceToString()}")
                        }
                        if (result.throwable == null && rawDataSB.isNotEmpty()) { // url 错误
                            emit(ChatEvent.Completed(Exception("数据解析异常，请检查 baseurl 是否正确，元数据:\n`$rawDataSB`")))
                        } else {
                            emit(ChatEvent.Completed(result.throwable))
                        }
                    }

                    is StreamNetResult.CaughtError -> {
                        logE("Stream error: ${result.throwable.stackTraceToString()}")
                    }

                    is StreamNetResult.RawData -> {
                        if (result.raw != null) {
                            rawDataSB.append(result.raw + "\n")
                        }
                    }
                }
            }
        }.catch { t ->
            emit(ChatEvent.Completed(t))
        }
}