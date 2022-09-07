package com.sdercolin.vlabeler.util

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

class SavedMutableState<T>(initial: T, private val save: (T) -> Unit) : MutableState<T> {

    private val state = mutableStateOf(initial)

    override var value: T
        get() = state.value
        set(value) {
            state.value = value
            save(value)
        }

    override fun component1() = state.component1()
    override fun component2() = state.component2()
}

fun <T> savedMutableStateOf(initial: T, save: (T) -> Unit) = SavedMutableState(initial, save)
