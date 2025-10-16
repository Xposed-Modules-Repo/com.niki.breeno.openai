package com.niki914.breeno.ui.activity

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.niki914.breeno.provider.messaging.NotificationUtils.getNotificationFromIntent
import com.niki914.breeno.requestPermission
import com.niki914.breeno.ui.compose.theme.BreenoTheme
import com.niki914.breeno.ui.navigation.MainNav
import com.niki914.breeno.viewmodel.MainIntent
import com.niki914.breeno.viewmodel.MainViewModel
import com.zephyr.log.setOnCaughtListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setOnCaughtListener { thread, throwable ->
            throwable.apply {
                val pair = ("应用发生异常: $message") to stackTraceToString()
                pair.report()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermission(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            BreenoTheme { // 应用动态颜色主题
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNav()
                }
            }
        }

        getNotificationFromIntent().report()
    }

    private fun Pair<String?, String?>.report() {
        if (first == null || second == null)
            return
        viewModel.sendIntent(MainIntent.ShowDialog(first!!, second!!))
    }
}