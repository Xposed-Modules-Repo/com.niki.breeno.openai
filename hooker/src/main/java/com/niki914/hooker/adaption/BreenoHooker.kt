package com.niki914.hooker.adaption

import com.highcapable.yukihookapi.hook.param.PackageParam
import com.niki914.hooker.BaseHooker
import com.niki914.hooker.beans.MessageProcessorHookedParams
import com.niki914.hooker.internal.BreenoHooker_all_below_1192

/**
 * hook 小布官方大模型对话请求回调回来的数据
 *
 * 回调返回的布尔值应该根据 MessageProcessorHookedParams 来判断是否阻断原函数，阻断的话就不渲染出来了
 *
 * TODO DexKit 版本自适应查找方法
 */
class BreenoHooker(var version: String) :
    BaseHooker<MessageProcessorHookedParams, Boolean>() {
    private val hooker_ALL = BreenoHooker_all_below_1192()
    override val TAG: String = hooker_ALL.TAG

    override fun PackageParam.hookInternal(
        callback: (MessageProcessorHookedParams) -> Boolean
    ) {
        when (version) {
            else -> hooker_ALL.hookWith(this, callback)
        }
    }
}