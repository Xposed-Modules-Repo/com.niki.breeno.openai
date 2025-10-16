package com.niki914.breeno.viewmodel.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.niki914.core.logE
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * MVI 架构的 ViewModel 基类 - Compose 优化版
 *
 * @param Intent 用户意图/行为
 * @param State UI状态
 * @param Event 一次性效果
 */
abstract class ComposeMVIViewModel<Intent, State, Event, Repo>(
    protected val repo: Repo
) : ViewModel() {
    // UI状态 - 使用StateFlow，完美适配Compose
    private val _uiState = MutableStateFlow(repo.initUiState())
    val uiState: StateFlow<State> = _uiState.asStateFlow()

    // 副作用 - 用于一次性事件（导航、Toast、SnackBar等）
    private val _uiEvent = MutableSharedFlow<Event>(
        extraBufferCapacity = 1 // 防止丢失effect
    )
    val uiEvent: SharedFlow<Event> = _uiEvent.asSharedFlow()

    // Intent处理通道
    private val intentChannel = Channel<Intent>(Channel.UNLIMITED)

    init {
        handleIntents()
    }

    // 处理Intent流
    private fun handleIntents() {
        viewModelScope.launch {
            intentChannel.consumeAsFlow().collect { intent ->
                try {
                    handleIntent(intent)
                } catch (e: Exception) {
                    onError(e)
                }
            }
        }
    }

    /**
     * 发送用户意图
     * Compose中直接调用：viewModel.sendIntent(SomeIntent)
     */
    fun sendIntent(intent: Intent) {
        viewModelScope.launch {
            intentChannel.trySend(intent).getOrThrow()
        }
    }

    /**
     * 更新UI状态
     * 使用copy语法：updateState { copy(loading = true) }
     */
    protected fun updateState(update: State.() -> State) {
        _uiState.update(update)
    }

    /**
     * 发送副作用
     * 用于一次性事件：sendEvent(ShowToast("成功"))
     */
    protected fun sendEvent(effect: Event) {
        viewModelScope.launch {
            _uiEvent.emit(effect)
        }
    }

    /**
     * 获取当前状态
     */
    protected val currentState: State
        get() = _uiState.value

    /**
     * 错误处理 - 子类可重写自定义错误处理
     */
    protected open fun onError(error: Throwable) {
        logE("viewmodel 出现异常", error)
    }

    // 抽象方法
    protected abstract fun Repo.initUiState(): State
    protected abstract fun handleIntent(intent: Intent)
}