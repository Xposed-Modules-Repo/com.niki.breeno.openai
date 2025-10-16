package com.niki914.breeno.viewmodel

import androidx.lifecycle.viewModelScope
import com.niki914.breeno.repository.MainSettingsRepository
import com.niki914.breeno.repository.OtherSettingsRepository
import com.niki914.breeno.viewmodel.base.BaseMVIViewModel
import com.niki914.breeno.viewmodel.bean.MessagePair
import com.niki914.chat.Chat
import com.niki914.chat.beans.ChatEvent
import com.niki914.chat.beans.Message
import com.niki914.core.logRelease
import com.niki914.core.logV
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CTIntent {
    //    data class EditMessage(val index: Int, val newValue: String) : CTIntent() // 暂不实现
    data class UpdateInput(val text: String) : CTIntent()
    data class Chat(val msg: String) : CTIntent()
    data object Clear : CTIntent()
    data object PreConnect : CTIntent()
    data object Stop : CTIntent()
}

data class CTState(
    val messagePairs: List<MessagePair>,
    val input: String,
    val isGenerating: Boolean
)

sealed class CTEvent {
    data object Generating : CTEvent()
}

@HiltViewModel
class ChatTestingViewModel @Inject constructor(
    val mainRepo: MainSettingsRepository,
    val otherRepo: OtherSettingsRepository
) : BaseMVIViewModel<CTIntent, CTState, CTEvent, Any>(Any()) {

    override fun Any.initUiState(): CTState {
        return CTState(
            listOf(
//                MessagePair(
//                    "asdasdasd, asfoaf, sdadada",
//                    "asdasdasd, asfoaf, sdadadaasdasdasd, asfoaf, sdadadaasdasdasd, asfoaf, sdadada",
//                ),
//                MessagePair(
//                    "asdasdasd, asfoaf, sdadada",
//                    "",
//                    MessagePair.State.WAITING
//                )
            ),
            "",
            false
        )
    }

    // chat 不会自动添加消息到队列，全部需要自行添加
    private var chat = Chat("", "", null, null)

    private var chatJob: Job? = null

    private fun refreshConfig() {
        chat.updateConfig {
            apiKey = mainRepo.getAPIKey()
            modelName = mainRepo.getModelName()
            prompt = mainRepo.getSystemPrompt()

            val proxy = otherRepo.getProxy()

            network {
                baseUrl = mainRepo.getUrl()
                connectTimeout = mainRepo.getTimeout()
                if (proxy.first != null && proxy.second != null)
                    socksProxy(proxy.first!!, proxy.second!!)
            }

            logRelease("配置已刷新: " + this@updateConfig.toString())
        }
    }

    private fun handleError(errorString: String) {
        updateState {
            val last = messagePairs
                .last()
                .toErrorMessagePair(errorString) // 没有 last 又怎么会发生错误呢？因此断言此处不会异常
            copy(
                messagePairs = messagePairs.dropLast(1) + last,
                isGenerating = false
            )
        }
    }

    private fun chat(query: String) {
        if (uiState.value.isGenerating) {
            sendEvent(CTEvent.Generating)
            return
        }
        chatJob?.cancel()
        updateState { copy(isGenerating = false) }
        chatJob = viewModelScope.launch(Dispatchers.IO) {
            refreshConfig()
            val newPair = MessagePair.newPendingPair(query)

            updateState {
                copy(
                    messagePairs = messagePairs.filter {
                        it.state != MessagePair.State.WAITING
                    } + newPair,
                    input = ""
                )
            }
            chat.sendMessages(uiState.value.messagePairs.toValidMessages())
                .catch { t ->
                    val errorString = "错误:\n${t.message}"

                    logRelease("发生异常并捕捉，终止流: $errorString", t)
                    handleError(errorString)
                }
                .collect { data ->
                    when (data) {
                        ChatEvent.Started -> {
                            logV("流开始")
                            updateState {
                                copy(
                                    messagePairs = messagePairs.editWaitingPair {
                                        copy(
                                            aiMessage = listOf(Message.Assistant(""))
                                        )
                                    },
                                    isGenerating = true
                                )
                            }
                        }

                        is ChatEvent.Content -> {
                            updateState {
                                copy(
                                    messagePairs = messagePairs.editAiMessageInWaitingPair {
                                        val last = aiMessage.lastOrNull()
                                        val newContent = data.content

                                        return@editAiMessageInWaitingPair when (last) {
                                            is Message.Assistant -> {
                                                aiMessage.dropLast(1) + last.copy(content = last.content + newContent)
                                            }

//                                            is Message.Tool -> {
//                                                aiMessage + Message.Assistant(newContent)
//                                            }

                                            else -> {
                                                aiMessage
                                            }
                                        }
                                    }
                                )
                            }
                        }

                        is ChatEvent.Completed -> {
                            // 1. 检查是否有异常，有则在 UI 显示然后终止 flow
                            data.throwable?.apply {
                                val errorString = "错误:\n${this.message}"

                                logRelease("由于异常完成对话回合，终止流: $errorString", this)
                                handleError(errorString)
                                return@collect
                            }

                            updateState {
                                copy(
                                    messagePairs = messagePairs.editWaitingPair {
                                        copy(
                                            state = MessagePair.State.SUCCESS
                                        )
                                    },
                                    isGenerating = false
                                )
                            }
                        }

                        else -> {}
                    }
                }
        }
    }

    private fun preConnect() = chat.preConnect()

    override fun handleIntent(intent: CTIntent) {
        logV("接受 intent: ${intent.javaClass.simpleName}")
        when (intent) {
            is CTIntent.UpdateInput -> {
                updateState { copy(input = intent.text) }
            }

            is CTIntent.Chat -> {
                chat(intent.msg)
            }

            is CTIntent.Clear -> {
                chatJob?.cancel()
                updateState {
                    copy(
                        messagePairs = listOf(),
                        isGenerating = false
                    )
                }
            }

            is CTIntent.PreConnect -> {
                preConnect()
            }

            CTIntent.Stop -> {
                if (uiState.value.isGenerating) {
                    chatJob?.cancel()
                    updateState {
                        copy(
                            messagePairs = messagePairs.editWaitingPair {
                                val firstMessage = aiMessage.firstOrNull()
                                val lastMessage = aiMessage.lastOrNull()
                                if (
                                    firstMessage == null
                                    || lastMessage !is Message.Assistant
                                    || !lastMessage.toolCalls.isNullOrEmpty() /*防止工具调用协议导致的服务器报错*/
                                    || firstMessage.content.isNullOrBlank()
                                ) {
                                    copy(state = MessagePair.State.ERROR) // 没有保留的意义
                                } else {
                                    copy(state = MessagePair.State.SUCCESS)
                                }
                            },
                            isGenerating = false
                        )
                    }
                }
            }
        }
    }
}

