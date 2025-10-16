package com.niki914.breeno.ui.compose

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.niki914.breeno.viewmodel.CTEvent
import com.niki914.breeno.viewmodel.CTIntent
import com.niki914.breeno.viewmodel.ChatTestingViewModel
import com.niki914.breeno.viewmodel.bean.MessagePair
import com.niki914.breeno.viewmodel.fixToUIMessage
import com.niki914.core.R
import com.niki914.core.R.string
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ChatTestingScreen(
    viewModel: ChatTestingViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val toastState = remember { mutableStateOf<ToastInfo?>(null) }

    val generatingStr = stringResource(string.saved)

    val toastInfo = ToastInfo(
        message = generatingStr,
        icon = Icons.Default.HourglassTop,
        duration = 1000L,
        backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f),
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )

    // Toast 等一次性事件的处理
    LaunchedEffect(key1 = viewModel.uiEvent) {
        viewModel.uiEvent.collectLatest { event ->
            // 在这里处理 Toast 或 SnackBar
            when (event) {
                is CTEvent.Generating -> {
                    toastState.showToast(toastInfo)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            ChatTestingTopBar(
                isGenerating = state.isGenerating,
                hasHistory = state.messagePairs.isNotEmpty(),
                onBack = onBack,
                onIntent = {
                    viewModel.sendIntent(it)
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = 16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            // 聊天列表，占据剩余空间
            ChatList(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                messagePairs = state.messagePairs
            )

            // 输入栏固定在底部
            ChatInputBar(
                modifier = Modifier.padding(
                    top = 5.dp,
                    bottom = paddingValues.calculateBottomPadding()
                ),
                input = state.input,
                onIntent = {
                    viewModel.sendIntent(it)
                }
            )
        }

        Toast(toastInfoState = toastState)
    }
}

@Composable
fun ChatTestingTopBar(
    isGenerating: Boolean,
    hasHistory: Boolean,
    onBack: () -> Unit,
    onIntent: (CTIntent) -> Unit
) {
    BaseTopBar(
        R.string.chat_testing_bar, // 硬编码标题
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(string.back)
                )
            }
        },
        actions = {
            if (isGenerating) {
                IconButton(onClick = { onIntent(CTIntent.Stop) }) {
                    Icon(
                        imageVector = Icons.Default.StopCircle,
                        contentDescription = stringResource(string.stop_generating),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            } else if (hasHistory) {
                IconButton(onClick = { onIntent(CTIntent.Clear) }) {
                    Icon(
                        imageVector = Icons.Default.ClearAll,
                        contentDescription = stringResource(string.clear_conversation)
                    )
                }
            } else {
                IconButton(onClick = { }) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = ""
                    )
                }
            }
        }
    )
}

@Composable
fun ChatList(
    modifier: Modifier = Modifier,
    messagePairs: List<MessagePair>
) {
    val listState = rememberLazyListState()

    // 当消息列表变化时，自动滚动到底部
    LaunchedEffect(messagePairs.size) {
        if (messagePairs.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        reverseLayout = true,
        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.Top)
    ) {
        itemsIndexed(messagePairs.reversed()) { index, pair ->
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                UserMessageBubble(text = pair.userMessage.content)
                AssistantMessageBubble(pair = pair)
            }
        }
    }
}

@Composable
fun UserMessageBubble(text: String) {
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .pressableScaling(
                    interactionSource = interactionSource,
                    endPaddingRange = 0..15,
                    cornerRadiusRange = 2..27
                )
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(20.dp, 2.dp, 20.dp, 22.dp)
                )
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}

@Composable
fun AssistantMessageBubble(pair: MessagePair) {
    val interactionSource = remember { MutableInteractionSource() }

    val colors = when (pair.state) {
        MessagePair.State.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .pressableScaling(
                    interactionSource = interactionSource,
                    startPaddingRange = 0..15,
                    cornerRadiusRange = 2..27
                )
                .background(
                    color = colors.first,
                    shape = RoundedCornerShape(2.dp, 20.dp, 22.dp, 20.dp)
                )
        ) {
            val combinedText = pair.aiMessage.fixToUIMessage().content ?: ""
            if (pair.state == MessagePair.State.WAITING && combinedText.isEmpty()) {
                TypingIndicator()
            } else {
                Text(
                    text = combinedText,
                    color = colors.second,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
fun ChatInputBar(
    modifier: Modifier = Modifier,
    input: String,
    onIntent: (CTIntent) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = { onIntent(CTIntent.UpdateInput(it)) },
            modifier = Modifier.weight(1f),
            placeholder = { Text(stringResource(string.ask_anything)) },
            shape = RoundedCornerShape(24.dp)
        )
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = { onIntent(CTIntent.Chat(input)) },
            enabled = input.isNotBlank(),
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(string.send)
            )
        }
    }
}

/**
 * 加载中样式
 */
@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing-indicator")

    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        (1..3).forEach { index ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, delayMillis = index * 100, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ), label = "dot-alpha-$index"
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .alpha(alpha)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant)
            )

        }
    }
}