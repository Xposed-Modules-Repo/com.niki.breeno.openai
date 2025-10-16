package com.niki914.breeno.viewmodel.bean

import com.niki914.chat.beans.Message

data class MessagePair(
    val userMessage: Message.User,
    val aiMessage: List<Message> = emptyList(), // 考虑工具调用情况
    val state: State = State.WAITING,
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class State {
        WAITING,
        ERROR,
        SUCCESS
    }

    constructor(
        userMsg: String = "",
        aiMsg: String = "",
        state: State = State.SUCCESS,
    ) : this(
        Message.User(userMsg),
        listOf(Message.Assistant(aiMsg)),
        state
    )

    companion object {
        fun newPendingPair(message: String): MessagePair {
            return MessagePair(
                userMessage = Message.User(message),
                aiMessage = emptyList(),
                state = State.WAITING,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}