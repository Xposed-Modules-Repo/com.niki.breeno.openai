package com.niki914.breeno.model.breeno

import com.niki914.core.logE
import com.niki914.core.logV
import com.niki914.hooker.call
import org.json.JSONArray
import org.json.JSONObject

class BreenoUIInjector {
    @Volatile
    var messageProcessor: Any? = null // MessageProcessor 的实例

    // 用于保存最后一次会话的上下文信息
    @Volatile
    var conversationInfo: Any? = null // ConversationInfo 的实例

    /**
     * 注入自定义响应的公共接口
     * 你可以在任何地方（比如你的大模型API回调中）调用此方法
     */
    fun injectJson(customJson: String): Boolean {
        if (messageProcessor == null || conversationInfo == null) {
            logE("注入失败: 尚未捕获到 MessageProcessor 实例或 ConversationInfo。请先触发一次官方对话。")
            return false
        }

        try {
            logV("准备注入自定义响应...")

            // 1. 为我们自己的JSON添加签名
            val jsonObject = JSONObject(customJson)

            // 2. 为我们自己的JSON添加签名（添加到顶级对象）
            jsonObject.put(
                OurJsonProtocol.SIGNATURE_KEY,
                OurJsonProtocol.SIGNATURE_VALUE
            )
            val signedJsonString = jsonObject.toString() // 这是 d 方法的第一个 String 参数

            // 3. 从签名后的 JSON 对象中获取 "directives" 数组，作为 d 方法的第二个 JSONArray 参数
            val signedJsonArray: JSONArray =
                jsonObject.getJSONArray("directives") // 这是 d 方法的第二个 JSONArray 参数

            // 2. 使用主动调用 e.d()
            messageProcessor!!.call<Any>(
                "d",
                signedJsonString,       // 修改后的带签名的 String
                signedJsonArray,                  // 修改后的带签名的 JSONArray
                conversationInfo                                // 使用我们保存的会话上下文
            )

            logV("自定义响应注入调用成功！")
            return true
        } catch (t: Throwable) {
            logE("注入自定义响应时发生严重错误", t)
            return false
        }
    }
}