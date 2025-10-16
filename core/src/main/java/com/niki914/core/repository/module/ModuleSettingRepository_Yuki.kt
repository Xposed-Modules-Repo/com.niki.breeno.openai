package com.niki914.core.repository.module

import com.highcapable.yukihookapi.hook.param.PackageParam
import com.niki914.core.repository.ISettingsRepository

@Deprecated("在我的设备上不能工作, 估计和不能直接访问其他应用的 /data/data/package_name/ 的问题有关, 解决办法未知")
abstract class ModuleSettingRepository_Yuki(param: PackageParam, prefsName: String) :
    YukiParamPrefsRepository(param, prefsName), ISettingsRepository