package com.niki914.breeno.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niki914.breeno.issueReport
import com.niki914.breeno.viewmodel.MainEvent
import com.niki914.breeno.viewmodel.MainIntent
import com.niki914.breeno.viewmodel.MainViewModel
import com.niki914.core.Key
import com.niki914.core.MainMenuChoices
import com.niki914.core.R.string
import kotlinx.coroutines.launch

/**
 * 整个大模型参数配置界面的 Composable 函数
 */
@Composable
fun MainSettingsScreen(
    onMenuItemClicked: (MainMenuChoices) -> Unit = {}
) {
    val context = composableContext

    val viewModel: MainViewModel = hiltViewModel()
    val state = viewModel.uiState.collectAsStateWithLifecycle()

    val url = state.value.url
    val apiKey = state.value.apiKey
    val modelName = state.value.modelName
    val systemPrompt = state.value.systemPrompt
    val timeout = state.value.timeout

    val savedStr = stringResource(string.saved)

    val performAutoCompleteStr = stringResource(string.perform_auto_complete)
    val suggestAutoCompleteStr = stringResource(string.suggest_auto_complete)

    val shouldShowDialog = state.value.shouldShowDialog
    val dialogTitle = state.value.dialogTitle
    val dialogContent = state.value.dialogContent

    val snackBarHostState = remember { SnackbarHostState() }
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
                MainEvent.SuggestURLAutoComplete -> {
                    launch { // 避免阻塞导致多次 toast
                        val result = snackBarHostState.showSnackbar(
                            message = suggestAutoCompleteStr,
                            actionLabel = performAutoCompleteStr,
                            withDismissAction = true,
                            duration = SnackbarDuration.Long
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            viewModel.sendIntent(MainIntent.NeedAutoComplete)
                        }
                    }
                }

                MainEvent.ConfigSaved -> {
                    toastState.showToast(toastInfo)
                }
            }
        }
    }

    Scaffold(
        topBar = { MainTopBar(onMenuItemClicked) },
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { paddingValues ->
        val bottomPadding = paddingValues.calculateBottomPadding() * 3

        Column(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding()) // 只处理顶部
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()) // 允许内容滚动
                .fillMaxSize()
        ) {
            Spacer(modifier = Modifier.height(10.dp))
            UrlInput(url) {
                viewModel.sendIntent(MainIntent.UpdateUIValue(Key.Url, it))
            }
            Spacer(modifier = Modifier.height(10.dp))
            ApiKeyInput(apiKey) {
                viewModel.sendIntent(MainIntent.UpdateUIValue(Key.ApiKey, it))
            }
            Spacer(modifier = Modifier.height(10.dp))
            ModelNameInput(modelName) {
                viewModel.sendIntent(MainIntent.UpdateUIValue(Key.ModelName, it))
            }
            Spacer(modifier = Modifier.height(10.dp))
            SystemPromptInput(systemPrompt) {
                viewModel.sendIntent(MainIntent.UpdateUIValue(Key.SystemPrompt, it))
            }
            Spacer(modifier = Modifier.height(10.dp))
            TimeoutInput(timeout) {
                viewModel.sendIntent(MainIntent.UpdateUIValue(Key.Timeout, it))
            }

            Spacer(modifier = Modifier.height(24.dp))
            SaveButton {
                viewModel.sendIntent(MainIntent.SaveValue(Key.ApiKey, apiKey))
                viewModel.sendIntent(MainIntent.SaveValue(Key.Url, url))
                viewModel.sendIntent(MainIntent.SaveValue(Key.ModelName, modelName))
                viewModel.sendIntent(MainIntent.SaveValue(Key.Timeout, timeout))
                viewModel.sendIntent(
                    MainIntent.SaveValue(
                        Key.SystemPrompt,
                        systemPrompt
                    )
                )
            }
            Spacer(modifier = Modifier.height(bottomPadding))
        }

        MainDialog(
            title = dialogTitle,
            shouldShow = shouldShowDialog,
            content = dialogContent,
            onDismiss = { confirmed ->
                viewModel.sendIntent(MainIntent.CloseDialog)
                if (confirmed) {
                    context.issueReport(dialogTitle, dialogContent)
                }
            }
        )

        Toast(toastInfoState = toastState)
    }
}

