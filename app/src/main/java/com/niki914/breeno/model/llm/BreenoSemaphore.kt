package com.niki914.breeno.model.llm


import com.niki914.core.logV
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * 对话管理基类，维护信号量来解决异步条件问题
 *
 * 异步问题：~~自定义大模型的回答必须在获取完 context 并读取完本地配置才能开始（已通过阻塞获取 application 解决）~~
 * UI注入必须在获取到官方api的关键id后才能开始
 */
class BreenoSemaphore {
    private var callbackSemaphore = semaphoreWithNoPermits   // 控制回调条件

    @Volatile
    private var isCallbackTriggered = false

    fun isAvailable(): Boolean {
        return isCallbackTriggered
    }

    // 回调被触发时调用
    fun enable() {
        synchronized(this) {
            if (!isCallbackTriggered) {
                isCallbackTriggered = true
                logV("breeno callback triggered")
                callbackSemaphore.setTo(1)
            }
        }
    }

    /**
     * 通过这个函数执行操作来进行注入的控制
     */
    suspend fun withPermit(action: suspend () -> Unit) {
        callbackSemaphore.withPermit {
            logV("breeno UI 注入 取得锁")
            try {
                action()
                logV("breeno UI 注入结束")
            } finally {
                // action完成后，重置回调状态
                synchronized(this) {
                    isCallbackTriggered = false
                    logV("breeno callback 信号量重置")
                    callbackSemaphore.setTo(0)
                }
            }
        }
    }

    private val semaphoreWithNoPermits: Semaphore
        get() {
            val s = Semaphore(1).apply { setTo(0) }
            return s
        }

    /**
     * 直接实例化一个 permits 为 0 的信号量会被抛出异常，所以需要这样做
     *
     * 而由于每一个 semaphoreWithNoPermits 实例化互不影响，所以不存在线程不安全问题
     */
    private fun Semaphore.setTo(num: Int) {
        if (num == 0) return
        while (availablePermits != num) {
            if (availablePermits > num) {
                tryAcquire()
            } else {
                release()
            }
        }
    }
}