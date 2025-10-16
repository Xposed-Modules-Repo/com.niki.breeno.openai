package com.niki914.tool.call.models

import android.app.Application
import androidx.annotation.Keep
import com.niki914.chat.beans.FunctionParameters
import com.niki914.chat.beans.FunctionTool
import com.niki914.chat.beans.PropertyDefinition
import com.niki914.chat.beans.ToolDefinition
import com.niki914.core.logD
import com.niki914.core.logE
import com.niki914.core.logV
import com.niki914.core.parseToJsonObj
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// 使用 keep 来防止精简和混淆，使用 internal 来控制外部的使用
// 使用 反射获取 sealed 子类并且是 object 的对象来注册一个工具

/**
 * 主要用于封装请求 LLMs Api 用的工具的定义（以 map 的形式直接写入请求体）以及实际的调用逻辑
 */
@Keep
internal sealed class BaseToolModel {
    companion object {
        // 创建一个 Json 实例，用于序列化 Map 到 JsonObject

        fun exceptionCaughtJson(message: String? = null): JsonObject = buildJsonObject {
            put("error", "工具调用时出错: $message")
        }

        fun illegalArgumentJson(msg: String? = null): JsonObject = buildJsonObject {
            put("error", msg ?: "不合法的传参")
        }

        fun toolNotFoundJson(): JsonObject = buildJsonObject {
            put("error", "尝试调用未定义的工具")
        }

        fun simpleResultJson(result: String): JsonObject = buildJsonObject {
            put("result", result)
        }
    }

    abstract val name: String
    abstract val description: String
    open val type: String = "object"

    open val properties: Map<String, PropertyDefinition> = mapOf()
    open val required: List<String> = emptyList()

    private val parameters: FunctionParameters = FunctionParameters(
        type = type,
        properties = properties,
        required = required
    )

    val tool: ToolDefinition
        get() {
            return ToolDefinition(
                function = FunctionTool(
                    name = name,
                    description = description,
                    parameters = parameters
                )
            )
        }

    /**
     * 调用工具，并处理超时和异常。
     * @param argsJson 字符串形式的 JSON 参数。
     * @param timeout 可选的超时时间（毫秒）。
     * @return 工具执行结果的 JsonObject，或 null（如果超时）或包含错误信息的 JsonObject。
     */
    suspend fun call(
        argsJson: String,
        application: Application? = null,
        timeout: Long? = null,
        extraParams: Map<String, Any?> = mapOf()
    ): JsonObject {
        return try {
            val argsJsonObject: JsonObject = argsJson.parseToJsonObj()
                ?: return illegalArgumentJson()

            val extrasJsonObject = buildJsonObject {
                extraParams.forEach { (key, value) ->
                    logD("parsing extra: [$key, $value]")
                    when (value) {
                        null -> put(key, JsonNull)
                        is String -> put(key, value)
                        is Number -> put(key, JsonPrimitive(value))
                        is Boolean -> put(key, JsonPrimitive(value))
                        is JsonElement -> put(key, value)
                        else -> put(key, JsonPrimitive(value.toString()))
                    }
                }
            }

            logV("调用工具: $name")
            if (timeout == null || timeout < 1) {
                callInternal(argsJsonObject, extrasJsonObject, application)
            } else {
                withTimeoutOrNull(timeout) {
                    callInternal(argsJsonObject, extrasJsonObject, application)
                } ?: exceptionCaughtJson("工具执行超时")
            }
        } catch (t: Throwable) {
            logE("Tool[$name]#call", t)
            exceptionCaughtJson(t.message)
        }
    }

    /**
     * 内部工具调用逻辑，由子类实现。
     * @param args 工具的 JsonObject 参数。
     * @return 工具执行结果的 JsonObject。
     */
    protected abstract suspend fun callInternal(
        args: JsonObject,
        extras: JsonObject,
        application: Application?
    ): JsonObject

    protected fun JsonObjectBuilder.putType(type: String) {
        put("type", type)
    }

    protected fun JsonObjectBuilder.putDescription(description: String) {
        put("description", description)
    }

    // 函数实例获取，没什么用
//    fun getInstance() = ::call
}