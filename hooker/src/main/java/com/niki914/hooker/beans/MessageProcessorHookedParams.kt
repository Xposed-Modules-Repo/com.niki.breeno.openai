package com.niki914.hooker.beans

data class MessageProcessorHookedParams(
    val json: String?,
    val messageProcessorInstance: Any?,
    val lastConversationInfo: Any?
)