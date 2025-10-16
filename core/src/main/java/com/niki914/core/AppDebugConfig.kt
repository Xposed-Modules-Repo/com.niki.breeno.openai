package com.niki914.core

object AppDebugConfig {
    val isDebug = BuildConfig.DEBUG

    const val ALL = -1
    const val VERBOSE = 0
    const val DEBUG = 1
    const val ERROR = 2
    const val NONE = 3

    val LOG_LEVEL: Int = if (isDebug) {
        ALL
    } else {
        NONE
    }
    const val LOG_HEADER = "[niki]"
}