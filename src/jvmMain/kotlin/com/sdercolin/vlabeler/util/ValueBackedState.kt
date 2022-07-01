package com.sdercolin.vlabeler.util

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlin.reflect.KProperty

class ValueBackedState<T, K>(
    private val backState: MutableState<K>,
    valueSelector: (K) -> T,
    private val valueUpdater: (K, T) -> K
) {

    private val state = mutableStateOf(valueSelector(backState.value))

    operator fun getValue(thisObj: Any?, property: KProperty<*>) = state.value

    operator fun setValue(thisObj: Any?, property: KProperty<*>, value: T) {
        this.state.value = value
        backState.value = valueUpdater(backState.value, value)
    }
}

fun <T, K> MutableState<K>.backed(
    valueSelector: (K) -> T,
    valueUpdater: (K, T) -> K
) = ValueBackedState(this, valueSelector, valueUpdater)