fun List<MessagePair>.toValidMessages(): List<Message> {
    val list = mutableListOf<Message>()
    forEach { pair ->
        when (pair.state) {
            MessagePair.State.WAITING -> {
                list.add(pair.userMessage)
            }

            MessagePair.State.SUCCESS -> {
                list.add(pair.userMessage)
                list.addAll(pair.aiMessage)
            }

            MessagePair.State.ERROR -> {}
        }
    }
    return list.toList()
}

fun List<MessagePair>.toUIMessages(): List<Message> {
    val list = mutableListOf<Message>()
    forEachIndexed { index, pair ->
        val aiMessages = pair.aiMessage.filter { it is Message.Assistant }

        when (pair.state) {
            MessagePair.State.ERROR -> {
                if (index == lastIndex) {
                    list.add(pair.userMessage)
                    list.add(aiMessages.fixToUIMessage())
                }
            }

            else -> {
                list.add(pair.userMessage)
                list.add(aiMessages.fixToUIMessage())
            }
        }
    }
    return list.toList()
}

fun List<MessagePair>.editAiMessageInWaitingPair(block: MessagePair.() -> List<Message>): List<MessagePair> {
    return editWaitingPair {
        copy(aiMessage = block())
    }
}

fun List<MessagePair>.editWaitingPair(block: MessagePair.() -> MessagePair): List<MessagePair> {
    val pair = lastOrNull {
        it.state == MessagePair.State.WAITING
    }

    if (pair == null) {
        return this
    }

    return dropLast(1) + pair.block()
}

fun List<Message>.fixToUIMessage(): Message.Assistant {
    val aiSb = StringBuilder()

    forEachIndexed { index, msg ->
        logRelease(msg.toString())

        val content = msg.content
        if (msg is Message.Assistant && !content.isNullOrBlank()) {
            aiSb.append(content)
        }

        if (lastIndex != index) {
            aiSb.append("\n")
        }
    }

    if (aiSb.isBlank()) {
        aiSb.clear()
    }

    return Message.Assistant(aiSb.toString())
}

fun MessagePair.toErrorMessagePair(errorString: String): MessagePair {
    val aiSb = StringBuilder()

    logRelease("对话错误: $errorString")
    aiMessage.forEach { msg ->
        logRelease(msg.toString())

        val content = msg.content
        if (msg is Message.Assistant && !content.isNullOrBlank()) {
            aiSb.append(content + "\n")
        }
    }

    if (aiSb.isBlank()) {
        aiSb.clear()
    }

    aiSb.append(errorString)

    return copy(
        aiMessage = listOf(Message.Assistant(aiSb.toString())),
        state = MessagePair.State.ERROR
    )
}