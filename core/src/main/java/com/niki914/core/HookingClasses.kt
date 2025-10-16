package com.niki914.core

/**
 * 关键逆向类名
 */
object HookingClasses {
    /*
        TODO 更好的数据响应注入点，可能可以欺骗 Room：com.heytap.speech.engine.connect.core.listener.EventListener.a.onMessageReceived(String, String, String, boolean) void
     */


    const val AI_CHAT_COMMON_HELPER =
        "com.heytap.speechassist.aichat.floatwindow.utils.AiChatCommonHelperKt"
    const val AI_CHAT_ENGINE_HELPER =
        "com.heytap.speechassist.aichat.floatwindow.AIChatEngineHelper"
    const val DIRECTIVE = "com.heytap.speech.engine.protocol.directive.Directive"
    const val MESSAGE_PROCESSOR = "com.heytap.speech.engine.connect.core.manager.e"
    const val CONVERSATION_INFO = "com.heytap.speech.engine.net.remote.ConversationInfo"

    //    const val APP_APPLICATION = "com.heytap.speechassist.AppApplication" // hook 超类就行了
    const val APP_APPLICATION = "android.app.Application"
    const val SPEECH_SERVICE = "com.heytap.speechassist.core.SpeechService"
    const val AI_CHAT_CONVERSATION_BASE_ADAPTER =
        "com.heytap.speechassist.aichat.ui.adapter.AIChatConversationBaseAdapter"
    const val AI_CHAT_VIEW_BEAN = "com.heytap.speechassist.aichat.bean.AIChatViewBean"
}