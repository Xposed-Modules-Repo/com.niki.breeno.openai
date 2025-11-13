package com.niki914.download.util

import com.zephyr.log.logE
import com.zephyr.provider.TAG
import java.util.Locale

// 定义常用的字节常量，使用 Long 以避免溢出
private const val ONE_KB: Long = 1024L
private const val ONE_MB: Long = ONE_KB * 1024L
private const val ONE_GB: Long = ONE_MB * 1024L
private const val ONE_TB: Long = ONE_GB * 1024L

val Number.KB: Long
    get() = this.toLong() * ONE_KB

val Number.MB: Long
    get() = this.toLong() * ONE_MB

val Number.GB: Long
    get() = this.toLong() * ONE_GB

val Number.TB: Long
    get() = this.toLong() * ONE_TB

/**
 * 将字节数 (Long) 转换为 KB
 * @return 转换后的 KB 数值 (Double)
 */
fun Long.toKB(): Double {
    return this.toDouble() / ONE_KB
}

/**
 * 将字节数 (Long) 转换为 MB
 * @return 转换后的 MB 数值 (Double)
 */
fun Long.toMB(): Double {
    return this.toDouble() / ONE_MB
}

/**
 * 将字节数 (Long) 转换为 GB
 * @return 转换后的 GB 数值 (Double)
 */
fun Long.toGB(): Double {
    return this.toDouble() / ONE_GB
}

/**
 * 将字节数 (Long) 转换为 TB
 * @return 转换后的 TB 数值 (Double)
 */
fun Long.toTB(): Double {
    return this.toDouble() / ONE_TB
}

/**
 * 智能格式化字节数，自动选择最合适的单位 (B, KB, MB, GB, TB...)
 * 结果会保留两位小数。
 *
 * @return 格式化后的字符串 (例如: "1.23 GB", "456 KB")
 */
fun Long.toFormattedSizeString(): String {
    return when {
        this < ONE_KB -> String.format(Locale.getDefault(), "%d B", this)
        this < ONE_MB -> String.format(Locale.getDefault(), "%.2f KB", this.toKB())
        this < ONE_GB -> String.format(Locale.getDefault(), "%.2f MB", this.toMB())
        this < ONE_TB -> String.format(Locale.getDefault(), "%.2f GB", this.toGB())
        else -> String.format(Locale.getDefault(), "%.2f TB", this.toTB())
    }.also {
        logE(TAG, "$this bytes ==> '$it'")
    }
}