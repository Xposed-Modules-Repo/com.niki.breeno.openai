package com.niki914.breeno.model.llm.prefs

/**
 * OtherChatPrefs 的 DSL 构建器
 */
class OtherChatPrefsBuilder {

    companion object {
        /**
         * DSL 构建 OtherChatPrefs
         */
        internal fun otherChatPrefs(block: OtherChatPrefsBuilder.() -> Unit = {}): OtherChatPrefs {
            return OtherChatPrefsBuilder().apply(block).build()
        }

        internal fun initialOtherChatPrefs(
            showToolCalling: Boolean,
            fallback: String,
            enableRootAccess: Boolean,
            isBlackList: Boolean,
            list: String
        ): OtherChatPrefs {
            return OtherChatPrefsBuilder().apply {
                this.showToolCalling = showToolCalling
                this.fallback = fallback
                this.enableRootAccess = enableRootAccess
                this.isBlackList = isBlackList
                this.list = list
            }.build()
        }

        internal fun fromPrefs(prefs: OtherChatPrefs): OtherChatPrefsBuilder {
            return OtherChatPrefsBuilder().apply {
                showToolCalling = prefs.showToolCalling
                fallback = prefs.fallback
                enableRootAccess = prefs.enableRootAccess
                isBlackList = prefs.isBlackList
                list = prefs.list
            }
        }
    }

    var showToolCalling: Boolean = false

    var fallback = ""

    var enableRootAccess: Boolean = false

    var isBlackList: Boolean = true

    var list: String = ""

    internal fun build(): OtherChatPrefs {
        return OtherChatPrefs(showToolCalling, fallback, enableRootAccess, isBlackList, list)
    }
}