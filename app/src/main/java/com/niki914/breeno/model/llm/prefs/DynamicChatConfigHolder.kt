package com.niki914.breeno.model.llm.prefs

import java.util.concurrent.atomic.AtomicReference


/**
 * 线程安全的动态配置持有者
 * 使用AtomicReference确保配置的更新和读取是原子操作，避免多线程问题。
 *
 * ai
 */
internal class DynamicOtherChatPrefsHolder(
    initialConfig: OtherChatPrefs = OtherChatPrefsBuilder.otherChatPrefs()
) {

    private val prefsRef = AtomicReference(initialConfig)

    fun getPrefs(): OtherChatPrefs {
        return prefsRef.get()
    }

    fun update(newConfig: OtherChatPrefs) {
        prefsRef.set(newConfig)
    }

    // 你甚至可以保留DSL的便利性
    fun update(block: OtherChatPrefsBuilder.() -> Unit) {
        val newConfig = OtherChatPrefsBuilder.fromPrefs(getPrefs()).apply(block).build()
        update(newConfig)
    }
}