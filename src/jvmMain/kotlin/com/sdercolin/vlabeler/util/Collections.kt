package com.sdercolin.vlabeler.util

fun <K, V> Iterable<Pair<K, V>>.groupByFirst(): Map<K, List<V>> = groupBy { it.first }
    .map { group -> group.key to group.value.map { it.second } }
    .toMap()
