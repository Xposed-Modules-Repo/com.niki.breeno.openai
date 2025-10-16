package com.niki914.breeno.model.breeno.faker.json

import com.niki914.core.generateRandomHexString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * UI 欺骗的对话末帧
 */
object LastStreamFrameJson {

    private fun getStreamTextCardDirective(roomId: String): JsonObject {
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
                put("statement", "由 AI 生成，内容仅供参考")
                put("isFinal", true)
                put("type", 0)
                put("roomId", roomId)
                put("content", "")
            }
        }
    }

    private fun getExpectSpeechDirective(): JsonObject {
        return buildJsonObject {
            putJsonObject("header") {
                put("id", generateRandomHexString(32))
                put("name", "ExpectSpeech")
                put("namespace", "SpeechRecognizer")
                put("namespaceVersion", "2.0.10")
                put("version", "2.2")
            }
            putJsonObject("payload") {
                put("micAct", "off")
            }
        }
    }

    private fun getClientTrackingDirective(): JsonObject {
        return buildJsonObject {
            putJsonObject("header") {
                put("id", generateRandomHexString(32))
                put("name", "ClientTracking")
                put("namespace", "Tracking")
                put("namespaceVersion", "2.0.8")
                put("version", "2.1")
            }
            putJsonObject("payload") {
                put("skillId", -88888888)
                put("code", "normal_bot-rhythm-controller_llm-finished")
                put(
                    "expIds",
                    "21853,26537,38244,15544,34851,49705,49050,42681,51474,38573,45941,46888,60189"
                )
                putJsonObject("extendMap") {
                    put("skillName", "llm_general_skill_name")
                    putJsonObject("exp_info") {
                        put("个性化问答", "29193,35866,38692,42406,56568,58160")
                        put(
                            "center",
                            "21853,26537,38244,15544,34851,49705,49050,42681,51474,38573,45941,46888,60189"
                        )
                    }
                }
                put("message", "正常大模型返回最后一帧")
                put("dmName", "bot-rhythm-controller")
                put("resourceType", "normal_finished")
            }
        }
    }

    private fun getBreenoFeedbackDirective(query: String, recordId: String): JsonObject {
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
                        put("buttonText", "复制")
                        put("type", "copy")
                    }
                    addJsonObject {
                        put("buttonText", "播报全文")
                        put("type", "speak")
                    }
                    addJsonObject {
                        put("buttonText", "点踩")
                        put("type", "dislike")
                    }
                    addJsonObject {
                        put("buttonText", "点赞")
                        put("type", "like")
                    }
                }
                putJsonArray("newLongPressInfoList") {
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
                putJsonObject("footerInfo") {
                    put("copyFlag", true)
                    putJsonObject("copyInfo") {
                        put("autoCopy", false)
                    }
                    put("upvoteFlag", true)
                    putJsonArray("shareInfos") {
                        addJsonObject {
                            put("buttonText", "导出到便签")
                            put("type", "export_to_notes")
                        }
                    }
                    putJsonObject("regenerate") {
                        put("echoInfo", "{\"recordId\":\"$recordId\",\"query\":\"$query\"}")
                        put("showText", "换一个回复")
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
        query: String,
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
                // StreamTextCard 指令
                add(getStreamTextCardDirective(roomId))

                // ExpectSpeech 指令
                add(getExpectSpeechDirective())

                // ClientTracking 指令
                add(getClientTrackingDirective())

                // BreenoFeedback 指令
                add(getBreenoFeedbackDirective(query, recordId))

                // AckPublish 指令
                add(getAckPublishDirective())
            }

            putJsonObject("extend") {
                put("isLlmResult", true)
                put("userInputType", "1")
                put("isLlmFinalResponse", true)
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