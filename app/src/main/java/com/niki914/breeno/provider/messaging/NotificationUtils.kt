package com.niki914.breeno.provider.messaging

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.niki914.breeno.R
import com.niki914.breeno.isPermissionGranted

object NotificationUtils {
    private const val CHANNEL_ID = "niki_breeno_report_channel"
    private const val CHANNEL_NAME = "错误报告" // TODO 修改命名
    private const val CHANNEL_DESCRIPTION = "模块错误报告"

    // --- 通知 ID 常量 ---
    private const val NOTIFICATION_ID = 4321

    private const val NOTIFICATION_TITLE_KEY = "notification_title"
    private const val NOTIFICATION_MSG_KEY = "notification_message"

    /**
     * 发送一个简单的通知。
     *
     * @param title 通知标题
     * @param message 通知内容
     */
    @SuppressLint("MissingPermission")
    fun <A : Activity> Context.sendNotification(
        title: String = "",
        message: String = "",
        activityClazz: Class<A>? = null
    ) {
        // 1. 创建通知渠道 (Android 8.0+ 仅需)
        createNotificationChannel()

        val pendingIntent = activityClazz?.let {
            buildPendingIntent(title, message, it)
        }


        // 3. 构建通知
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.bear) // 通知小图标，必填
            .setContentTitle(title) // 通知标题
            .setContentText(message) // 通知内容
            .setPriority(NotificationCompat.PRIORITY_MAX) // 通知优先级 (影响 Android 8.0 以下版本)
//        .setDefaults(NotificationCompat.DEFAULT_ALL) // 确保有声音和震动，有助于横幅显示
            .setCategory(NotificationCompat.CATEGORY_ERROR) // 设置通知类别
            .setAutoCancel(true) // 用户点击后自动关闭通知
            .apply {
                setContentIntent(pendingIntent)
            }

        // 4. 发送通知
        val notificationManager = NotificationManagerCompat.from(this)

        // 检查并发送通知
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (isPermissionGranted(Manifest.permission.POST_NOTIFICATIONS)) {
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        } else {
            // Android 13 以下版本无需运行时权限
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun <A : Activity> Context.buildPendingIntent(
        title: String,
        message: String,
        activityClazz: Class<A>
    ): PendingIntent {
        val intent =
            Intent(this, activityClazz).apply { // 替换为你的主 Activity 的完整路径
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(NOTIFICATION_TITLE_KEY, title)
                putExtra(NOTIFICATION_MSG_KEY, message)
//            putExtra("launched_from_pending_intent", true)
            }

        // 将 Intent 封装成 PendingIntent
        // Request Code: 0 (一个请求码，用于区分不同的 PendingIntent，如果你有多个的话)
        // Flags: PendingIntent.FLAG_UPDATE_CURRENT 表示如果 PendingIntent 已经存在，则更新其内部的 Intent
        //        或者使用 PendingIntent.FLAG_IMMUTABLE (API 23+) 更安全，表示创建的 PendingIntent 不可变
        return PendingIntent.getActivity(
            this,
            0, // Request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // FLAG_IMMUTABLE 在 API 23+ 推荐使用
        )
    }

    fun Activity.getNotificationFromIntent(): Pair<String?, String?> {
        val title = intent.getStringExtra(NOTIFICATION_TITLE_KEY)?.ifBlank { null }
        val msg = intent.getStringExtra(NOTIFICATION_MSG_KEY)?.ifBlank { null }
        return title to msg
    }

    /**
     * 创建通知渠道。
     * 在 Android 8.0 (API level 26) 及更高版本上，发送通知前必须创建通知渠道。
     */
    private fun Context.createNotificationChannel() {
        // 重要性级别，HIGH 或 MAX 才能显示横幅通知
        val importance = NotificationManager.IMPORTANCE_HIGH // 或 IMPORTANCE_MAX
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
            description = CHANNEL_DESCRIPTION
//        enableLights(true) // 启用闪光灯
//        enableVibration(true) // 启用震动
        }
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * 取消指定ID的通知。
     */
    fun Context.cancelNotification() {
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.cancel(NOTIFICATION_ID)
    }

    /**
     * 取消所有通知 (针对该应用)。
     */
    fun Context.cancelAllNotifications() {
        val notificationManager = NotificationManagerCompat.from(this)
        notificationManager.cancelAll()
    }
}