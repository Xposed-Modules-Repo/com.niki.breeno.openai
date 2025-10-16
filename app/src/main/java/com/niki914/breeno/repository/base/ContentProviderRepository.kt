package com.niki914.breeno.repository.base

import android.content.Context
import com.niki914.breeno.provider.config.CursorUtils
import com.niki914.core.Key

open class ContentProviderRepository(context: Context) {
    private val provider = CursorUtils.with(context)

    protected fun getString(key: Key): String? {
        return provider.get<String>(key)
    }

    protected fun getInt(key: Key): Int? {
        return provider.get<Int>(key)
    }

    protected fun getLong(key: Key): Long? {
        return provider.get<Long>(key)
    }

    protected fun getFloat(key: Key): Float? {
        return provider.get<Float>(key)
    }

    protected fun getBoolean(key: Key): Boolean? {
        return provider.get<Boolean>(key)
    }

//    @Deprecated("系统级上下文由于权限问题不能访问 cp")
//    private fun getSystemContext(p: XC_LoadPackage.LoadPackageParam): Context? {
//        val activityThreadClass =
//            XposedHelpers.findClass("android.app.ActivityThread", p.classLoader)
//
//        // 尝试直接调用静态方法 currentActivityThread() 来获取 ActivityThread 实例
//        // 这个方法在不同Android版本上可能存在差异，甚至可能不是静态方法
//        val activityThreadInstance =
//            XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread")
//
//        val systemContext =
//            XposedHelpers.callMethod(activityThreadInstance, "getSystemContext") as? Context
//
//        return systemContext
//    }
}