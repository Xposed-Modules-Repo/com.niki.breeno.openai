package com.niki914.breeno.model.breeno

import com.niki914.core.logRelease
import org.json.JSONArray
import org.json.JSONObject

class BreenoJsonRecognizer {

    sealed class Type {
        data class Acked(val text: String) : Type() // 响应 (有重要字段)
        data object ASR : Type() // 语音识别
        data object Ours : Type() // 我们注入的
        data class Other(val rawJson: String) : Type() // 其他的直接屏蔽即可
    }

    private val JSONObject.directives: JSONArray?
        get() = optJSONArray("directives")

    fun getType(json: JSONObject): Type {
        try {
            val ours = json.containsOurs()
            ours?.let {
                return it
            }

            val acked = json.containsAcked()
            acked?.let {
                return it
            }

            val asr = json.containsASR()
            asr?.let {
                return it
            }
        } catch (t: Throwable) {
            logRelease("UI Interceptor error", t)
        }

        return Type.Other(json.toString(2))
    }

    private fun JSONObject.containsAcked(): Type.Acked? {
        directives?.apply {
            val len = length()
            for (i in 0..<len) {
                val thisObj = (get(i) as? JSONObject) ?: continue

                val type = thisObj
                    .optJSONObject("header")
                    ?.optString("name")

                if (type == "LoadingStateCard") {
                    val title = thisObj
                        .optJSONObject("payload")
                        ?.optString("title") ?: continue

                    return Type.Acked(title)
                }
            }
        }
        return null
    }

    private fun JSONObject.containsASR(): Type.ASR? {
        directives?.apply {
            val len = length()
            for (i in 0..<len) {
                val thisObj = (get(i) as? JSONObject) ?: continue

                val name = thisObj.optJSONObject("header")
                    ?.optString("name")

                val namespace = thisObj.optJSONObject("header")
                    ?.optString("namespace")

                val isRecognizeCommand =
                    (name == "RecognizeCommand" && namespace == "SpeechRecognizer")
                val isStreamRecognizeResult =
                    (name == "StreamRecognizeResult" && namespace == "SpeechRecognizer")

                if (isRecognizeCommand || isStreamRecognizeResult) {
                    return Type.ASR // 发现 ASR 相关指令，立即返回
                }
            }
        }
        return null
    }

    private fun JSONObject.containsOurs(): Type.Ours? {
        if (optString(OurJsonProtocol.SIGNATURE_KEY) == OurJsonProtocol.SIGNATURE_VALUE) {
            return Type.Ours
        }

        return null
    }
}