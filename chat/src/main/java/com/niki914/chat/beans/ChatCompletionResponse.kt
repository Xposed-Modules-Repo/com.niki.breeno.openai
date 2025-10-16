package com.niki914.chat.beans

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * 简化的流式聊天补全响应体。
 * 仅包含你最关心的 `choices` 列表，用于提取模型生成的内容。
 */
@Keep
internal data class ChatCompletionResponse(
    val choices: List<Choice?>?
)

/**
 * 简化的模型选择。
 * 仅包含 `delta`，用于获取流式内容。
 */
@Keep
internal data class Choice(
    val delta: Delta? // 包含本次流中新增的内容片段
)

/**
 * 简化的内容增量。
 * 仅包含实际的模型生成内容。
 */
@Keep
internal data class Delta(
    val content: String?, // 模型生成的内容片段
    @SerializedName("tool_calls") val toolCalls: List<ToolCall?>? // 新增：模型生成的工具调用列表
)

/**
 * 简化的工具调用。
 * 包含工具调用的 ID 和函数信息。
 */
@Keep
data class ToolCall(
    val id: String?, // 工具调用的唯一 ID
    val type: String?, // 工具类型，通常是 "function"
    val function: FunctionCall? // 函数调用详情
)

/**
 * 简化的函数调用详情。
 * 包含函数名和参数。
 */
@Keep
data class FunctionCall(
    val name: String?, // 函数名
    val arguments: String? // 函数参数，通常是 JSON 字符串
)