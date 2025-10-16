package com.niki914.chat

import com.niki914.chat.beans.ToolDefinition
import com.niki914.chat.inner.ChatConfig
import com.niki914.net.NetworkConfigBuilder

/**
 * ChatConfig 的 DSL 构建器
 */
class ChatConfigBuilder {

    companion object {
        /**
         * DSL 构建 ChatConfig
         */
        internal fun chatConfig(block: ChatConfigBuilder.() -> Unit = {}): ChatConfig {
            return ChatConfigBuilder().apply(block).build()
        }

        internal fun initialChatConfig(
            apiKey: String,
            modelName: String,
            prompt: String? = null,
            tools: List<ToolDefinition>? = null
        ): ChatConfig {
            return ChatConfigBuilder().apply {
                this.apiKey = apiKey
                this.modelName = modelName
                this.prompt = prompt
                this.tools = tools
            }.build()
        }

        internal fun fromConfig(config: ChatConfig): ChatConfigBuilder {
            return ChatConfigBuilder().apply {
                apiKey = config.apiKey
                modelName = config.modelName
                prompt = config.prompt
                tools = config.tools
                netConfigBuilder = NetworkConfigBuilder.fromConfig(config.netConfig)
            }
        }
    }

    var apiKey: String = ""
    var modelName: String = ""
    var prompt: String? = null
    var tools: List<ToolDefinition>? = null

    private var netConfigBuilder: NetworkConfigBuilder = NetworkConfigBuilder() // 新增

    override fun toString(): String {
        return "ChatConfig[key: ${
            apiKey.ellipsisIfExceeded(5)
        }, model: $modelName, prompt: ${
            prompt?.ellipsisIfExceeded(
                10
            )
        }, net: $netConfigBuilder]"
    }

    private fun String.ellipsisIfExceeded(exceededLength: Int): String {
        return if (length > exceededLength) {
            this.take(exceededLength) + "..."
        } else {
            this
        }
    }

    /**
     * 用于配置 NetworkConfig 的 DSL 块
     */
    fun network(block: NetworkConfigBuilder.() -> Unit) { // 新增
        netConfigBuilder.apply(block)
    }

    internal fun build(): ChatConfig {
        return ChatConfig(apiKey, modelName, prompt, tools, netConfigBuilder.build()) // 修改
    }
}