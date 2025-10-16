package com.niki914.core.repository

import com.niki914.core.Key
import com.niki914.core.utils.SharedPreferenceHelper

open class SharedPrefsRepository(val helper: SharedPreferenceHelper) {
    protected inline fun <reified T> get(key: Key): T {
        return helper.get(key)
    }

    protected inline fun <reified T> put(key: Key, value: T) {
        return helper.put(key, value)
    }
}