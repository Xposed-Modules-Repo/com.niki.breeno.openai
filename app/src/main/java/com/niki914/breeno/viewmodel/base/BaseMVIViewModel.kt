package com.niki914.breeno.viewmodel.base

import com.niki914.core.Key

/**
 * 加一个 repository
 */
abstract class BaseMVIViewModel<Intent, State, Event, Repo>(repo: Repo) :
    ComposeMVIViewModel<Intent, State, Event, Repo>(repo) {

    /**
     * 应该根据业务范围筛选进行更新的项
     */
    protected open fun updateStateByIntent(key: Key, value: Any) {}
}