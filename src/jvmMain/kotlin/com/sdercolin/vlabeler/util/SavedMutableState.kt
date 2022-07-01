package com.sdercolin.vlabeler.util

import androidx.compose.runtime.mutableStateOf
import kotlin.reflect.KProperty

class SavedMutableState<T>(initial: T, private val save: (T) -> Unit) {

    private val state = mutableStateOf(initial)

    operator fun getValue(thisObj: Any?, property: KProperty<*>) = state.value

    operator fun setValue(thisObj: Any?, property: KProperty<*>, value: T) {
        this.state.value = value
        save(value)
    }
}

fun <T> savedMutableStateOf(initial: T, save: (T) -> Unit) = SavedMutableState(initial, save)
