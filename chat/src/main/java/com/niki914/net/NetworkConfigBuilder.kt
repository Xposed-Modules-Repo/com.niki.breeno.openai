package com.niki914.net

import com.niki914.net.inner.NetworkConfig
import okhttp3.Interceptor
import java.net.Proxy

/**
 * NetworkConfig 的 DSL 构建器
 */
class NetworkConfigBuilder {
    var baseUrl: String = ""
    var readTimeout: Long = 30L
    var connectTimeout: Long = 30L
    var writeTimeout: Long = 30L
    var callTimeout: Long = 30L
    var proxy: Proxy? = null

    override fun toString(): String {
        return "NetworkConfig[url: $baseUrl, timeout: $connectTimeout, proxy: $proxy]"
    }

    private val interceptors = mutableListOf<Interceptor>()

    companion object {
        /**
         * DSL 构建 NetworkConfig
         */
        internal fun networkConfig(block: NetworkConfigBuilder.() -> Unit = {}): NetworkConfig {
            return NetworkConfigBuilder().apply(block).build()
        }

        internal fun fromConfig(config: NetworkConfig): NetworkConfigBuilder {
            return NetworkConfigBuilder().apply {
                baseUrl = config.baseUrl
                readTimeout = config.readTimeout
                connectTimeout = config.connectTimeout
                writeTimeout = config.writeTimeout
                callTimeout = config.callTimeout
                proxy = config.proxy
            }
        }
    }

//    fun baseUrl(url: String) {
//        this.baseUrl = url
//    }
//
//    fun readTimeout(seconds: Long) {
//        this.readTimeout = seconds
//    }
//
//    fun connectTimeout(seconds: Long) {
//        this.connectTimeout = seconds
//    }
//
//    fun writeTimeout(seconds: Long) {
//        this.writeTimeout = seconds
//    }
//
//    fun callTimeout(seconds: Long) {
//        this.callTimeout = seconds
//    }
//
//    fun proxy(proxy: Proxy) {
//        this.proxy = proxy
//    }

    fun socksProxy(host: String, port: Int) {
        this.proxy = NetworkConfig.createSocksProxy(host, port)
    }

    fun httpProxy(host: String, port: Int) {
        this.proxy = NetworkConfig.createHttpProxy(host, port)
    }

//    fun addInterceptor(interceptor: Interceptor) {
//        interceptors.add(interceptor)
//    }
//
//    fun interceptors(vararg interceptors: Interceptor) {
//        this.interceptors.addAll(interceptors)
//    }
//
//    fun interceptors(interceptors: List<Interceptor>) {
//        this.interceptors.addAll(interceptors)
//    }

    internal fun build(): NetworkConfig {
        return NetworkConfig(
            baseUrl = baseUrl,
            readTimeout = readTimeout,
            connectTimeout = connectTimeout,
            writeTimeout = writeTimeout,
            callTimeout = callTimeout,
            proxy = proxy,
            interceptors = interceptors.toList()
        )
    }
}