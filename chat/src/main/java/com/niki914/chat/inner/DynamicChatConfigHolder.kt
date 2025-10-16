package com.niki914.chat.inner

import com.niki914.chat.ChatConfigBuilder
import java.util.concurrent.atomic.AtomicReference

/**
 * 线程安全的动态配置持有者
 * 使用AtomicReference确保配置的更新和读取是原子操作，避免多线程问题。
 *
 * ai
 */
internal class DynamicChatConfigHolder(
    initialConfig: ChatConfig
) {

    private val configRef = AtomicReference(initialConfig)

    fun getConfig(): ChatConfig {
        return configRef.get()
    }

    fun update(newConfig: ChatConfig) {
        configRef.set(newConfig)
    }

    // 你甚至可以保留DSL的便利性
    fun update(block: ChatConfigBuilder.() -> Unit) {
        val newConfig = ChatConfigBuilder.fromConfig(getConfig()).apply(block).build()
        update(newConfig)
    }
}