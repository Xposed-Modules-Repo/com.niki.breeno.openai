package com.niki914.core.repository.app

import com.niki914.core.repository.IEditableSettingsRepository
import com.niki914.core.repository.SharedPrefsRepository
import com.niki914.core.utils.SharedPreferenceHelper

@Deprecated("在我的设备上不能工作, 估计和不能直接访问其他应用的 /data/data/package_name/ 的问题有关, 解决办法未知")
abstract class AppSettingRepository(helper: SharedPreferenceHelper) :
    SharedPrefsRepository(helper), IEditableSettingsRepository