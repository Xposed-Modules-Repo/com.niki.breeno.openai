package com.niki914.breeno.repository.base

import android.content.Context
import com.niki914.core.Key
import com.niki914.core.logRelease
import com.niki914.core.repository.ISettingsRepository

abstract class ModuleSettingRepository(context: Context) : ContentProviderRepository(context),
    ISettingsRepository {
    protected fun getStringOrDefault(key: Key): String {
        return getString(key) ?: defaultAs<String>(key)
    }

    protected fun getIntOrDefault(key: Key): Int {
        return getInt(key) ?: defaultAs<Int>(key)
    }

    protected fun getLongOrDefault(key: Key): Long {
        return getLong(key) ?: defaultAs<Long>(key)
    }

    protected fun getFloatOrDefault(key: Key): Float {
        return getFloat(key) ?: defaultAs<Float>(key)
    }

    protected fun getBooleanOrDefault(key: Key): Boolean {
        return getBoolean(key) ?: defaultAs<Boolean>(key)
    }

    private inline fun <reified T> defaultAs(key: Key): T {
        return (key.default as T).also { // 只有打日志目的
            logRelease("${key.keyId} 使用了默认值: $it")
        }
    }
}