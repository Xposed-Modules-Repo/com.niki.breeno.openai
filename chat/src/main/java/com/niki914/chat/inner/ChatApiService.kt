package com.niki914.chat.inner

import com.google.gson.Gson
import com.niki914.chat.beans.ChatCompletionRequest
import com.niki914.chat.beans.ChatCompletionResponse
import com.niki914.core.logD
import com.niki914.net.DynamicOkhttpClientManager
import com.niki914.net.beans.StreamNetResult
import com.niki914.net.request.requestStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

internal class ChatApiService {

    private val okHttpClient by lazy { DynamicOkhttpClientManager.okHttpClient }
    private val gson by lazy { Gson() }

    private fun buildRequest(block: Request.Builder.() -> Unit = { }): Request {
        return Request.Builder()
            .apply(block)
            .url("https://okhttp.interceptor.will.update.this") // <--- 占位符，最后会被配置项动态修改
            .build()
    }

    /**
     * 实现无传参的 GET 请求，用于预连接功能。
     * 该请求将使用异步方式 (enqueue) 发起，即使返回 404 状态码也是可接受的。
     */
    fun preConnect() {
        runCatching {
            // 1. 构建 HTTP 请求
            val request = buildRequest {
                get()
            }

            // 2. 发起异步请求。
            // enqueue() 是非阻塞的，请求会在后台线程执行，并通过 Callback 回调结果。
            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    logD("pre-connect: ${e.message}")
                }

                override fun onResponse(call: Call, response: Response) {
                    logD("pre-connect: ${response.code}")
                    response.close() // 关闭响应体，释放连接资源
                }
            })
        }
    }

    suspend fun chat(
        apiKey: String,
        requestBody: ChatCompletionRequest
    ): Flow<StreamNetResult<ChatCompletionResponse>> {
        return try {
            val headers = mapOf(
                "Authorization" to "Bearer $apiKey",
                "Content-Type" to "application/json"
            )

            // 1. 序列化请求体
            val requestBodyJson = gson.toJson(requestBody)
            val body = requestBodyJson.toRequestBody("application/json".toMediaType())

            // 2. 构建请求
            val request = buildRequest {
                headers(headers.toHeaders())
                post(body)
            }

            // 3. 发起请求并执行
            // execute() 是一个阻塞调用，但在 suspend 函数中是安全的
            val response = okHttpClient.newCall(request).execute()

            // 4. 将 okhttp3.Response 传递给流式处理函数
            response.requestStream<ChatCompletionResponse>()
        } catch (t: Throwable) {
            flowOf(
                StreamNetResult.Start,
                StreamNetResult.Complete(t)
            )
        }
    }
}