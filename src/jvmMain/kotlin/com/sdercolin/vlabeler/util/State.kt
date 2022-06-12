package com.sdercolin.vlabeler.util

import androidx.compose.runtime.MutableState

fun <T> MutableState<T>.update(updater: T.() -> T) {
    value = updater(value)
}