package com.niki914.breeno.viewmodel

import androidx.lifecycle.viewModelScope
import com.niki914.breeno.repository.MainSettingsRepository
import com.niki914.breeno.viewmodel.base.BaseMVIViewModel
import com.niki914.core.Key
import com.zephyr.log.logI
import com.zephyr.provider.TAG
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MainIntent {
    data class UpdateUIValue(val key: Key, val value: Any) : MainIntent()
    data class SaveValue(val key: Key, val value: Any) : MainIntent()
    data object ReloadFromRepository : MainIntent()

    data class ShowDialog(val title: String, val content: String) : MainIntent()
    data object NeedAutoComplete : MainIntent()
    data object CloseDialog : MainIntent()
}

data class MainState(
    val apiKey: String,
    val url: String,
    val modelName: String,
    val systemPrompt: String,
    val timeout: Long,

    val shouldShowDialog: Boolean = false,
    val dialogTitle: String = "",
    val dialogContent: String = ""
)

sealed class MainEvent {
    data object SuggestURLAutoComplete : MainEvent()
    data object ConfigSaved : MainEvent()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    repo: MainSettingsRepository
) : BaseMVIViewModel<MainIntent, MainState, MainEvent, MainSettingsRepository>(repo) {

    override fun MainSettingsRepository.initUiState(): MainState {
        return MainState(
            getAPIKey(),
            getUrl(),
            getModelName(),
            getSystemPrompt(),
            getTimeout()
        )
    }

    init {
        viewModelScope.launch {
            delay(200)
            val url = uiState.value.url
            if (url.canBeAutoCompleted()) {
                sendEvent(MainEvent.SuggestURLAutoComplete)
            }
        }
    }

    override fun handleIntent(intent: MainIntent) {
        logI(TAG, "接受 intent: ${intent.javaClass.simpleName}")
        when (intent) {
            is MainIntent.UpdateUIValue -> {
                updateStateByIntent(intent.key, intent.value)
            }

            is MainIntent.SaveValue -> {
                val value = if (intent.key is Key.Url) {
                    val url = intent.value as String
                    if (url.canBeAutoCompleted()) {
                        sendEvent(MainEvent.SuggestURLAutoComplete)
                    }
                    url
                } else {
                    intent.value
                }

                updateStateByIntent(intent.key, value)

                when (intent.key) {
                    Key.ApiKey -> repo.setAPIKey(value as String)
                    Key.ModelName -> repo.setModelName(value as String)
                    Key.SystemPrompt -> repo.setSystemPrompt(value as String)
                    Key.Timeout -> repo.setTimeout(value as Long)
                    Key.Url -> repo.setUrl(value as String)

                    else -> {}
                }

                sendEvent(MainEvent.ConfigSaved)
            }

            is MainIntent.ReloadFromRepository -> {
                updateState { repo.initUiState() }
            }

            MainIntent.CloseDialog -> {
                updateState {
                    copy(
                        shouldShowDialog = false,
                        dialogTitle = "",
                        dialogContent = ""
                    )
                }
            }

            is MainIntent.ShowDialog -> {
                updateState {
                    copy(
                        shouldShowDialog = true,
                        dialogTitle = intent.title,
                        dialogContent = intent.content
                    )
                }
            }

            is MainIntent.NeedAutoComplete -> {
                val newUrl = uiState.value.url.autoComplete()
                updateState { copy(url = newUrl) }

                repo.setUrl(newUrl)
                sendEvent(MainEvent.ConfigSaved)
            }
        }
    }

    override fun updateStateByIntent(key: Key, value: Any) {
        when (key) {
            Key.ApiKey -> updateState {
                copy(apiKey = value as String)
            }

            Key.Url -> updateState {
                copy(url = value as String)
            }

            Key.ModelName -> updateState {
                copy(modelName = value as String)
            }

            Key.SystemPrompt -> updateState {
                copy(systemPrompt = value as String)
            }

            Key.Timeout -> updateState {
                copy(timeout = value as Long)
            }

            else -> {}
        }
    }

    private fun String.canBeAutoCompleted(): Boolean {
        return (endsWith("/v1") || endsWith("/v1/") || endsWith("v1/chat") || endsWith("v1/chat/"))
    }

    private fun String.autoComplete(): String {
        return StringBuilder().apply {
            append(this@autoComplete.trim())
            if (endsWith("/v1") || endsWith("v1/chat")) {
                append("/")
            }
            if (endsWith("/v1/")) {
                append("chat/completions")
            }
            if (endsWith("v1/chat/")) {
                append("completions")
            }
        }.toString()
    }
}