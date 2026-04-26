package com.niki914.hooker.internal

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.niki914.core.HookingClasses
import com.niki914.hooker.BaseHooker
import com.niki914.hooker.call

/**
 * Hook AIChatConversationBaseAdapter.S() 方法，拦截并记录所有即将添加和显示在聊天界面的消息
 * 此处判断 bean 类如果是来自用户的提问的就回调
 * 
 * 最新版 12.7.4 映射：
 * 原: o(AIChatViewBean, Integer) / n(AIChatViewBean, Integer)
 * 现: S(AIChatViewBean, Integer)
 */
internal class ChatRVAdapterHooker_1274 : BaseHooker<String, Unit>() {
    override val TAG: String = "AIChatConversationBaseAdapter_12_7_4#S"

    override fun PackageParam.hookInternal(callback: (String) -> Unit) {
        HookingClasses.AI_CHAT_CONVERSATION_BASE_ADAPTER.toClass().resolve().firstMethod {
            name = "S"
            HookingClasses.AI_CHAT_VIEW_BEAN.toClass()
            Integer::class.java
        }.hook {
            before {
                val bean = args.getOrNull(0) ?: return@before

                val content = bean.call<String>("getContent")
                val isQuery = bean.call<Boolean>("isQuery") ?: false

                if (isQuery && !content.isNullOrBlank()) {
                    callback(content)
                }
            }
        }
    }
}
