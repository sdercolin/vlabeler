package com.sdercolin.vlabeler.util

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * A [MutableState] that saves its value to a persistent storage.
 *
 * @param initial The initial value of the state.
 * @param save The function to save the value to a persistent storage.
 */
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

/**
 * Creates a [SavedMutableState] that saves its value to a persistent storage.
 *
 * @param initial The initial value of the state.
 * @param save The function to save the value to a persistent storage.
 */
fun <T> savedMutableStateOf(initial: T, save: (T) -> Unit) = SavedMutableState(initial, save)
