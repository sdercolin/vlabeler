package com.sdercolin.vlabeler.util

/**
 * Get a new entry name to meet the requirement of duplication.
 *
 * @param sourceEntryName The name of the entry as the source for duplication.
 * @param allEntryNames The list of all existing entry names.
 * @param allowDuplicate Whether to allow duplicate entry names.
 */
fun getDefaultNewEntryName(sourceEntryName: String, allEntryNames: List<String>, allowDuplicate: Boolean): String {
    if (allowDuplicate) return sourceEntryName

    val countMatch = Regex("(?<=_)\\d+\$").find(sourceEntryName)
    val body = if (countMatch == null) sourceEntryName + "_" else sourceEntryName.take(countMatch.range.first)
    var count = countMatch?.value?.toInt() ?: 1
    while (true) {
        count++
        val newName = body + count.toString()
        if (allEntryNames.contains(newName).not()) {
            return newName
        }
    }
}
