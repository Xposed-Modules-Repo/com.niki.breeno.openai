package com.niki914.breeno.ui.navigation

sealed class Screens(val route: String) {
    object MainSettings : Screens("main_settings_screen")
    object OtherSettings : Screens("other_settings_screen")
    object ShellCmdSettings : Screens("shell_cmd_settings_screen")
    object ChatTest : Screens("chat_test_screen")
}