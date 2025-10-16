package com.niki914.core.repository

// TODO 给 repository 实现
interface IMainSettingsRepository_ReadOnly {
    fun getAPIKey(): String
    fun getUrl(): String
    fun getModelName(): String
    fun getSystemPrompt(): String
    fun getTimeout(): Long
}

interface IMainSettingsRepository : IMainSettingsRepository_ReadOnly {
    fun setAPIKey(value: String)
    fun setUrl(value: String)
    fun setModelName(value: String)
    fun setSystemPrompt(value: String)
    fun setTimeout(value: Long)
}

interface IOtherSettingsRepository_ReadOnly {
    fun getProxy(): Pair<String?, Int?>
    fun getEnableApp(): Boolean
    fun getEnableShowToolCalling(): Boolean
    fun getEnableUri(): Boolean
    fun getEnableGetDeviceInfo(): Boolean
    fun getEnableShellCmd(): Boolean
    fun getFallbackToBreeno(): String
}

interface IOtherSettingsRepository {
    fun setProxy(host: String?, port: Int?)
    fun setProxy(uri: String)
    fun setEnableShowToolCalling(value: Boolean)
    fun setEnableApp(value: Boolean)
    fun setEnableUri(value: Boolean)
    fun setEnableGetDeviceInfo(value: Boolean)
    fun setEnableShellCmd(value: Boolean)
    fun setFallbackToBreeno(value: String)
}

interface IShellCmdSettingsRepository_ReadOnly {
    fun getEnableRootAccess(): Boolean
    fun getIsBlackList(): Boolean
    fun getKeywords(): String
    fun getAskBeforeExec(): Boolean
}

interface IShellCmdSettingsRepository {
    fun setEnableRootAccess(value: Boolean)
    fun setIsBlackList(value: Boolean)
    fun setKeywords(value: String)
    fun setAskBeforeExec(value: Boolean)
}