package com.niki914.net.request

@Deprecated("当前只用流式")
class NonStream

//@JvmName("requestExecute2")
//fun <T> requestExecute(
//    call: Call<T>,
//    callback: (NetResult<T>) -> Unit
//) = call.requestExecute(callback)
//
//@JvmName("requestEnqueue2")
//fun <T> requestEnqueue(
//    call: Call<T>,
//    callback: (NetResult<T>) -> Unit
//) = call.requestEnqueue(callback)
//
///**
// * 同步请求方法
// */
//@JvmName("requestExecute1")
//fun <T> Call<T>.requestExecute(
//    callback: (NetResult<T>) -> Unit
//) = try {
//    val response = execute()
//    handleOnResponse(response, callback)
//} catch (t: Throwable) {
//    handleOnFailure(t, callback)
//}
//
///**
// * 异步请求方法
// */
//@JvmName("requestEnqueue1")
//fun <T> Call<T>.requestEnqueue(
//    callback: (NetResult<T>) -> Unit
//) = enqueue(object : Callback<T> {
//    override fun onResponse(call: Call<T>, response: Response<T>) {
//        try {
//            handleOnResponse(response, callback)
//        } catch (t: Throwable) {
//            handleOnFailure(t, callback)
//        }
//    }
//
//    override fun onFailure(call: Call<T>, throwable: Throwable) {
//        handleOnFailure(throwable, callback)
//    }
//})
//
//
//private fun <T> Call<T>.handleOnResponse(
//    response: Response<T>?,
//    callback: (NetResult<T>) -> Unit
//) = response?.apply {
//    val url = request().url.toString()
//    when {
//        isSuccessful -> { // 成功的判定标准为: 状态码 in [200, 300)
//            logI("[${code()}] at [$url]")
//            val json = body().toPrettyJson()
//            logD("body:\n$json")
//            callback(Success(body()))
//        } // 成功
//
//        else -> {
//            logW("[${code()}] at [$url]")
//            val string = errorBody()?.string()
//            logW("error string\n$string")
//            callback(Error(code(), string))
//        } // 其他失败情况
//    }
//} ?: callback(Error(null, "retrofit response is null"))
//
//private fun <T> Call<T>.handleOnFailure(
//    t: Throwable?,
//    callback: (NetResult<T>) -> Unit
//) {
//    if (t == null) return
//
//    val url = request().url.toString()
//    val throwableString = t.stackTraceToString()
//    logE("[exception] at [$url]")
//    logE("throwable:\n$throwableString")
//    callback(Error(null, throwableString))
//}