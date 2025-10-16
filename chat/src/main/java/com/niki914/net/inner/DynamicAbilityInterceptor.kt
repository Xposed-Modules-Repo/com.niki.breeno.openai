package com.niki914.net.inner

import okhttp3.Interceptor
import okhttp3.Response
import java.util.concurrent.TimeUnit

/**
 * 动态应用连接偏好
 * 将占位符URL替换为配置中的真实URL，并应用动态超时设置
 */
internal class DynamicAbilityInterceptor(
    private val configHolder: DynamicNetworkConfigHolder
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val currentConfig = configHolder.getConfig()
        val originalRequest = chain.request()

        // 1. 直接使用配置中的完整URL替换占位符URL
        val newRequest = originalRequest.newBuilder()
            .url(currentConfig.baseUrl) // 配置中的完整目标URL
            .build()

        // 2. 动态应用超时设置
        return chain
            .withConnectTimeout(currentConfig.connectTimeout.toInt(), TimeUnit.SECONDS)
            .withReadTimeout(currentConfig.readTimeout.toInt(), TimeUnit.SECONDS)
            .withWriteTimeout(currentConfig.writeTimeout.toInt(), TimeUnit.SECONDS)
            .proceed(newRequest)
    }
}