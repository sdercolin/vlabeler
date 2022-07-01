package com.sdercolin.vlabeler.util

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlin.reflect.KProperty

class CachedState<T, K>(
    private val backState: MutableState<K>,
    selector: (K) -> T,
    private val updater: (K, T) -> K
) {

    private val state = mutableStateOf(selector(backState.value))

    operator fun getValue(thisObj: Any?, property: KProperty<*>) = state.value

    operator fun setValue(thisObj: Any?, property: KProperty<*>, value: T) {
        this.state.value = value
        backState.value = updater(backState.value, value)
    }
}

fun <T, K> MutableState<K>.cached(
    selector: (K) -> T,
    updater: (K, T) -> K
) = CachedState(this, selector, updater)
