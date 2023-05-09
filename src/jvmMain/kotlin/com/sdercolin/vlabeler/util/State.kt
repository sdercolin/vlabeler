package com.sdercolin.vlabeler.util

import androidx.compose.runtime.MutableState

fun <T> MutableState<T?>.updateNonNull(updater: T.() -> T) {
    value?.let(updater)?.let { value = it }
}

fun <T> MutableState<T>.update(updater: T.() -> T) {
    value = updater(value)
}

fun <T> MutableState<T?>.clear() {
    value = null
}
