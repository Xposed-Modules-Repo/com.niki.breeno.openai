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
 * 回调返回的布尔值应该根据 MessageProcessorHookedParams 来判断是否阻断原函数，阻断的话就不渲染出来了
 */
internal class BreenoHooker_all_below_1192 : BaseHooker<MessageProcessorHookedParams, Boolean>() {

    override val TAG: String = "BreenoHooker_all_below_11_9_1#d"

    override fun PackageParam.hookInternal(callback: (MessageProcessorHookedParams) -> Boolean) {
        HookingClasses.MESSAGE_PROCESSOR.toClass().resolve().firstMethod {
            name = "d"
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