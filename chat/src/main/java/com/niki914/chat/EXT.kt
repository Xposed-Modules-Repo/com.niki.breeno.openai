package com.niki914.chat

import com.niki914.chat.beans.FunctionParameters
import com.niki914.chat.beans.FunctionTool
import com.niki914.chat.beans.ToolDefinition

// 简化一个函数工具的定义
fun function(name: String, description: String, parameters: FunctionParameters): ToolDefinition {
    return ToolDefinition(
        function = FunctionTool(
            name, description, parameters
        )
    )
}

fun toolsOf(vararg toolDefinition: ToolDefinition): List<ToolDefinition> {
    return listOf(*toolDefinition)
}