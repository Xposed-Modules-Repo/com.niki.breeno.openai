package com.niki914.breeno.provider.config

import android.content.Context
import android.database.Cursor
import com.niki914.core.Key
import com.niki914.core.logD
import com.niki914.core.logE
import com.niki914.core.logRelease

// 1. CursorUtils 提供静态方法，返回一个中间对象
object CursorUtils {
    fun with(context: Context): CursorProvider {
        // 在此处，我们返回一个CursorProvider实例，它临时持有Context
        return CursorProvider(context.applicationContext)
    }
}

// 2. 中间对象，负责接收key并执行查询
class CursorProvider(private val context: Context) {
    inline operator fun <reified T> get(key: Key): T? {
        val cursor = getCursor(key.keyId)
        if (cursor == null) {
            logRelease("Cursor 读取出错: getCursor 失败")
        }
        return cursor?.getValue<T>()
    }

    fun getCursor(key: String): Cursor? {
        // 在这里使用context进行查询，它的生命周期只在`with()`方法被调用时
        val queryUri = ConfigProvider.getQueryUri(key)
        val cursor = context.contentResolver.query(queryUri, null, null, null, null)
        return cursor
    }

    inline fun <reified T> Cursor.getValue(): T? {
        try {
            use {
                if (!it.moveToFirst()) {
                    logRelease("Cursor 读取出错: failed to move cursor to first")
                } else {
                    val columnIndex = it.getColumnIndex("value")
                    if (columnIndex < 0) {
                        logRelease("Cursor 读取出错: index = $columnIndex")
                    } else {
                        val v = when (T::class) {
                            String::class -> it.getString(columnIndex)
                            Int::class -> it.getInt(columnIndex)
                            Long::class -> it.getLong(columnIndex)
                            Float::class -> it.getFloat(columnIndex)
                            Boolean::class -> (it.getInt(columnIndex) != 0)   // Boolean 存储为 0/1
                            else -> {
                                logD("ContentProviderRepository#Cursor\$getValue: t is ${T::class.java.name}")
                                null
                            }
                        }
                        return (v as? T)
                    }
                }
            }
        } catch (t: Throwable) {
            logRelease("Cursor 读取出错", t)
            logE("ContentProviderRepository#Cursor\$getValue", t)
        }
        return null
    }
}