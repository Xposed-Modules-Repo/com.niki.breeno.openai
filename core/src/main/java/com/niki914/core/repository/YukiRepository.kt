package com.niki914.core.repository

import com.highcapable.yukihookapi.hook.xposed.prefs.YukiHookPrefsBridge
import com.niki914.core.Key
import com.niki914.core.logD

@Deprecated("不总是正常工作")
abstract class YukiRepository {
    abstract val yukiHookPrefsBridge: YukiHookPrefsBridge

    inline fun <reified T> get(key: Key): T {
        logD("xprefs 状态: " + yukiHookPrefsBridge.isPreferencesAvailable)
        if (yukiHookPrefsBridge.isPreferencesAvailable && !yukiHookPrefsBridge.contains(key.keyId)) {
            set(key, key.default)
        }
        return when (key.default) {
            is String -> yukiHookPrefsBridge.getString(key.keyId, key.default)
            is Boolean -> yukiHookPrefsBridge.getBoolean(key.keyId, key.default)
            is Int -> yukiHookPrefsBridge.getInt(key.keyId, key.default)
            is Long -> yukiHookPrefsBridge.getLong(key.keyId, key.default)
            is Float -> yukiHookPrefsBridge.getFloat(key.keyId, key.default)
            else -> throw IllegalArgumentException("yuki prefs 不支持的类型: ${key.default} - $key")
        } as T
    }

    fun <T> set(key: Key, value: T) {
        yukiHookPrefsBridge.edit {
            when (value) {
                is String -> putString(key.keyId, value)
                is Boolean -> putBoolean(key.keyId, value)
                is Int -> putInt(key.keyId, value)
                is Long -> putLong(key.keyId, value)
                is Float -> putFloat(key.keyId, value)
                else -> logD("yuki prefs 不支持的类型: $value for $key")
            }
        }
    }
}