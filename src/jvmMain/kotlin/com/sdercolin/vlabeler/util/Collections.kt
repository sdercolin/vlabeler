package com.sdercolin.vlabeler.util

fun <T> List<T>.getPreviousOrNull(predict: (T) -> Boolean): T? {
    val index = indexOfFirst { predict(it) }
    return getOrNull(index - 1)
}

fun <T> List<T>.getNextOrNull(predict: (T) -> Boolean): T? {
    val index = indexOfFirst { predict(it) }
    return getOrNull(index + 1)
}

fun <K, V> Map<K, V?>.getNullableOrElse(key: K, defaultValue: () -> V): V? =
    if (containsKey(key)) get(key) else defaultValue()
