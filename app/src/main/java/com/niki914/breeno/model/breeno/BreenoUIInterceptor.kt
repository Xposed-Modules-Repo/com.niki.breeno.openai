package com.niki914.breeno.model.breeno

import com.highcapable.yukihookapi.hook.param.PackageParam
import com.niki914.breeno.HookEntry
import com.niki914.breeno.model.breeno.faker.FakeJsonParamsControl
import com.niki914.core.logE
import com.niki914.core.logV
import com.niki914.hooker.adaption.BreenoHooker
import org.json.JSONObject

/**
 * 小布对话hook，hook UI 注入函数、阻止官方响应绘制等
 */
class BreenoUIInterceptor {

    @Volatile
    var shouldBlock = true

    private val jsonRecognizer = BreenoJsonRecognizer()
    private val uiInjector = BreenoUIInjector()
    private val jsonController = FakeJsonParamsControl()

    private var readyListener: ((Boolean) -> Unit)? = null

    fun setOnIDsReadyStateChangedListener(l: ((Boolean) -> Unit)?) {
        readyListener = l
    }

    fun setOnNewRoomListener(l: (() -> Unit)?) {
        jsonController.setOnNewRoomListener(l)
    }

    /**
     * 启动hook并提供回调
     */
    fun startHookLLMResponse(p: PackageParam) {
        BreenoHooker(HookEntry.Companion.versionName).hookWith(p) { hookedParams ->
            if (!shouldBlock) {
                logV("shouldBlock = false, 放行")
                return@hookWith false
            }

            hookedParams.run {
                if (json.isNullOrEmpty()) {
                    // 如果 json 为空，直接放行
                    return@hookWith false
                }

                val jsonObject = try {
                    JSONObject(json!!)
                } catch (t: Throwable) {
                    logE("json 解析失败", t)
                    return@run false
                }

                // 保存关键对象，供我们后续注入使用
                // 这是最关键的一步，确保我们有能力调用 d 方法
                // 总是更新为最新的
                messageProcessorInstance?.let {
                    uiInjector.messageProcessor = it
                    logV("成功捕获 MessageProcessor 实例!")
                }

                lastConversationInfo?.let {
                    uiInjector.conversationInfo = it
                    logV("成功捕获 ConversationInfo 实例!")
                }

                val type = jsonRecognizer.getType(jsonObject)

                when (type) {
                    BreenoJsonRecognizer.Type.ASR -> {
                        logV("放行 Asr 结果")
                        return@hookWith false // 放行此消息
                    }

                    is BreenoJsonRecognizer.Type.Acked -> {
                        val text = type.text
                        when (text) {
                            "好的，已收到" -> {
                                jsonObject.refreshJsonController()
                                logV("略过并刷新: 好的，已收到")
                                readyListener?.invoke(true)
                            }

                            "开始解析" -> {
                                logV("略过: 开始解析")
                            }

                            else -> {
                                return@hookWith true // 不放行此消息
                            }
                        }

                        return@hookWith false // 放行此消息
                    }

                    is BreenoJsonRecognizer.Type.Other -> {
                        logV("未辨识的官方响应已成功拦截！")
                        logV(type.rawJson)
                        return@hookWith true // 到这里的就是官方的 llm 响应, 全部拦截
                    }

                    BreenoJsonRecognizer.Type.Ours -> {
                        logV("监测到注入的响应，放行！JSON: $json")
                        return@hookWith false // 放行我们的注入响应
                    }
                }
            }
        }
    }

    /**
     * 注入大模型末帧
     */
    fun injectLastStreamFrame(query: String): Boolean = synchronized(this) {
        readyListener?.invoke(false)
        jsonController.getLastStreamFrame(query)?.let {
            return uiInjector.injectJson(it)
        }
        return false
    }

    /**
     * 注入大模型流式响应帧
     */
    fun injectStreamFrame(content: String): Boolean = synchronized(this) {
        jsonController.getStreamFrame(content)?.let {
            return uiInjector.injectJson(it)
        }
        return false
    }

    private fun JSONObject.refreshJsonController() {
        jsonController.refresh(
            sessionId = optString("sessionId"),
            recordId = optString("recordId"),
            originalRecordId = optString("originalRecordId"),
            roomId = optString("roomId"),
        )
    }
}