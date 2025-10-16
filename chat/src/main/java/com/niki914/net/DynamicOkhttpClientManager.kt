package com.niki914.net

import com.niki914.net.NetworkConfigBuilder.Companion.networkConfig
import com.niki914.net.inner.DynamicAbilityInterceptor
import com.niki914.net.inner.DynamicNetworkConfigHolder
import com.niki914.net.inner.DynamicProxySelector
import com.niki914.net.inner.NetworkConfig
import okhttp3.OkHttpClient

/**
 * OkHttp 客户端管理器
 * 职责：
 * 1. 维护一个支持动态配置（BaseUrl, Timeout, Proxy）的 OkHttpClient 单例。
 * 2. 提供更新网络配置的接口。
 * 3. 向业务层提供配置好的 OkHttpClient 实例。
 *
 * 本来是管理 retrofit 的，但是现在不用了
 */
object DynamicOkhttpClientManager {
    private val initialConfig = networkConfig()

    private val dynamicNetworkConfigHolder = DynamicNetworkConfigHolder(initialConfig)

    fun updateConfig(block: NetworkConfigBuilder.() -> Unit) {
        dynamicNetworkConfigHolder.update(block)
    }

    internal fun updateConfig(config: NetworkConfig) {
        dynamicNetworkConfigHolder.update(config)
    }

    /**
     * 向模块内部暴露配置好的 OkHttpClient 实例
     */
    internal val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(DynamicAbilityInterceptor(dynamicNetworkConfigHolder))
            .proxySelector(DynamicProxySelector(dynamicNetworkConfigHolder))
            // 注意：这里不再需要设置全局超时，因为 DynamicAbilityInterceptor 会为每个请求动态设置
            .build()
    }
}