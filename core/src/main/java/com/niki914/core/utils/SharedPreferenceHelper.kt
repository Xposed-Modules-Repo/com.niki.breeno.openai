package com.niki914.core.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.niki914.core.Key
import com.niki914.core.logRelease
import java.util.concurrent.ConcurrentHashMap

/**
 * 帮助类，用于简化 SharedPreferences 的读写操作
 * 使用 datastore 存在模块和主app同时操作的问题，datastore内部抛出异常
 *
 * @param context 上下文对象，通常传入 ApplicationContext
 * @param prefName SharedPreferences 文件的名称
 */
class SharedPreferenceHelper private constructor(
    private val context: Context,
    private val prefName: String
) {
    companion object {
        // 使用 ConcurrentHashMap 来存储不同 prefName 对应的单例实例
        private val INSTANCES = ConcurrentHashMap<String, SharedPreferenceHelper>()

        /**
         * 根据 preference 文件名获取单例实例。
         * 如果实例不存在，则创建并缓存。
         *
         * @param context Context 对象，用于获取 SharedPreferences。
         * @param prefName preference 文件的名称。
         * @return 对应的 SharedPreferenceHelper 实例。
         */
        fun getInstance(context: Context, prefName: String): SharedPreferenceHelper {
            return INSTANCES.getOrPut(prefName) {
                // 如果不存在，则创建新实例并放入 Map
                SharedPreferenceHelper(context.applicationContext, prefName)
            }
        }

        fun getInstance(name: String): SharedPreferenceHelper =
            INSTANCES[name] ?: throw IllegalArgumentException("SharedPreferenceHelper 未初始化")
    }

    /**
     * 获取 SharedPreferences 实例
     * 使用 lazy 初始化确保只在第一次访问时创建，并且是单例。
     */
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
    }

    /**
     * 插入泛型值到 SharedPreferences 中。
     * 支持 Int, Long, Float, Boolean, String 类型。
     */
    fun <T> put(key: Key, value: T) {
        sharedPreferences.edit {
            when (value) {
                is Int -> putInt(key.keyId, value)
                is Long -> putLong(key.keyId, value)
                is Float -> putFloat(key.keyId, value)
                is Boolean -> putBoolean(key.keyId, value)
                is String -> putString(key.keyId, value)
                else -> return
            }
//        editor.commit()
        } // 异步提交更改
    }

    /**
     * 从 SharedPreferences 中获取泛型值。
     * 支持 Int, Long, Float, Boolean, String 类型。
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: Key): T {
        val defaultValue = key.default as T
        try {
            if (!sharedPreferences.contains(key.keyId)) { // 检查是否存在键
                put(key, defaultValue)
                logRelease("初始化键: ${key.keyId}")
                return defaultValue
            }

            val v = when (defaultValue) {
                is Int -> sharedPreferences.getInt(key.keyId, defaultValue) as? T
                is Long -> sharedPreferences.getLong(key.keyId, defaultValue) as? T
                is Float -> sharedPreferences.getFloat(key.keyId, defaultValue) as? T
                is Boolean -> sharedPreferences.getBoolean(key.keyId, defaultValue) as? T
                is String -> sharedPreferences.getString(key.keyId, defaultValue) as? T

                else -> null
            }

            if (v == null) {
                logRelease("键 ${key.keyId} 读取失败")
            } else {
                return v
            }
        } catch (t: Throwable) {
            logRelease("键 ${key.keyId} 读取失败", t)
        }
        return defaultValue
    }
}