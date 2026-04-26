package com.niki914.hooker.internal

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.niki914.core.HookingClasses
import com.niki914.hooker.BaseHooker
import com.niki914.hooker.beans.MessageProcessorHookedParams
import org.json.JSONArray

/**
 * hook 小布官方大模型对话请求回调回来的数据
 * 
 * 最新版 12.7.4 映射：
 * 原: com.heytap.speech.engine.connect.core.manager.e.d
 * 现: com.heytap.speech.engine.connect.core.manager.h.v
 */
internal class BreenoHooker_1274 : BaseHooker<MessageProcessorHookedParams, Boolean>() {

    override val TAG: String = "BreenoHooker_12_7_4#v"

    override fun PackageParam.hookInternal(callback: (MessageProcessorHookedParams) -> Boolean) {
        HookingClasses.MESSAGE_PROCESSOR.toClass().resolve().firstMethod {
            name = "v"
            String::class.java
            JSONArray::class.java
            HookingClasses.CONVERSATION_INFO.toClass()
        }.hook {
            before {
                val json = args.getOrNull(0) as? String
                val messageProcessorInstance = instance
                val lastConversationInfo = args.getOrNull(2)

                val messageProcessorResult = MessageProcessorHookedParams(
                    json, messageProcessorInstance, lastConversationInfo
                )

                if (callback(messageProcessorResult)) { // 回调的返回值决定是否拦截
                    result = null // 拦截原逻辑
                }
            }
        }
    }
}
