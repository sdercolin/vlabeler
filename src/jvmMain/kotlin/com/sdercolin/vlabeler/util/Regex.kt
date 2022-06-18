package com.sdercolin.vlabeler.util

fun String.matchGroups(regex: Regex): List<String> {
    return regex.find(this)?.groupValues.orEmpty().drop(1)
}
