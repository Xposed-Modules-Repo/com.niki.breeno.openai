package com.niki914.breeno.ui.compose


import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niki914.core.Key
import com.niki914.core.MainMenuChoices
import com.niki914.core.R.string

/**
 * 一个自定义 Modifier，用于实现按压时的缩放和形变动画效果。
 *
 * @param interactionSource 用于监听交互状态，如是否被按下。
 * @param hPaddingRange 水平内边距的动画范围。`start` 是按下时的值，`endInclusive` 是松开时的值。
 * @param vPaddingRange 垂直内边距的动画范围。`start` 是按下时的值，`endInclusive` 是松开时的值。
 * @param cornerRadiusRange 圆角半径的动画范围。`start` 是按下时的值，`endInclusive` 是松开时的值。
 * @param onVibrationThreshold 当按压达到阈值时（即开始按压时）触发的回调，通常用于震动。
 */
fun Modifier.pressableScaling(
    interactionSource: MutableInteractionSource,
    hPaddingRange: IntRange = 0..0,
    vPaddingRange: IntRange = 0..0,
    cornerRadiusRange: IntRange,
    onVibrationThreshold: (() -> Unit)? = null
): Modifier = pressableScaling(
    interactionSource,
    startPaddingRange = hPaddingRange,
    topPaddingRange = vPaddingRange,
    endPaddingRange = hPaddingRange,
    bottomPaddingRange = vPaddingRange,
    cornerRadiusRange = cornerRadiusRange,
    onVibrationThreshold = onVibrationThreshold
)

fun Modifier.pressableScaling(
    interactionSource: MutableInteractionSource,
    startPaddingRange: IntRange = 0..0,
    topPaddingRange: IntRange = 0..0,
    endPaddingRange: IntRange = 0..0,
    bottomPaddingRange: IntRange = 0..0,
    cornerRadiusRange: IntRange,
    onVibrationThreshold: (() -> Unit)? = null
): Modifier = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    // 当按压状态从未按下变为按下时，触发震动
    LaunchedEffect(isPressed) {
        if (isPressed) {
            onVibrationThreshold?.invoke()
                ?: haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    // 分别为四个方向的 padding 创建动画
    val startPadding by animateIntAsState(
        if (!isPressed) startPaddingRange.start else startPaddingRange.endInclusive,
        label = "startPadding"
    )
    val topPadding by animateIntAsState(
        if (!isPressed) topPaddingRange.start else topPaddingRange.endInclusive,
        label = "topPadding"
    )
    val endPadding by animateIntAsState(
        if (!isPressed) endPaddingRange.start else endPaddingRange.endInclusive,
        label = "endPadding"
    )
    val bottomPadding by animateIntAsState(
        if (!isPressed) bottomPaddingRange.start else bottomPaddingRange.endInclusive,
        label = "bottomPadding"
    )
    val cornerRadius by animateIntAsState(
        if (!isPressed) cornerRadiusRange.start else cornerRadiusRange.endInclusive,
        label = "cornerRadius"
    )

    this
        .padding(
            start = startPadding.dp,
            top = topPadding.dp,
            end = endPadding.dp,
            bottom = bottomPadding.dp
        )
        .clip(RoundedCornerShape(cornerRadius.dp))
        .clickable(
            interactionSource = interactionSource,
            indication = null, // 无水波纹效果
            onClick = {}
        )
}

@Composable
fun StringSettingItem(
    key: Key, // 传入整个 OtherSettings 枚举项
    currentValue: String = "", // 用于输入类型设置的当前字符串值
    errorMsg: String = "",
    onChange: (String) -> Unit = {}, // 用于输入类型，传入新的字符串值
    validator: (String) -> Boolean = { true } // 新增：校验回调，返回 true 表示通过
) {
    // 状态应该由外部传入，这里只是管理 UI 自身的瞬态状态
    var showInputDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf(currentValue) } // 使用传入的 currentValue 初始化
    var isError by remember { mutableStateOf(false) } // 新增：用于输入框的错误状态

    SettingRow(
        onClick = {
            // 点击时，将当前最新的值赋给 inputText，防止显示旧值
            inputText = currentValue
            isError = false // 每次打开对话框重置错误状态
            showInputDialog = true
        }
    ) {
        Column(
            modifier = Modifier
                .weight(0.7f)
                .padding(start = horizontalDp)
                .padding(vertical = verticalDp)
        ) {
            // 标题
            Text(
                text = key.uiString,
                style = titleTextStyle
            )
            // 描述 - 可能没有
            key.uiDescription?.let {
                if (it.isNotBlank()) {
                    Text(
                        text = it,
                        modifier = Modifier.padding(3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.padding(horizontal = 4.dp))

        // 预览
        Text(
            text = currentValue, // 显示外部传入的最新字符串值
            style = titleTextStyle,
            color = MaterialTheme.colorScheme.primary,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            modifier = Modifier
                .weight(0.3f)
                .padding(end = horizontalDp)
        )
    }

    // 输入对话框
    if (showInputDialog) {
        AlertDialog(
            onDismissRequest = { showInputDialog = false },
            title = { Text(key.uiString) },
            text = {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        isError = !validator(it) // 实时校验
                    },
                    singleLine = true,
                    isError = isError, // 应用错误状态
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri), // 常用作 URL 或 IP
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(string.input_value)) },
                    supportingText = {
                        if (isError) {
                            Text(errorMsg) // 错误提示
                        }
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isError) { // 只有在没有错误时才确认
                            onChange(inputText) // 传入新的值
                            showInputDialog = false
                        }
                    },
                    enabled = !isError // 按钮在有错误时禁用
                ) {
                    Text(stringResource(string.okay))
                }
            },
            dismissButton = {
                Button(onClick = { showInputDialog = false }) {
                    Text(stringResource(string.cancel))
                }
            },
            modifier = Modifier.padding(20.dp)
        )
    }
}

