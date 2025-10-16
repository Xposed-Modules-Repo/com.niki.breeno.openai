package com.niki914.chat.inner

import com.niki914.chat.beans.ToolDefinition
import com.niki914.net.inner.NetworkConfig

internal data class ChatConfig(
    val apiKey: String,
    val modelName: String,
    val prompt: String?,
    val tools: List<ToolDefinition>?,

    val netConfig: NetworkConfig
)