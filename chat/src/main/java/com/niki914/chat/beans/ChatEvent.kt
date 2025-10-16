package com.niki914.chat.beans

/**
 * 封装流式聊天补全结果的密封类。
 */
sealed class ChatEvent {

    data object Started : ChatEvent()

    /**
     * 表示流中返回的文本内容。
     * @param content 文本内容片段。
     */
    data class Content(val content: String) : ChatEvent()

    /**
     * 表示已完成解析的工具调用意图。
     * @param toolCall 已完成的工具调用对象。
     */
    data class ToolCallIntent(val toolCall: ToolCall) : ChatEvent()

    /**
     * 表示流式传输已完成。
     */
    data class Completed(
        val throwable: Throwable? = null
    ) : ChatEvent() {
        val isSuccess: Boolean
            get() = throwable == null
    }
}