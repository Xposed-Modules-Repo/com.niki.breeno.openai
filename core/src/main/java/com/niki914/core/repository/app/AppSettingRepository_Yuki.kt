package com.niki914.core.repository.app

import android.content.Context
import com.niki914.core.repository.IEditableSettingsRepository

@Deprecated("在我的设备上不能工作, 估计和不能直接访问其他应用的 /data/data/package_name/ 的问题有关, 解决办法未知")
abstract class AppSettingRepository_Yuki(appContext: Context, prefsName: String) :
    YukiContextPrefsRepository(appContext, prefsName), IEditableSettingsRepository