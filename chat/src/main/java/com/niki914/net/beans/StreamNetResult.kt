package com.niki914.net.beans

/**
 * 流式请求结果封装
 */
sealed class StreamNetResult<out T> {
    data object Start : StreamNetResult<Nothing>()
    data class Data<out T>(val data: T?) : StreamNetResult<T>()
    data class RawData(val raw: String?) : StreamNetResult<Nothing>() // 当 gson 解析失败
    data class CaughtError(val throwable: Throwable) : StreamNetResult<Nothing>() // 被捕捉的异常在这
    data class Complete(
        val throwable: Throwable? = null // 致命的，会终结 flow 的异常被抛出在这里
    ) : StreamNetResult<Nothing>() {
        val isSuccess: Boolean
            get() = throwable == null
    }
}