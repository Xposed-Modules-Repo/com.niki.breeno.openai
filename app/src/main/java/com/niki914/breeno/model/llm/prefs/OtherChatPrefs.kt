package com.niki914.breeno.model.llm.prefs

data class OtherChatPrefs(
    val showToolCalling: Boolean,
    val fallback: String,
    val enableRootAccess: Boolean,
    val isBlackList: Boolean,
    val list: String
)