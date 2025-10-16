package com.niki914.core.repository.app

import android.content.Context
import com.highcapable.yukihookapi.hook.factory.prefs
import com.highcapable.yukihookapi.hook.xposed.prefs.YukiHookPrefsBridge
import com.niki914.core.repository.YukiRepository

@Deprecated("不能工作")
open class YukiContextPrefsRepository(val context: Context, prefsName: String) : YukiRepository() {
    override val yukiHookPrefsBridge: YukiHookPrefsBridge = context.prefs(prefsName)
}