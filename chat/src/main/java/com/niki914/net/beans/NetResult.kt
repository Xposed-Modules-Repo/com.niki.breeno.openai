package com.niki914.net.beans

@Deprecated("")
sealed class NetResult<out T> {
    class Success<out T>(val data: T?) : NetResult<T>()
    class Error(val code: Int?, val msg: String?) : NetResult<Nothing>()
}