enum class ClickSource {
    Row,
    Switch
}

@Composable
fun BooleanSettingItem(
    key: Key, // 传入整个 OtherSettings 枚举项
    currentValue: Boolean = false, // 用于开关类型设置的当前布尔值
    onRowClicked: (Boolean) -> Unit = { }, // 用于开关类型，传入新的布尔值,
    onSwitchClicked: (Boolean) -> Unit = { }
) {
    SettingRow(
        // 1. 将切换状态的逻辑移到 onClick 回调中
        onClick = {
            onRowClicked(!currentValue)
        },
        // 2. 将 toggleable 提供的无障碍语义 (Accessibility) 手动添加回来
        //    这样可以确保屏幕阅读器等辅助功能仍然能正确识别它是一个开关。
        modifier = { this.semantics { role = Role.Switch } }
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    vertical = verticalDp,
                    horizontal = horizontalDp
                )
        ) {
            Text(
                text = key.uiString,
                style = titleTextStyle
            )
            key.uiDescription?.let {
                if (it.isNotBlank()) {
                    Text(
                        text = it,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(3.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.padding(horizontal = 4.dp))

        Switch(
            checked = currentValue,
            onCheckedChange = onSwitchClicked, // 保持为 null，因为父组件 Row 已经处理了点击事件
            modifier = Modifier
                .padding(vertical = verticalDp)
                .padding(end = horizontalDp)
                .pointerInput(Unit) {
                    detectTapGestures { /* 消费Switch区域的点击事件 */ }
                }
        )
    }
}

@Composable
fun SettingRow(
    modifier: Modifier.() -> Modifier = { this },
    onClick: () -> Unit = {},
    content: @Composable (RowScope.() -> Unit)
) {
    val haptic = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalDp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = {
                    onClick()
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress) // 触发震动
                    onClick()
                }
            )
            .run(modifier),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/**
 * [新增] 封装了通用样式的 OutlinedTextField，用于配置项输入。
 *
 * @param value 输入框的当前值。
 * @param onValueChange 值更改时的回调。
 * @param modifier 自定义修饰符。
 * @param label 输入框的标签文本。
 * @param description 正常的辅助/描述文本。
 * @param isError 是否处于错误状态。
 * @param errorText 当 isError 为 true 时显示的错误文本。
 * @param singleLine 是否为单行输入框。
 * @param visualTransformation 用于转换显示文本，如密码遮盖。
 * @param trailingIcon 输入框尾部的图标。
 * @param keyboardOptions 键盘选项，如设置键盘类型。
 * @param maxLines 最大行数。
 */
@Composable
fun CommonOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    label: String,
    description: String?,
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    maxLines: Int = if (singleLine) 1 else 10
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        supportingText = {
            val text = if (isError) errorText else description
            text?.let {
                Text(it)
            }
        },
        isError = isError,
        modifier = modifier,
        singleLine = singleLine,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        maxLines = maxLines
    )
}

