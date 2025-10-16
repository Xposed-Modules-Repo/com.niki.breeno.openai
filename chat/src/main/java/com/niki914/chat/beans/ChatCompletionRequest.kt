package com.niki914.chat.beans

import androidx.annotation.Keep

/*
    val requestBody = ChatCompletionRequest(
        model = modelName,
        messages = messages,
        tools = listOf(
            ToolDefinition(
                type = "function",
                function = FunctionTool(
                    name = "getCurrentWeather",
                    description = "天气查询",
                    parameters = FunctionParameters(
                        type = "object",
                        properties = mapOf(
                            "location" to PropertyDefinition(
                                type = "string",
                                description = "城市名，例如：北京"
                            )
                        ),
                        required = listOf("location") // 明确指定 required 参数
                    )
                )
            )
        )
    )
 */


// 整个聊天完成请求的 Body
@Keep
internal data class ChatCompletionRequest(
    val model: String,
    val messages: List<Message>,
    val tools: List<ToolDefinition>? = null // 工具列表，可为空
) {
    val stream: Boolean = true
}

// 工具的定义，用于请求中的 "tools" 字段
@Keep
data class ToolDefinition(
    val function: FunctionTool
) {
    val type: String = "function" // 工具类型，例如 "function"
}

// 函数工具的详细定义
@Keep
data class FunctionTool(
    val name: String,        // 函数名称，例如 "getCurrentWeather"
    val description: String, // 函数描述，例如 "天气查询"
    val parameters: FunctionParameters // 函数参数的 JSON Schema
)

// 函数参数的 JSON Schema 定义
@Keep
data class FunctionParameters(
    val type: String, // 参数类型，例如 "object"
    val properties: Map<String, PropertyDefinition>, // 参数属性的映射
    val required: List<String>? = null // 必须参数列表，可为空
)

// 单个参数属性的定义，例如 "location"
@Keep
data class PropertyDefinition(
    val type: String,        // 属性类型，例如 "string"
    val description: String // 属性描述
)