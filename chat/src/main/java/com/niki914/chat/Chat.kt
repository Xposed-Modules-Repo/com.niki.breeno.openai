package com.niki914.chat

import com.niki914.chat.ChatConfigBuilder.Companion.initialChatConfig
import com.niki914.chat.beans.ChatCompletionRequest
import com.niki914.chat.beans.ChatEvent
import com.niki914.chat.beans.Message
import com.niki914.chat.beans.ToolDefinition
import com.niki914.chat.inner.ChatConfig
import com.niki914.chat.inner.ChatStreamProcessor
import com.niki914.chat.inner.DynamicChatConfigHolder
import com.niki914.core.logD
import com.niki914.net.DynamicOkhttpClientManager
import kotlinx.coroutines.flow.Flow

// TODO 重构
class Chat(
    apiKey: String,
    modelName: String,
    prompt: String? = null,
    tools: List<ToolDefinition>? = null
) {
    private val processor by lazy { ChatStreamProcessor() }

    private val initialConfig = initialChatConfig(
        apiKey, modelName, prompt, tools
    )

    private val dynamicChatConfigHolder = DynamicChatConfigHolder(initialConfig)

    private val systemMessage: Message?
        get() {
            val config = getConfig()
            return if (config.prompt.isNullOrBlank())
                null
            else
                Message.System(config.prompt)
        }

    private val _history: MutableList<Message> = mutableListOf()
    val history: List<Message>
        get() = _history.toList()

    fun preConnect() = processor.preConnect()

//    fun exampleUpdateConfig() {
//        updateConfig {
//            apiKey = ""
//            modelName = ""
//            prompt = null
//            tools = listOf<ToolDefinition>(
//                function(
//                    name = "getCurrentWeather",
//                    description = "天气查询",
//                    parameters = FunctionParameters(
//                        type = "object",
//                        properties = mapOf(
//                            "location" to PropertyDefinition(
//                                type = "string",
//                                description = "城市名，例如：北京"
//                            )
//                        ),
//                        required = listOf("location") // 明确指定 required 参数
//                    )
//                ) as ToolDefinition
//            )
//            network {
//                baseUrl = "https://a.com"
//                httpProxy("1.1.1.1", 8080)
//            }
//        }
//    }

    fun updateConfig(block: ChatConfigBuilder.() -> Unit) {
        val oldConfig = dynamicChatConfigHolder.getConfig() // 获取旧配置
        dynamicChatConfigHolder.update(block) // 更新 ChatConfig

        // 获取新的 ChatConfig
        val newConfig = dynamicChatConfigHolder.getConfig()

        // 如果 NetworkConfig 发生变化，则同步更新 DynamicRetrofitManager
        if (oldConfig.netConfig != newConfig.netConfig) {
            DynamicOkhttpClientManager.updateConfig(newConfig.netConfig)
        }
    }

    fun append(vararg message: Message) {
        _history.addAll(message)
    }

    fun clear() {
        _history.clear()
    }

    suspend fun sendMessages(messages: List<Message>): Flow<ChatEvent> {
        return streaming(listOf(systemMessage) + messages)
    }

    suspend fun sendMessage(vararg message: Message? = arrayOf(null)): Flow<ChatEvent> {
        message.toList().forEach {
            it?.let { msg ->
                append(msg)
            }
        }
        return streaming(listOf(systemMessage) + _history.toList())
    }

    private suspend fun streaming(
        messages: List<Message?>
    ): Flow<ChatEvent> {
        val config = getConfig()
        logD(config.toString())
        return processor.streaming( // service.chat 是安全的，内部有异常捕捉
            config.apiKey,
            ChatCompletionRequest(
                model = config.modelName,
                messages = messages.filterNotNull(),
                tools = config.tools
            )
        )
    }

    private fun getConfig(): ChatConfig {
        return dynamicChatConfigHolder.getConfig()
    }
}