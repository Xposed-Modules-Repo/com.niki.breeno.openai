package com.niki914.breeno.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niki914.breeno.ui.navigation.Screens
import com.niki914.breeno.viewmodel.OSIntent
import com.niki914.breeno.viewmodel.OtherSettingsViewModel
import com.niki914.core.Key
import com.niki914.core.R.string
import com.niki914.core.parseToProxyPair

val horizontalDp = 15.dp
val verticalDp = 9.dp

fun OtherSettingsViewModel.saveValue(key: Key, value: Boolean) {
    sendIntent(OSIntent.SaveValue(key, value))
}

@Composable
fun OtherSettingsScreen(
    onNav: (route: String) -> Unit = {},
    onBack: () -> Unit = {}
) {
    val context = composableContext

    val viewModel: OtherSettingsViewModel = hiltViewModel()

    val state = viewModel.uiState.collectAsStateWithLifecycle()
    val proxy = state.value.proxy

    Scaffold(
        topBar = { SettingTopBar(onBack) }
    ) { paddingValues ->
        val bottomPadding = paddingValues.calculateBottomPadding() * 3

        Column(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding()) // 只处理顶部
                .verticalScroll(rememberScrollState()) // 允许内容滚动
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            StringSettingItem(
                key = Key.Proxy,
                currentValue = proxy,
                errorMsg = stringResource(string.proxy_error_msg),
                onChange = { str ->
                    viewModel.sendIntent(OSIntent.SaveValue(Key.Proxy, str))
                },
                validator = { str ->
                    val pair = str.parseToProxyPair()
                    (pair.first != null && pair.second != null) || str.isBlank()
                }
            )

            StringSettingItem(
                key = Key.FallbackToBreeno,
                currentValue = state.value.fallbackToBreeno,
                onChange = { str ->
                    viewModel.sendIntent(
                        OSIntent.SaveValue(
                            Key.FallbackToBreeno,
                            str
                        )
                    )
                }
            )

            ToggleSettingItem(
                Key.EnableShowToolCalling,
                state.value.enableShowToolCalling,
                onUpdated = {
                    viewModel.sendIntent(OSIntent.SaveValue(Key.EnableShowToolCalling, it))
                }
            )
            ToggleSettingItem(
                Key.EnableLaunchApp,
                state.value.enableLaunchApp,
                onUpdated = {
                    viewModel.sendIntent(OSIntent.SaveValue(Key.EnableLaunchApp, it))
                }
            )
            ToggleSettingItem(
                Key.EnableLaunchURI,
                state.value.enableLaunchUri,
                onUpdated = {
                    viewModel.sendIntent(OSIntent.SaveValue(Key.EnableLaunchURI, it))
                }
            )
            ToggleSettingItem(
                Key.EnableGetDeviceInfo,
                state.value.enableGetDeviceInfo,
                onUpdated = {
                    viewModel.sendIntent(OSIntent.SaveValue(Key.EnableGetDeviceInfo, it))
                }
            )
            ToggleSettingItem(
                Key.EnableShellCmd,
                state.value.enableRunShell,
                onUpdated = {
                    viewModel.saveValue(Key.EnableShellCmd, it)
                },
                shouldConsumeRowClick = {
                    onNav(Screens.ShellCmdSettings.route)
                    true // 不触发更改
                }
            )

            Text(
                text = stringResource(string.might_cause_error_msg),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = verticalDp)
                    .padding(horizontal = horizontalDp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(bottomPadding))
        }
    }
}

/**
 * 封装通用布尔设置项的 Composable。
 * 它负责从 ViewModel 的 UI 状态中获取当前值，并向 ViewModel 发送保存 Intent。
 *
 * @param key 设置项的唯一标识 Key。
 */
@Composable
fun ToggleSettingItem(
    key: Key,
    currentValue: Boolean,
    onUpdated: (Boolean) -> Unit = {},
    shouldConsumeRowClick: () -> Boolean = { false } // 是否消费来自行的点击而不切换 switch
) {
    BooleanSettingItem(
        key = key,
        currentValue = currentValue,
        onRowClicked = { boolean ->
            if (!shouldConsumeRowClick()) {
                onUpdated(boolean)
            }
        },
        onSwitchClicked = { boolean ->
            onUpdated(boolean)
        }
    )
}

@Composable
fun SettingTopBar(onBack: () -> Unit = {}) {
    BaseTopBar(
        string.other_settings_bar,
        navigationIcon = {
            IconButton(
                onClick = onBack,
                colors = iconColors
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, // 使用 AutoMirrored.Filled.ArrowBack
                    contentDescription = stringResource(string.back)
                )
            }
        }
    )
}