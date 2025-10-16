package com.niki914.breeno.provider.messaging

import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.net.Uri
import androidx.core.net.toUri
import com.niki914.breeno.ui.activity.MainActivity
import com.niki914.core.logE
import com.niki914.core.utils.EmptyContentProvider

/**
 * 目的是通过 provider 让模块在出错时或其他情况下通过主应用发送通知
 */
class MessagingProvider : EmptyContentProvider() {

    companion object {
        fun sendNotificationWithProvider(
            context: Context,
            title: String?,
            content: String? = null,
            shouldStartActivity: Boolean = false
        ) {
            val queryUri = getUri()

            val shouldStartActivityInt = if (shouldStartActivity) {
                1
            } else {
                0
            }

            val contentValues = ContentValues().apply {
                put(COLUMN_TITLE, title)
                put(COLUMN_CONTENT, content)
                put(COLUMN_SHOULD_START_ACTIVITY, shouldStartActivityInt)
            }
            context.contentResolver?.insert(queryUri, contentValues)
        }

        // ContentProvider 的 Authority，必须在 AndroidManifest.xml 中声明
        private const val AUTHORITY = "com.niki914.breeno.messaging.provider"

        private const val PATH = "notify"

        // 接收错误信息的 URI
        private fun getUri(): Uri = "content://$AUTHORITY/$PATH".toUri()

        // URI 匹配码
        private const val CODE_POST_NOTIFICATION = 1

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH).apply {
            addURI(AUTHORITY, PATH, CODE_POST_NOTIFICATION)
        }

        // 用于传递错误信息和堆栈跟踪的键名
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_CONTENT = "content"
        private const val COLUMN_SHOULD_START_ACTIVITY = "should_start_activity"
    }

    override fun onCreate(): Boolean {
        return true
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return when (uriMatcher.match(uri)) {
            CODE_POST_NOTIFICATION -> {
                val title = values?.getAsString(COLUMN_TITLE) ?: ""
                val content = values?.getAsString(COLUMN_CONTENT) ?: ""
                val shouldStartActivity = values?.getAsInteger(COLUMN_SHOULD_START_ACTIVITY) == 1

                val clazz = if (shouldStartActivity) {
                    MainActivity::class.java
                } else {
                    null
                }

                NotificationUtils.apply {
                    context?.sendNotification(
                        title,
                        content,
                        clazz
                    )
                }

                logE("收到 Hook 模块的消息:\n标题:\n$title\n内容:\n$content")
                uri // 返回接收到的 URI，表明处理成功
            }

            else -> {
                null // 忽略
            }
        }
    }
}