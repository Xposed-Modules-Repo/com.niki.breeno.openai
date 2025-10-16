package com.niki914.core

import androidx.annotation.Keep
import androidx.annotation.StringRes
import com.niki914.core.R.string


/**
 * 设置配置项的基本数据类
 *
 * 用的地方超级多，从本地化存储到 UI 都在用
 */
@Keep
sealed class Key(
    val keyId: String,
    @StringRes val uiStringRes: Int, // 使用资源 ID
    @StringRes val uiDescriptionRes: Int?, // 使用资源 ID
    val default: Any,
    val type: ConfigSettingType
) {

    /**
     * 配置项的修改方式
     */
    enum class ConfigSettingType {
        INPUT, // 对话框
        SWITCH, // 开关
        ACTIVITY
    }

    ///////// MAIN SETTINGS START /////////

    data object ApiKey : Key(
        "api_key",
        string.api_key_ui_string,
        string.api_key_ui_description,
        "",
        ConfigSettingType.INPUT
    )

    data object Url : Key(
        "api_url", // 为了避免丢失就不改这个键了，懒得迁移
        string.llm_url_ui_string,
        string.llm_url_ui_description,
        "https://api.openai.com/v1/chat/completions",
        ConfigSettingType.INPUT
    )

    data object ModelName : Key(
        "model_name",
        string.model_name_ui_string,
        string.model_name_ui_description,
        "",
        ConfigSettingType.INPUT
    )

    data object SystemPrompt : Key(
        "system_prompt",
        string.system_prompt_ui_string,
        string.system_prompt_ui_description,
        "",
        ConfigSettingType.INPUT
    )

    data object Timeout : Key(
        "timeout",
        string.timeout_ui_string,
        string.timeout_ui_description,
        15L,
        ConfigSettingType.INPUT
    )

    ///////// MAIN SETTINGS EMD /////////
    ///////// OTHER SETTINGS START /////////

    data object Proxy : Key(
        "socks_proxy",
        string.proxy_ui_string,
        string.proxy_ui_description,
        "",
        ConfigSettingType.INPUT
    )

    data object EnableShowToolCalling : Key(
        "show_tool_calling",
        string.show_tool_calling_ui_string,
        null,
        false,
        ConfigSettingType.SWITCH
    )

    data object EnableLaunchApp : Key(
        "llm_launch_app",
        string.llm_launch_app_ui_string,
        null,
        false,
        ConfigSettingType.SWITCH
    )

    data object EnableLaunchURI : Key(
        "llm_launch_uri",
        string.llm_launch_uri_ui_string,
        string.llm_launch_uri_ui_description,
        false,
        ConfigSettingType.SWITCH
    )

    data object EnableGetDeviceInfo : Key(
        "llm_get_device_info",
        string.llm_get_device_info_ui_string,
        null,
        false,
        ConfigSettingType.SWITCH
    )

    data object EnableShellCmd : Key(
        "llm_run_shell_command",
        string.llm_run_shell_command_ui_string,
        string.llm_run_shell_command_ui_description,
        false,
        ConfigSettingType.ACTIVITY
    )

    data object FallbackToBreeno : Key(
        "fallback_to_breeno",
        string.fallback_to_breeno_ui_string,
        string.fallback_to_breeno_ui_description,
        "",
        ConfigSettingType.INPUT
    )

    ///////// OTHER SETTINGS END /////////
    ///////// SHELL CMD SETTINGS START /////////

    data object EnableRootAccessForShellCmd : Key(
        "run_shell_command_with_root_access",
        string.run_shell_cmd_with_root_ui_string,
        null,
        false,
        ConfigSettingType.SWITCH
    )

    data object IsShellUsingBlackList : Key(
        "is_shell_using_black_list",
        string.shell_list_color_ui_string,
        string.shell_list_color_ui_description,
        true,
        ConfigSettingType.SWITCH
    )

    /**
     * 通过黑白名单切换作用
     */
    data object ShellCmdList : Key(
        "shell_cmd_list",
        string.shell_list_ui_string,
        string.shell_list_ui_description,
        "",
        ConfigSettingType.INPUT
    )

    data object AskBeforeExecuteShell : Key(
        "ask_before_exec_shell",
        string.shell_ask_first_ui_string,
        null,
        true,
        ConfigSettingType.SWITCH
    )

    ///////// SHELL CMD SETTINGS END /////////
    ///////// CHAT TEST START /////////


    ///////// CHAT TEST END /////////

    companion object {
        // 手动维护所有实例的列表
        private val entries: List<Key> = listOf(
            ApiKey,
            Url,
            ModelName,
            SystemPrompt,
            Timeout,
            Proxy,
            EnableShowToolCalling,
            EnableLaunchApp,
            EnableLaunchURI,
            EnableGetDeviceInfo,
            EnableShellCmd,
            FallbackToBreeno,
            EnableRootAccessForShellCmd,
            IsShellUsingBlackList,
            ShellCmdList,
            AskBeforeExecuteShell,
        )

//        private val entries1: List<Key> by lazy { // 发行版不可用，被优化掉了
//            getSealedChildren<Key> { kClass ->
//                kClass.objectInstance // 获取那些定义为 `object xxx: Key` 的单例
//            }
//        }

        fun getList(): List<String> = entries.map { it.keyId }

        fun getByKeyId(keyId: String): Key? = entries.firstOrNull { it.keyId == keyId }
    }
}