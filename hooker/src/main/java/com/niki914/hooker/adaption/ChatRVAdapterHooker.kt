package com.niki914.hooker.adaption

import com.highcapable.yukihookapi.hook.param.PackageParam
import com.niki914.hooker.BaseHooker
import com.niki914.hooker.internal.ChatRVAdapterHooker_1186
import com.niki914.hooker.internal.ChatRVAdapterHooker_1191_to_1192

class ChatRVAdapterHooker(var version: String) : BaseHooker<String, Unit>() {
    private val hooker_1186 = ChatRVAdapterHooker_1186()
    private val hooker_1191_to_1192 = ChatRVAdapterHooker_1191_to_1192()
    override val TAG: String = hooker_1186.TAG

    override fun PackageParam.hookInternal(callback: (String) -> Unit) {
        when (version) {
            "11.8.6" -> hooker_1186.hookWith(this, callback)
            else -> hooker_1191_to_1192.hookWith(this, callback)
        }
    }
}