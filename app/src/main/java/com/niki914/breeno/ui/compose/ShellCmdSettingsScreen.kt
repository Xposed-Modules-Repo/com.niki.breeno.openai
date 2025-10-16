package com.niki914.breeno.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niki914.breeno.viewmodel.SCSEvent
import com.niki914.breeno.viewmodel.SCSIntent
import com.niki914.breeno.viewmodel.ShellCmdSettingsViewModel
import com.niki914.core.Key
import com.niki914.core.R.string

/**
 * 整个大模型参数配置界面的 Composable 函数
 */
@Composable
fun ShellCmdSettingsScreen(
    onBack: () -> Unit = {}
) {
    val context = composableContext

    val viewModel: ShellCmdSettingsViewModel = hiltViewModel()
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    val enableRoot = state.value.enableRoot
    val isBlackList = state.value.isBlackList
    val list = state.value.list
//    val askBeforeExec = state.value.askBeforeExec

    val savedStr = stringResource(string.saved)

    val toastState = remember { mutableStateOf<ToastInfo?>(null) }

    val toastInfo = ToastInfo(
        message = savedStr,
        icon = Icons.Default.CheckCircle,
        duration = 1000L,
        backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )

    LaunchedEffect(key1 = viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                SCSEvent.ConfigSaved -> {
                    toastState.showToast(toastInfo)
                }
            }
        }
    }

    Scaffold(
        topBar = { ShellCmdTopBar(onBack) }
    ) { paddingValues ->
        val bottomPadding = paddingValues.calculateBottomPadding() * 3

        Column(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding()) // 只处理顶部
                .verticalScroll(rememberScrollState()) // 允许内容滚动
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(string.shell_unsafe_warn_msg),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = verticalDp)
                    .padding(horizontal = horizontalDp),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))
            ToggleSettingItem(
                Key.EnableRootAccessForShellCmd,
                enableRoot,
                onUpdated = {
                    viewModel.sendIntent(
                        SCSIntent.SaveValue(
                            Key.EnableRootAccessForShellCmd,
                            it
                        )
                    )
                }
            )
            ToggleSettingItem(
                Key.IsShellUsingBlackList,
                isBlackList,
                onUpdated = {
                    viewModel.sendIntent(SCSIntent.SaveValue(Key.IsShellUsingBlackList, it))
                }
            )

            CommonOutlinedTextField(
                value = list,
                onValueChange = { newValue ->
                    viewModel.sendIntent(SCSIntent.UpdateUIValue(Key.ShellCmdList, newValue))
                },
                label = Key.ShellCmdList.uiString,
                description = Key.ShellCmdList.uiDescription,
                singleLine = false,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
                    .heightIn(min = 120.dp) // 保留特定修饰符
            )

            // 没实现，还没试过能不能实现
//            ToggleSettingItem(
//                Key.AskBeforeExecuteShell,
//                askBeforeExec,
//                onUpdated = {
//                    viewModel.sendIntent(SCSIntent.SaveValue(Key.AskBeforeExecuteShell, it))
//                }
//            )

            Spacer(modifier = Modifier.height(24.dp))
            SaveButton(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                viewModel.sendIntent(SCSIntent.SaveValue(Key.ShellCmdList, list))
            }

            Spacer(modifier = Modifier.height(bottomPadding))
        }

        Toast(toastInfoState = toastState)
    }
}

@Composable
fun ShellCmdTopBar(onBack: () -> Unit = {}) {
    BaseTopBar(
        string.shell_cmd_settings_bar,
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