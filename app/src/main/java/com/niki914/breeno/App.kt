package com.niki914.breeno

import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.highcapable.yukihookapi.hook.xposed.application.ModuleApplication
import com.zephyr.log.LogConfig
import com.zephyr.log.LogLevel
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : ModuleApplication() {

    override fun onCreate() {
        super.onCreate()

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        LogConfig.edit {
            writeToFile = false
            logLevel = LogLevel.DO_NOT_LOG // 实际上根本没用这个日志
        }

        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}