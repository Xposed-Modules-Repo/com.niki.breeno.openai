package com.niki914.net.inner

import com.niki914.net.NetworkConfigBuilder
import java.util.concurrent.atomic.AtomicReference

/**
 * 线程安全的动态配置持有者
 * 使用AtomicReference确保配置的更新和读取是原子操作，避免多线程问题。
 */
internal class DynamicNetworkConfigHolder(
    initialConfig: NetworkConfig
) {

    private val configRef = AtomicReference(initialConfig)

    fun getConfig(): NetworkConfig {
        return configRef.get()
    }

    fun update(newConfig: NetworkConfig) {
        configRef.set(newConfig)
    }

    // 你甚至可以保留DSL的便利性
    fun update(block: NetworkConfigBuilder.() -> Unit) {
        val newConfig = NetworkConfigBuilder.fromConfig(getConfig()).apply(block).build()
        update(newConfig)
    }
}