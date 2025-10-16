package com.niki914.tool.call.models

import android.app.Application
import android.content.Context
import android.media.AudioManager
import android.os.BatteryManager
import androidx.annotation.Keep
import com.niki914.chat.beans.PropertyDefinition
import com.niki914.core.ToolsNames
import com.niki914.core.logE
import com.niki914.core.logV
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 一个用于获取本机设备信息的工具。
 * 目前支持获取：当前时间、手机电量以及媒体音量信息。
 */
@Keep
internal data object GetDeviceInfoModel : BaseToolModel() {
    override val name: String = ToolsNames.GET_DEVICE_INFO
    override val description: String =
        "Provides real-time info about the device's current state. " +
                "This includes the current local time (YYYY-MM-DD HH:MM), " +
                "the phone's battery level(%), " +
                "and the device's media volume (current level and maximum level). " +
                "This tool is useful for answering user queries about the device's status or for " +
                "providing contextual awareness within assistant workflows."

    // 此工具不需要任何输入参数，因此 properties 为空
    override val properties: Map<String, PropertyDefinition>
        get() = emptyMap()

    // 不需要任何必需的输入参数
    override val required: List<String> = emptyList()

    /**
     * 获取当前格式化的时间。
     * @return 格式为 "YYYY-MM-DD HH:MM" 的时间字符串。
     */
    private fun getCurrentFormattedTime(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return dateFormat.format(Date())
    }

    /**
     * 获取电池信息（电量和充电状态）。
     * @param context 应用上下文。
     * @return 包含 "batteryLevel" (Int) 的 Map，
     *         或包含 "error" 信息的 Map。
     */
    private fun Context.getBatteryInfo(): Map<String, Any?> {
        val batteryManager = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            ?: return mapOf("error" to "BatteryManager 服务不可用")

        return try {
            val batteryLevel =
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            mapOf(
                "batteryLevel" to "$batteryLevel%"
            )
        } catch (e: Exception) {
            mapOf("error" to "获取电池信息失败: ${e.message}")
        }
    }

    /**
     * 获取媒体音量信息（当前音量和最大音量）。
     * @param context 应用上下文。
     * @return 包含 "volumeLevel" (Int), "maxVolume" (Int), "streamType" (String) 的 Map，
     *         或包含 "error" 信息的 Map。
     */
    private fun Context.getVolumeInfo(): Map<String, Any?> {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            ?: return mapOf("error" to "AudioManager 服务不可用")

        return try {
            // 我们通常关心媒体音量 (STREAM_MUSIC)
            val streamType = AudioManager.STREAM_MUSIC
            val currentVolume = audioManager.getStreamVolume(streamType)
            val maxVolume = audioManager.getStreamMaxVolume(streamType)
            mapOf(
                "volumeLevel" to currentVolume,
                "maxVolume" to maxVolume,
                "streamType" to "STREAM_MUSIC" // 明确是媒体音量
            )
        } catch (e: Exception) {
            mapOf("error" to "获取音量信息失败: ${e.message}")
        }
    }

    /**
     * 执行获取设备信息的实际逻辑。
     * @param args 来自大模型的输入参数，此工具不使用。
     * @param application 应用上下文，用于获取系统服务。
     * @return 包含设备信息的 JsonObject。
     */
    override suspend fun callInternal(
        args: JsonObject,
        extras: JsonObject,
        application: Application?
    ): JsonObject {
        // 检查上下文实例是否可用
        if (application == null) {
            logE("获取设备信息失败: 上下文实例为空")
            return exceptionCaughtJson("上下文实例为空，无法获取设备信息")
        }

        val resultData = mutableMapOf<String, Any?>()
        var hasError = false

        // 1. 获取时间
        resultData["time"] = getCurrentFormattedTime()

        // 2. 获取电池信息
        val batteryInfo = application.getBatteryInfo()
        if (batteryInfo.containsKey("error")) {
            logE("获取电量信息出错: ${batteryInfo["error"]}")
            resultData["batteryInfoError"] = batteryInfo["error"]
            hasError = true
        } else {
            resultData["batteryLevel"] = batteryInfo["batteryLevel"]
            resultData["isCharging"] = batteryInfo["isCharging"]
        }

        // 3. 获取音量信息
        val volumeInfo = application.getVolumeInfo()
        if (volumeInfo.containsKey("error")) {
            logE("获取音量信息出错: ${volumeInfo["error"]}")
            resultData["volumeInfoError"] = volumeInfo["error"]
            hasError = true
        } else {
            resultData["volumeLevel"] = volumeInfo["volumeLevel"]
            resultData["maxVolume"] = volumeInfo["maxVolume"]
            resultData["streamType"] = volumeInfo["streamType"]
        }

        // 构建返回给大模型的 JsonObject
        val outputJson = buildJsonObject {
            resultData.forEach { (key, value) ->
                when (value) {
                    is String -> put(key, value)
                    is Int -> put(key, value)
                    is Boolean -> put(key, value)
                    else -> {
                        // 处理 null 或其他 unexpected 类型，转换为字符串或者 JsonNull
                        if (value == null) {
                            put(key, "") // 或者 JsonNull，取决于你希望大模型如何处理
                        } else {
                            put(key, value.toString())
                        }
                    }
                }
            }
        }

        if (hasError) {
            logV("部分设备信息获取成功，但存在错误: $outputJson")
            // 即使有错误也返回部分成功的数据，同时可以通过一个字段表示存在问题
        } else {
            logV("成功获取所有设备信息: $outputJson")
        }

        return outputJson // 返回实际的JSON对象
    }
}