@Composable
fun ScalingButton(
    modifier: Modifier = Modifier,
    text: String,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    hPaddingRange: IntRange,
    vPaddingRange: IntRange,
    radiusRange: IntRange,
    backgroundColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    roundButton: Boolean = true
) {
    fun Modifier.thenIf(
        condition: Boolean,
        modifier: Modifier.() -> Modifier
    ): Modifier {
        return if (condition) {
            this.then(modifier())
        } else this
    }

    val haptic = LocalHapticFeedback.current
    val shouldVibrate = remember { false }
    val isPressed by interactionSource.collectIsPressedAsState()

    val hPadding by animateIntAsState(
        targetValue = if (isPressed) hPaddingRange.start else hPaddingRange.endInclusive
    )
    val vPadding by animateIntAsState(
        targetValue = if (isPressed) vPaddingRange.start else vPaddingRange.endInclusive
    )
    val cornerRadius by animateIntAsState(
        targetValue = if (isPressed) radiusRange.start else radiusRange.endInclusive
    )

    Box(
        modifier = modifier
            .semantics { role = Role.Button }
            .defaultMinSize(
                minWidth = ButtonDefaults.MinWidth + hPaddingRange.endInclusive.dp * 2,
                minHeight = ButtonDefaults.MinHeight * 1.2F + vPaddingRange.endInclusive.dp * 2
            )
            .padding(
                horizontal = hPadding.dp,
                vertical = vPadding.dp
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(backgroundColor)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = {
                    onClick()
                    if (shouldVibrate) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                },
                onLongClick = {
                    onLongClick?.invoke()
                    if (shouldVibrate) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
            )
            .thenIf(roundButton) {
                aspectRatio(1f)
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.contentColorFor(backgroundColor),
            style = textStyle
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseTopBar(
    @StringRes titleId: Int,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(stringResource(titleId)) },
        colors = topBarColors,
        navigationIcon = navigationIcon,
        actions = actions
    )
}

val composableContext: Context
    @Composable
    get() {
        return LocalContext.current
    }

val Int.resString: String
    @Composable
    get() {
        return stringResource(this)
    }

val Key.uiString: String
    @Composable
    get() {
        return stringResource(uiStringRes)
    }

val Key.uiDescription: String?
    @Composable
    get() {
        uiDescriptionRes?.let {
            return stringResource(it)
        }
        return null
    }

val MainMenuChoices.uiString: String
    @Composable
    get() {
        return stringResource(uiStringRes)
    }

val titleTextStyle: TextStyle
    @Composable
    get() {
        return MaterialTheme.typography.bodyMedium.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.W500
        )
    }

val iconColors: IconButtonColors
    @Composable
    get() = IconButtonDefaults.iconButtonColors(
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer // 图标颜色与 TopAppBar 的 Action Icon 颜色保持一致
    )

@OptIn(ExperimentalMaterial3Api::class)
val topBarColors: TopAppBarColors
    @Composable
    get() = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer, // AppBar 的背景色
        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer, // 标题颜色
        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer // Action Icon 颜色
    )