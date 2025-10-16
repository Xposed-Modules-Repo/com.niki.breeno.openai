package com.niki914.net.inner

import okhttp3.Interceptor
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * 网络配置类，用于动态设置网络参数
 */
internal data class NetworkConfig(
    val baseUrl: String,
    val readTimeout: Long = 30L,
    val connectTimeout: Long = 30L,
    val writeTimeout: Long = 30L,
    val callTimeout: Long = 30L,
    val proxy: Proxy? = null,
    val interceptors: List<Interceptor> = emptyList()
) {
    companion object {
        /**
         * 创建 SOCKS 代理配置
         */
        fun createSocksProxy(host: String, port: Int): Proxy {
            return Proxy(Proxy.Type.SOCKS, InetSocketAddress(host, port))
        }

        /**
         * 创建 HTTP 代理配置
         */
        fun createHttpProxy(host: String, port: Int): Proxy {
            return Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port))
        }
    }
}