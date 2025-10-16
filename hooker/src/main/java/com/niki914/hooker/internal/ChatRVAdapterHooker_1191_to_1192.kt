package com.niki914.hooker.internal

import com.highcapable.kavaref.KavaRef.Companion.resolve
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.niki914.core.HookingClasses
import com.niki914.hooker.BaseHooker
import com.niki914.hooker.call

/**
 * Hook AIChatConversationBaseAdapter.o() 方法，拦截并记录所有即将添加和显示在聊天界面的消息
 * 此处判断 bean 类如果是来自用户的提问的就回调
 *
 * 由小布主应用和浮窗共用这这个 rv 所以hook的时候应该注意防抖动
 */
internal class ChatRVAdapterHooker_1191_to_1192 : BaseHooker<String, Unit>() {
    override val TAG: String = "AIChatConversationBaseAdapter_11_9_1#o"

    /*
      public final void o(@NotNull AIChatViewBean bean, @Nullable Integer num) {
     */
    override fun PackageParam.hookInternal(callback: (String) -> Unit) {
        HookingClasses.AI_CHAT_CONVERSATION_BASE_ADAPTER.toClass().resolve().firstMethod {
            name = "o"
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