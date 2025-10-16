package com.niki914.core

import androidx.annotation.StringRes

/**
 * 菜单项
 */
enum class MainMenuChoices(
    @StringRes val uiStringRes: Int, // 使用资源 ID
) {
    OtherSettings(R.string.other_setting_ui_string),
    Report(R.string.report_ui_string),
    Test(R.string.testing_ui_string),
    About(R.string.about_ui_string)
}