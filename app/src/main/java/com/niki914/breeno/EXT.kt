package com.niki914.breeno

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri

/**
 * 检查权限状态, 若未授予则先获取权限, 最后回调检查结果
 */
fun ComponentActivity.withPermission(
    name: String,
    callback: (isGranted: Boolean) -> Unit = {}
) {
    if (isPermissionGranted(name)) {
        callback(true)
    } else {
        requestPermission(name, callback)
    }
}

fun Context.issueReport(title: String = "模块发生错误", body: String = "") {
    val githubIssueUrl =
        "https://github.com/Xposed-Modules-Repo/com.niki.breeno.openai/issues/new?title=$title&body=$body"
    val intent = Intent(Intent.ACTION_VIEW, githubIssueUrl.toUri())
    startActivity(intent)
}

fun Context.aboutMe() {
    val githubPageUrl =
        "https://github.com/niki914"
    val intent = Intent(Intent.ACTION_VIEW, githubPageUrl.toUri())
    startActivity(intent)
}

/**
 * 尝试获取权限, 然后回调结果
 *
 * @param name Manifest.permission.XXX
 *
 * startActivityForResult 的简化版
 */
fun ComponentActivity.requestPermission(
    name: String,
    callback: (isGranted: Boolean) -> Unit = {}
) = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
    callback(it)
}.launch(name)

/**
 * 是否给予了权限
 */
fun Context.isPermissionGranted(name: String): Boolean = ContextCompat.checkSelfPermission(
    this,
    name
) == PackageManager.PERMISSION_GRANTED
