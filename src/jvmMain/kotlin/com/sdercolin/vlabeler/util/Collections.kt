package com.sdercolin.vlabeler.util

fun <T> List<T>.getPreviousOrNull(predict: (T) -> Boolean): T? {
    val index = indexOfFirst { predict(it) }
    return getOrNull(index - 1)
}

fun <T> List<T>.getNextOrNull(predict: (T) -> Boolean): T? {
    val index = indexOfFirst { predict(it) }
    return getOrNull(index + 1)
}

fun <T> Iterable<T>.splitAveragely(count: Int): List<List<T>> {
    val length = count()
    val remaining = length % count
    val divided = length / count
    val subSizes = (0 until count).map { divided + if (it < remaining) 1 else 0 }
    var rest = toList()
    val result = mutableListOf<List<T>>()
    repeat(count) { i ->
        val subSize = subSizes[i]
        val subList = rest.take(subSize)
        rest = rest.drop(subSize)
        result += subList
    }
    return result
}

fun <K, V> Map<K, V?>.getNullableOrElse(key: K, defaultValue: () -> V): V? =
    if (containsKey(key)) get(key) else defaultValue()
