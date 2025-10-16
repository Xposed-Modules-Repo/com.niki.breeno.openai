package com.niki914.tool.call.models

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.Keep
import com.niki914.chat.beans.PropertyDefinition
import com.niki914.core.ToolsNames
import com.niki914.core.getAs
import com.niki914.core.logE
import com.niki914.core.logV
import kotlinx.serialization.json.JsonObject
import androidx.core.net.toUri

@Keep
internal data object LaunchUriModel : BaseToolModel() {
    override val name: String = ToolsNames.LAUNCH_URI
    override val description: String =
        "Launch a URI using Android's Intent system. " +
                "This tool is exceptionally versatile and should be considered in virtually any scenario where you need to open: " +
                "web URLs (e.g., https://example.com), " +
                "maps (e.g., geo:latitude,longitude), " +
                "email clients (e.g., mailto:someone@example.com), " +
                "phone dialers (e.g., tel:1234567890), " +
                "or trigger other URI schemes (like deep links to specific app content) supported by the Android system or installed applications. " +
                "Its broad utility makes it a primary choice for various interaction flows."

    override val properties: Map<String, PropertyDefinition>
        get() = mapOf(
            "uri" to PropertyDefinition(
                type = "string",
                description = "The URI to launch. Can be a web URL (http/https), deep link, or other supported URI scheme"
            )
        )

    override val required: List<String> = listOf("uri")

    /**
     * 尝试使用 ACTION_VIEW 打开指定的 URI。
     * 适用于打开网页、拨号、地图等。
     */
    private fun Context.openUri(uriString: String): Result<Any> {
        val uri = try {
            uriString.toUri()
        } catch (t: Throwable) {
            return Result.failure(t)
        }

        val viewIntent = Intent(Intent.ACTION_VIEW, uri)
        // 同样需要添加 FLAG_ACTIVITY_NEW_TASK
        viewIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        return try {
            this.startActivity(viewIntent)
            logV("成功打开 URI: $uriString")
            Result.success(Any())
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    override suspend fun callInternal(
        args: JsonObject,
        extras: JsonObject,
        application: Application?
    ): JsonObject {
        val uri = args.getAs<String>("uri") ?: return illegalArgumentJson()

        val result = application?.openUri(uri)
            ?: Result.failure(Exception("上下文实例为空，无法启动 Uri"))

        return if (result.isSuccess) {
            logV(" 成功启动 Uri: $uri")
            simpleResultJson("操作执行成功")
        } else {
            val t = result.exceptionOrNull()
            logE("打开 Uri 出错 ($uri)", t)
            exceptionCaughtJson(t?.message ?: "未知错误")
        }
    }
}