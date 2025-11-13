package com.niki914.download

sealed interface IOResult {
    data class Success<T>(val data: T) : IOResult
    data class Error(val cause: Throwable) : IOResult

    companion object {
        fun success(msg: String): Success<String> {
            return Success<String>(msg)
        }

        inline fun <reified E : Exception> error(msg: String): Error {
            val clazz: Class<E> = E::class.java

            // 2. 获取接受 String (错误消息) 作为参数的构造函数
            // 这里假设所有目标 Exception 都有一个接受 String 的构造函数
            val constructor = clazz.getConstructor(String::class.java)

            // 3. 调用构造函数，创建 Exception 实例
            val exceptionInstance = constructor.newInstance(msg)

            // 4. 将 Exception 包装到自定义的 Error 对象中
            return Error(exceptionInstance)
        }
    }
}