package com.niki914.tool.call.models

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.annotation.Keep
import com.niki914.chat.beans.PropertyDefinition
import com.niki914.core.ToolsNames
import com.niki914.core.getAs
import com.niki914.core.logE
import com.niki914.tool.call.utils.AppInfoCache
import kotlinx.serialization.json.JsonObject

@Keep
internal data object LaunchAppModel : BaseToolModel() {
    override val name: String = ToolsNames.LAUNCH_APP
    override val description: String =
        "Launch an Android application using its package name or fuzzy matching by app name. " +
                "This tool starts the specified app through the Android ActivityManager system service. " +
                "**Important: If the provided 'app_name' is short or ambiguous (e.g., 'ins'), " +
                "you should infer the most likely full app name (e.g., 'Instagram') " +
                "and pass that inferred full name to call the tool, without asking the user for confirmation.**"

    override val properties: Map<String, PropertyDefinition>
        get() = mapOf(
            "package_name" to PropertyDefinition(
                type = "string",
                description = "The package name of the Android application to launch (e.g., 'com.android.chrome', 'com.whatsapp')"
            ),
            "app_name" to PropertyDefinition(
                type = "string",
                description = "The application name of the Android application to fuzzy match and launch (e.g., '微信', 'Chrome')"
            )
        )

    override val required: List<String> = listOf()

    private val cache by lazy {
        AppInfoCache.getInstance()
    }

    private sealed class LaunchAppEvent {
        data object Launched : LaunchAppEvent()
        data class BestMatches(val list: List<String>) : LaunchAppEvent()
        data object NotFound : LaunchAppEvent()
        data class Error(val throwable: Throwable) : LaunchAppEvent()
    }

    /**
     * 尝试启动指定包名的应用
     */
    private fun Context.startApp(packageName: String): LaunchAppEvent {
        val packageManager: PackageManager = this.packageManager
        val launchIntent: Intent? = packageManager.getLaunchIntentForPackage(packageName)

        return if (launchIntent != null) {
            // 为确保在新任务中启动，防止与当前应用的任务栈混淆
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                this.startActivity(launchIntent)
                LaunchAppEvent.Launched
            } catch (t: Throwable) {
                LaunchAppEvent.Error(t)
            }
        } else {
            LaunchAppEvent.Error(Exception("无法找到包名为 $packageName 的启动 Intent"))
        }
    }

    override suspend fun callInternal(
        args: JsonObject,
        extras: JsonObject,
        application: Application?
    ): JsonObject {
        val packageName = args.getAs<String>("package_name")
        val appName = args.getAs<String>("app_name")

        if (packageName == null && appName == null)
            return illegalArgumentJson()

        if (application == null)
            return exceptionCaughtJson("上下文实例为空，无法启动应用")

        val result: LaunchAppEvent = if (packageName != null) {
            application.startApp(packageName)
        } else {
            // 太慢了
//            val findResult = cache.findPackageNameByAppNameSmart(appName!!)
            val findResult = cache.findPackageNameByAppName(appName!!)

            when (findResult) {
                is AppInfoCache.FindAppResult.BestMatches -> {
                    LaunchAppEvent.BestMatches(findResult.packageNames)
                }

                is AppInfoCache.FindAppResult.Found -> {
                    application.startApp(findResult.packageName)
                }

                AppInfoCache.FindAppResult.NotFound -> {
                    LaunchAppEvent.NotFound
                }
            }
        }

        return when (result) {
            is LaunchAppEvent.BestMatches -> {
                val appListString = result.list.joinToString(separator = ", ")

                if (result.list.isNotEmpty()) {
                    simpleResultJson("未精确匹配成功，找到以下应用，请用最匹配的结果再次调用此工具: $appListString")
                } else {
                    simpleResultJson("未精确匹配成功，但获取匹配列表为空")
                }
            }

            is LaunchAppEvent.Error -> {
                logE("启动应用错误: ($appName $packageName)", result.throwable)
                exceptionCaughtJson(result.throwable.message ?: "未知错误")
            }

            LaunchAppEvent.Launched -> {
                LaunchAppEvent.Launched
                simpleResultJson("操作执行成功: $appName $packageName")
            }

            LaunchAppEvent.NotFound -> {
                LaunchAppEvent.Launched
                simpleResultJson("未找到应用: $appName $packageName")
            }
        }
    }
}