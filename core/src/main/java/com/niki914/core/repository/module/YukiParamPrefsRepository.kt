package com.niki914.core.repository.module

import com.highcapable.yukihookapi.hook.param.PackageParam
import com.niki914.core.repository.YukiRepository

@Deprecated("不能工作")
open class YukiParamPrefsRepository(param: PackageParam, prefsName: String) : YukiRepository() {
    override val yukiHookPrefsBridge = param.prefs(prefsName)
}