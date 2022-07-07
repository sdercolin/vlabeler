package com.sdercolin.vlabeler.util

fun <T> List<T>.getPreviousOrNull(item: T): T? {
    val index = indexOf(item).takeIf { it >= 0 } ?: return null
    return getOrNull(index - 1)
}

fun <T> List<T>.getNextOrNull(item: T): T? {
    val index = indexOf(item).takeIf { it >= 0 } ?: return null
    return getOrNull(index + 1)
}