@Composable
fun MainTopBar(onMenuItemClicked: (MainMenuChoices) -> Unit = {}) {
    var showMenu by remember { mutableStateOf(false) } // 控制菜单显示隐藏
    BaseTopBar(
        string.app_bar,
        actions = {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(string.more_options)
                )
            }

            // 更多选项菜单
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                MainMenuChoices.entries.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(choice.uiString) },
                        onClick = {
                            showMenu = false
                            onMenuItemClicked(choice)
                        }
                    )
                }
            }
        }
    )
}

@Composable
fun SaveButton(
    modifier: Modifier = Modifier,
    onSaveClick: () -> Unit
) {
    ScalingButton(
        modifier = modifier
            .fillMaxWidth(),
        text = stringResource(string.save),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.W500
        ),
        backgroundColor = MaterialTheme.colorScheme.primary,
        onClick = {
            onSaveClick()
        },
        roundButton = false,
        hPaddingRange = 0..15,
        vPaddingRange = 0..5,
        radiusRange = 25..50,
    )
}

@Composable
fun MainDialog(
    shouldShow: Boolean,
    title: String,
    content: String,
    onDismiss: (confirmed: Boolean) -> Unit
) {
    if (shouldShow) {
        AlertDialog(
            onDismissRequest = {
                onDismiss(false)
            },
            title = { Text(title) },
            text = {
                Text(
                    text = content.take(150) + "...",
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDismiss(true)
                    }
                ) {
                    Text(stringResource(string.okay))
                }
            },
            dismissButton = {
                Button(onClick = {
                    onDismiss(false)
                }) {
                    Text(stringResource(string.cancel))
                }
            },
            modifier = Modifier.padding(20.dp)
        )
    }
}


/**
 * URL 输入框 Composable
 */
@Composable
fun UrlInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    val protocolRegex = "^https?://.*$".toRegex()
    // 当输入不为空且不匹配协议头时，判定为错误
    val hasError = value.isNotBlank() && !value.matches(protocolRegex)

    CommonOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = Key.Url.uiString,
        description = Key.Url.uiDescription,
        singleLine = true,
        isError = hasError,
        errorText = stringResource(string.url_error_msg),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
    )
}

/**
 * API Key 输入框 Composable，带隐藏字符功能
 */
@Composable
fun ApiKeyInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    CommonOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = Key.ApiKey.uiString,
        description = Key.ApiKey.uiDescription,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val image =
                if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            // 优化：为不同状态提供不同的无障碍描述
            val description =
                if (passwordVisible) stringResource(string.hide_key) else stringResource(
                    string.show_key
                )
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(imageVector = image, contentDescription = description)
            }
        }
    )
}

/**
 * 模型名称输入框 Composable
 */
@Composable
fun ModelNameInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    CommonOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        label = Key.ModelName.uiString,
        description = Key.ModelName.uiDescription
    )
}

/**
 * 超时 Composable
 */
@Composable
fun TimeoutInput(
    value: Long,
    onValueChange: (Long) -> Unit
) {
    // 使用 remember(value) 可以在外部 value 变化时同步更新内部 textState
    var textState by remember(value) { mutableStateOf(value.toString()) }

    fun String.trimToLongOrNull(): Long? {
        val str = this.trim()
        return str.toLongOrNull()
    }

    fun checkError(text: String): Boolean {
        // 当输入不为空，且（无法转换为Long 或 转换为负数）时，判定为错误
        val long = text.trimToLongOrNull()
        return text.isNotBlank() && (long ?: -1) < 0
    }

    val hasError = checkError(textState)

    CommonOutlinedTextField(
        value = textState,
        onValueChange = { newText ->
            textState = newText // 立即更新UI显示
            if (!checkError(newText)) {
                // 仅当输入有效（或为空）时，才向上层回调
                onValueChange(newText.trimToLongOrNull() ?: 0L)
            }
        },
        label = Key.Timeout.uiString,
        description = Key.Timeout.uiDescription,
        isError = hasError,
        errorText = stringResource(string.invalid_number_msg),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true // 修正：超时通常是单行数字
    )
}


/**
 * 系统提示词输入框 Composable
 */
@Composable
fun SystemPromptInput(
    value: String,
    onValueChange: (String) -> Unit
) {
    CommonOutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = Key.SystemPrompt.uiString,
        description = Key.SystemPrompt.uiDescription,
        singleLine = false,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp) // 保留特定修饰符
    )
}