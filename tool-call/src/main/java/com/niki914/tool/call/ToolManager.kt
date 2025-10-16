package com.niki914.tool.call

import android.app.Application
import com.niki914.chat.beans.ToolCall
import com.niki914.chat.beans.ToolDefinition
import com.niki914.core.getSealedChildren
import com.niki914.core.logV
import com.niki914.core.parseToString
import com.niki914.tool.call.models.BaseToolModel

/**
 * 所有的函数 model 都被反射运行时添加
 *
 * 外部通过此对象获取工具定义以及调用函数
 */
object ToolManager {
    private val toolModelMap = mutableMapOf<String, BaseToolModel>()

    init {
        reflectAll().forEach { model ->
            toolModelMap[model.name] = model
        }

        logV("已注册函数: ${toolModelMap.keys}")
    }

    /**
     * 可选工具筛选，配合应用设置项可以控制可用的工具
     */
    fun getTools(filter: (name: String) -> Boolean = { true }): List<ToolDefinition> {
        return toolModelMap.filter { filter(it.key) }.map { it.value.tool }
    }

    private fun reflectAll(): List<BaseToolModel> {
        return getSealedChildren<BaseToolModel> { kClass ->
            kClass.objectInstance // 获取那些定义为 `object xxx: BaseToolModel` 的单例
        }
    }

    /**
     * 统一函数调用入口
     *
     * 保证输出一个 json 字串
     */
    suspend fun executeFunction(
        toolCall: ToolCall,
        application: Application? = null,
        timeout: Long? = null,
        extraParams: Map<String, Any?> = mapOf()
    ): String {
        val toolName = toolCall.function?.name
        val toolArgsJsonString = toolCall.function?.arguments // 这是一个 JSON 字符串

        logV("调用工具: $toolName 参数: $toolArgsJsonString")

        val model = toolModelMap[toolName]

        /**
         * 由于项目之前使用 https://github.com/Aallam/openai-kotlin 实现，所以用的是 kotlinx.serialization.json，但是现在改成自行实现网络请求了，没来得及改
         */
        val responseJsonObject =
            model?.call(toolArgsJsonString ?: "{}", application, timeout, extraParams)
                ?: BaseToolModel.toolNotFoundJson()
        val responseJsonString = responseJsonObject.parseToString()

        return responseJsonString.also { r ->
            logV("工具执行结果: $r")
        }
    }
}