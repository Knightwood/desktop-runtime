package com.github.knightwood.example.components.expand

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Stable
open class ExpandState {
    private var _expand_state = mutableStateOf(false)

    val isExpanded get() = _expand_state.value

    constructor()
    constructor(initState: Boolean) : this() {
        this._expand_state.value = initState
    }

    open fun show() {
        _expand_state.value = true
    }

    open fun close() = dismiss()

    /**
     * 等同于[close]
     */
    open fun dismiss() {
        _expand_state.value = false
    }

    /**
     * 改变状态
     */
    fun changed(it: Boolean) {
        _expand_state.value = it
    }
}

@Composable
fun rememberExpandState(): ExpandState {
    return remember { ExpandState(false) }
}
