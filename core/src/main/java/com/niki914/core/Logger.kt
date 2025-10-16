package com.niki914.core

import android.util.Log
import de.robv.android.xposed.XposedBridge

fun logV(msg: String = "") {
    if (AppDebugConfig.LOG_LEVEL <= AppDebugConfig.VERBOSE) {
        safeLog("${AppDebugConfig.LOG_HEADER}[详细]: $msg")
    }
}

fun logD(msg: String = "") {
    if (AppDebugConfig.LOG_LEVEL <= AppDebugConfig.DEBUG) {
        safeLog("${AppDebugConfig.LOG_HEADER}[调试]: $msg")
    }
}

fun logE(msg: String = "", t: Throwable? = null) {
    if (AppDebugConfig.LOG_LEVEL <= AppDebugConfig.ERROR) {
        var s = t?.stackTraceToString()
        if (s != null) {
            s = "\n$s"
        }
        safeLog("${AppDebugConfig.LOG_HEADER}[异常]: $msg${s ?: ""}")
    }
}

/**
 * 发行版日志，其他的日志都会被条件编译过滤掉
 */
fun logRelease(msg: String = "", t: Throwable? = null) {
    var s = t?.stackTraceToString()
    if (s != null) {
        s = "${t!!::class.simpleName}\n$s"
    }
    safeLog(
        "[小布换源]: $msg${s ?: ""}"
    )
}

/**
 * 在各种情况下都尽可能打日志
 *
 * 这里这么多捕捉是因为很容易 class not found
 */
private fun safeLog(string: String) = try {
    XposedBridge.log(string)
} catch (_: Throwable) {
    try {
        Log.e("XposedBridge", string)
    } catch (_: Throwable) {
        println("XposedBridge: $string")
    }
}