package com.niki914.breeno.repository

import android.annotation.SuppressLint
import android.content.Context
import com.niki914.core.Key
import com.niki914.core.logD
import com.niki914.core.repository.SharedPrefsRepository
import com.niki914.core.utils.SharedPreferenceHelper

class MainSettingsRepository private constructor(helper: SharedPreferenceHelper) :
    SharedPrefsRepository(helper) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: MainSettingsRepository? = null

        /**
         * 获取 SettingsRepository 的单例实例。
         * 采用双重检查锁定（Double-Checked Locking）模式，确保线程安全且高效。
         */
        fun getInstance(context: Context): MainSettingsRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    val helper = SharedPreferenceHelper.getInstance(context, PREF_NAME)
                    MainSettingsRepository(helper).also {
                        INSTANCE = it
                        logD("SettingsRepository 已经实例化")
                    }
                }
            }

        fun getInstance(): MainSettingsRepository =
            INSTANCE ?: throw IllegalArgumentException("SettingsRepository 未初始化")
    }

    fun getAPIKey(): String {
        return get<String>(Key.ApiKey)
    }

    fun getUrl(): String {
        return get<String>(Key.Url)
    }

    fun getModelName(): String {
        return get<String>(Key.ModelName)
    }

    fun getSystemPrompt(): String {
        return get<String>(Key.SystemPrompt)
    }

    fun getTimeout(): Long {
        return get<Long>(Key.Timeout)
    }

    fun setAPIKey(value: String) {
        put(Key.ApiKey, value)
    }

    fun setUrl(value: String) {
        put(Key.Url, value)
    }

    fun setModelName(value: String) {
        put(Key.ModelName, value)
    }

    fun setSystemPrompt(value: String) {
        put(Key.SystemPrompt, value)
    }

    fun setTimeout(value: Long) {
        put(Key.Timeout, value)
    }
}