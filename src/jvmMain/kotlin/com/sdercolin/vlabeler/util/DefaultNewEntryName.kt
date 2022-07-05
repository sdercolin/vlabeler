package com.sdercolin.vlabeler.util

fun getDefaultNewEntryName(sourceEntryName: String, allEntryNames: List<String>, allowDuplicate: Boolean): String {
    if (allowDuplicate) return sourceEntryName

    val countMatch = Regex("\\d+\$").find(sourceEntryName)
    val body = if (countMatch == null) sourceEntryName else sourceEntryName.take(countMatch.range.first)
    var count = countMatch?.value?.toInt() ?: 1
    while (true) {
        count++
        val newName = body + count.toString()
        if (allEntryNames.contains(newName).not()) {
            return newName
        }
    }
}
