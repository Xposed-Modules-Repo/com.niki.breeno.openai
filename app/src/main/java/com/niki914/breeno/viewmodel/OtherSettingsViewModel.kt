package com.niki914.breeno.viewmodel

import com.niki914.breeno.repository.OtherSettingsRepository
import com.niki914.breeno.viewmodel.base.BaseMVIViewModel
import com.niki914.core.Key
import com.niki914.core.logV
import com.niki914.core.proxyToString
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

sealed class OSIntent {
    data class UpdateUIValue(val key: Key, val value: Any) : OSIntent()
    data class SaveValue(val key: Key, val value: Any) : OSIntent()
}

data class OSState(
    val proxy: String,
    val enableShowToolCalling: Boolean,
    val enableLaunchApp: Boolean,
    val enableLaunchUri: Boolean,
    val enableGetDeviceInfo: Boolean,
    val enableRunShell: Boolean,
    val fallbackToBreeno: String
)

@HiltViewModel
class OtherSettingsViewModel @Inject constructor(
    repo: OtherSettingsRepository
) : BaseMVIViewModel<OSIntent, OSState, Nothing, OtherSettingsRepository>(repo) {

    override fun OtherSettingsRepository.initUiState(): OSState {
        return OSState(
            getProxy().proxyToString(),
            getEnableShowToolCalling(),
            getEnableApp(),
            getEnableUri(),
            getEnableGetDeviceInfo(),
            getEnableShellCmd(),
            getFallbackToBreeno()
        )
    }

    override fun handleIntent(intent: OSIntent) {
        logV("接受 intent: ${intent.javaClass.simpleName}")
        when (intent) {
            is OSIntent.UpdateUIValue -> {
                updateStateByIntent(intent.key, intent.value)
            }

            is OSIntent.SaveValue -> {
                updateStateByIntent(intent.key, intent.value)

                val value = intent.value

                when (intent.key) {
                    Key.Proxy -> repo.setProxy(value as String)
                    Key.EnableShowToolCalling -> repo.setEnableShowToolCalling(value as Boolean)
                    Key.EnableLaunchApp -> repo.setEnableApp(value as Boolean)
                    Key.EnableLaunchURI -> repo.setEnableUri(value as Boolean)
                    Key.EnableGetDeviceInfo -> repo.setEnableGetDeviceInfo(value as Boolean)
                    Key.EnableShellCmd -> repo.setEnableShellCmd(value as Boolean)
                    Key.FallbackToBreeno -> repo.setFallbackToBreeno(value as String)

                    else -> {}
                }
            }
        }
    }

    override fun updateStateByIntent(key: Key, value: Any) {
        when (key) {
            Key.Proxy -> updateState {
                copy(proxy = value as String)
            }

            Key.EnableShowToolCalling -> updateState {
                copy(enableShowToolCalling = value as Boolean)
            }

            Key.EnableLaunchApp -> updateState {
                copy(enableLaunchApp = value as Boolean)
            }

            Key.EnableLaunchURI -> updateState {
                copy(enableLaunchUri = value as Boolean)
            }

            Key.EnableGetDeviceInfo -> updateState {
                copy(enableGetDeviceInfo = value as Boolean)
            }

            Key.EnableShellCmd -> updateState {
                copy(enableRunShell = value as Boolean)
            }

            Key.FallbackToBreeno -> updateState {
                copy(fallbackToBreeno = value as String)
            }

            else -> {}
        }
    }
}