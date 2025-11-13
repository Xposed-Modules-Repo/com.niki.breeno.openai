package com.niki914.download.util

/**
 * 节流器 - 用于限制回调或数据库写入的频率
 */
internal class Throttle(val throttleSize: Long, val callback: suspend (Long) -> Unit) {
    private var current = 0L

    suspend fun add(size: Long) {
        current += size

        if (current >= throttleSize) {
            callback(current)
            resetThrottle()
        }
    }

    /**
     * 清空数据 + 回调
     */
    suspend fun flush() {
        if (current > 0) {
            callback(current)
            resetThrottle()
        }
    }

    private fun resetThrottle() {
        current = 0
    }
}