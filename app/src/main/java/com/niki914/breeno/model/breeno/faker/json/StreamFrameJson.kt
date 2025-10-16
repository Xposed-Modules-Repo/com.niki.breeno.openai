package com.niki914.breeno.model.breeno.faker.json

import com.niki914.core.generateRandomHexString
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * UI 欺骗的 llm 流式传输帧
 */
object StreamFrameJson {

    private fun getOutputSpeechDirective(content: String): JsonObject {
        return buildJsonObject {
            putJsonObject("header") {
                put("id", generateRandomHexString(32))
                put("name", "OutputSpeech")
                put("namespace", "SpeechSynthesizer")
                put("namespaceVersion", "2.0.2")
                put("version", "2.1")
            }
            putJsonObject("payload") {
                put("newAi", true)
                put("text", content)
                put("type", "text")
            }
        }
    }

    private fun getExpectSpeechDirective(micOn: Boolean): JsonObject {
        return buildJsonObject {
            putJsonObject("header") {
                put("id", generateRandomHexString(32))
                put("name", "ExpectSpeech")
                put("namespace", "SpeechRecognizer")
                put("namespaceVersion", "2.0.10")
                put("version", "2.2")
            }
            putJsonObject("payload") {
                put("newAi", true)
                put("micAct", if (micOn) "on" else "off")
            }
        }
    }

    private fun getSpeechDirective(content: String, micOn: Boolean = true): JsonArray {
        return buildJsonArray {
            add(getOutputSpeechDirective(content))
            add(getExpectSpeechDirective(micOn))
        }
    }

    private fun getStreamTextCardDirective(content: String, roomId: String): JsonObject {
        return buildJsonObject {
            putJsonObject("header") {
                put("id", generateRandomHexString(32))
                put("name", "StreamTextCard")
                put("namespace", "MyAI")
                put("namespaceVersion", "2.0.27")
                put("version", "2.8")
            }
            putJsonObject("payload") {
                put("needNewRoom", false)
                put("isHtml", false)
                put("isFinal", false)
                put("type", 0)
                put("roomId", roomId)
                put("content", content)
                put("charPerSec", 50)
            }
        }
    }

    private fun getBreenoFeedbackDirective(): JsonObject {
        return buildJsonObject {
            putJsonObject("header") {
                put("id", generateRandomHexString(32))
                put("name", "BreenoFeedback")
                put("namespace", "Tracking")
                put("namespaceVersion", "2.0.8")
                put("version", "2.4")
            }
            putJsonObject("payload") {
                putJsonArray("longPressInfoList") {
                    addJsonObject {
                        put("buttonText", "复制全文")
                        put("type", "copy_all")
                    }
                    addJsonObject {
                        put("buttonText", "选择文本")
                        put("type", "select")
                    }
                    addJsonObject {
                        put("buttonText", "播报全文")
                        put("type", "speak")
                    }
                    addJsonObject {
                        put("buttonText", "导出到便签")
                        put("type", "export_to_notes")
                    }
                    addJsonObject {
                        put("buttonText", "点赞")
                        put("type", "like")
                    }
                    addJsonObject {
                        put("buttonText", "点踩")
                        put("type", "dislike")
                    }
                }
                putJsonObject("dislikeInfo") {
                    putJsonArray("choices") {
                        addJsonObject {
                            put("choiceTitle", "体验问题")
                            putJsonArray("options") {
                                add("答非所问")
                                add("信息过时")
                                add("答案有误")
                                add("没有帮助")
                                add("过于模板化")
                                add("没结合上文")
                                add("语音识别错")
                                add("播报有问题")
                            }
                        }
                        addJsonObject {
                            put("choiceTitle", "安全问题")
                            putJsonArray("options") {
                                add("政治影响")
                                add("违反公序良俗")
                                add("违法")
                                add("侮辱")
                                add("偏见歧视")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun getAckPublishDirective(): JsonObject {
        return buildJsonObject {
            putJsonObject("header") {
                put("id", generateRandomHexString(32))
                put("name", "AckPublish")
                put("namespace", "Command")
                put("namespaceVersion", "2.0.8")
                put("version", "2.0")
            }
            putJsonObject("payload") {
                putJsonArray("type") {
                    add("REC_ACK")
                }
            }
        }
    }

    fun get(
        content: String,
        isFirst: Boolean,
        sessionId: String,
        recordId: String,
        originalRecordId: String,
        roomId: String,
        sequenceId: Int,
        timestamp: Long
    ): String {
        val jsonResponse = buildJsonObject {
            put("conversationId", "")

            putJsonArray("directives") {
                // 如果需要语音，添加语音指令
//                if (speech) {
//
//                    val speechDirectives = getSpeechDirective(content)
//                    speechDirectives.forEach { directive ->
//                        add(directive)
//                    }
//                }

                // 添加 StreamTextCard 指令
                add(getStreamTextCardDirective(content, roomId))

                // 添加 BreenoFeedback 指令
                add(getBreenoFeedbackDirective())

                // 添加 AckPublish 指令
                add(getAckPublishDirective())
            }

            putJsonObject("extend") {
                put("isLlmResult", true)
                put("userInputType", "1")
                put("isLlmFirstResp", isFirst)
            }

            put("originalRecordId", originalRecordId)
            put("recordId", recordId)
            put("roomId", roomId)
            put("sequenceId", sequenceId)
            put("sessionId", sessionId)
            put("uniqueId", timestamp.toString())
            put("version", "3.0")
        }

        return jsonResponse.toString()
    }
}