package com.niki914.chat.beans

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

/**
 * 表示聊天对话中的一条消息
 */
@Keep
sealed class Message(val role: String) {
    abstract val content: String?

    @Keep
    data class System(override val content: String) : Message("system")

    @Keep
    data class User(override val content: String) : Message("user")

    @Keep
    data class Assistant(
        override val content: String?,
        @SerializedName("tool_calls") val toolCalls: List<ToolCall>? = null
    ) : Message("assistant")

    @Keep
    data class Tool(
        @SerializedName("tool_call_id") val toolCallId: String,
        val name: String,
        override val content: String
    ) : Message("tool")
}