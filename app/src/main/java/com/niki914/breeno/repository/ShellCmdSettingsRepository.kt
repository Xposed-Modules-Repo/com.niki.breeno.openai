package com.niki914.breeno.repository

import android.annotation.SuppressLint
import android.content.Context
import com.niki914.core.Key
import com.niki914.core.logD
import com.niki914.core.repository.SharedPrefsRepository
import com.niki914.core.utils.SharedPreferenceHelper

class ShellCmdSettingsRepository private constructor(helper: SharedPreferenceHelper) :
    SharedPrefsRepository(helper) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: ShellCmdSettingsRepository? = null

        fun getInstance(context: Context): ShellCmdSettingsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val helper = SharedPreferenceHelper.getInstance(context, PREF_NAME)
                    ShellCmdSettingsRepository(helper).also {
                        INSTANCE = it
                        logD("SettingsRepository 已经实例化")
                    }
                }
            }

        fun getInstance(): ShellCmdSettingsRepository =
            INSTANCE ?: throw IllegalArgumentException("SettingsRepository 未初始化")
    }

    fun getEnableRootAccess(): Boolean {
        return get<Boolean>(Key.EnableRootAccessForShellCmd)
    }

    fun getIsBlackList(): Boolean {
        return get<Boolean>(Key.IsShellUsingBlackList)
    }

    fun getKeywords(): String {
        return get<String>(Key.ShellCmdList)
    }

    fun getAskBeforeExec(): Boolean {
        return get<Boolean>(Key.AskBeforeExecuteShell)
    }

    fun setEnableRootAccess(value: Boolean) {
        put(Key.EnableRootAccessForShellCmd, value)
    }

    fun setIsBlackList(value: Boolean) {
        put(Key.IsShellUsingBlackList, value)
    }

    fun setKeywords(value: String) {
        put(Key.ShellCmdList, value)
    }

    fun setAskBeforeExec(value: Boolean) {
        put(Key.AskBeforeExecuteShell, value)
    }
}