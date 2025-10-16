package com.niki914.breeno.viewmodel

import com.niki914.breeno.repository.ShellCmdSettingsRepository
import com.niki914.breeno.viewmodel.base.BaseMVIViewModel
import com.niki914.core.Key
import com.niki914.core.logV
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

sealed class SCSIntent {
    data class UpdateUIValue(val key: Key, val value: Any) : SCSIntent()
    data class SaveValue(val key: Key, val value: Any) : SCSIntent()
}

data class SCSState(
    val enableRoot: Boolean,
    val isBlackList: Boolean,
    val list: String,
    val askBeforeExec: Boolean
)

sealed class SCSEvent {
    data object ConfigSaved : SCSEvent()
}

@HiltViewModel
class ShellCmdSettingsViewModel @Inject constructor(
    repo: ShellCmdSettingsRepository
) : BaseMVIViewModel<SCSIntent, SCSState, SCSEvent, ShellCmdSettingsRepository>(repo) {

    override fun ShellCmdSettingsRepository.initUiState(): SCSState {
        return SCSState( // 直接用 repo 容易 npr
            getEnableRootAccess(),
            getIsBlackList(),
            getKeywords(),
            getAskBeforeExec()
        )
    }

    override fun handleIntent(intent: SCSIntent) {
        logV("接受 intent: ${intent.javaClass.simpleName}")
        when (intent) {
            is SCSIntent.UpdateUIValue -> {
                updateStateByIntent(intent.key, intent.value)
            }

            is SCSIntent.SaveValue -> {
                updateStateByIntent(intent.key, intent.value)
                val value = intent.value

                when (intent.key) {
                    Key.EnableRootAccessForShellCmd -> repo.setEnableRootAccess(value as Boolean)
                    Key.IsShellUsingBlackList -> repo.setIsBlackList(value as Boolean)
                    Key.ShellCmdList -> repo.setKeywords(value as String)
                    Key.AskBeforeExecuteShell -> repo.setAskBeforeExec(value as Boolean)

                    else -> {}
                }

                sendEvent(SCSEvent.ConfigSaved)
            }
        }
    }

    override fun updateStateByIntent(key: Key, value: Any) {
        when (key) {
            Key.EnableRootAccessForShellCmd -> updateState {
                copy(enableRoot = value as Boolean)
            }

            Key.IsShellUsingBlackList -> updateState {
                copy(isBlackList = value as Boolean)
            }

            Key.ShellCmdList -> updateState {
                copy(list = value as String)
            }

            Key.AskBeforeExecuteShell -> updateState {
                copy(askBeforeExec = value as Boolean)
            }

            else -> {}
        }
    }
}