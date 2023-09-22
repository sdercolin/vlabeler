package com.sdercolin.vlabeler.util

/**
 * Groups a list of items by a selector function, where the items are continuous in the selector. The items are sorted
 * by the selector before grouping.
 */
fun <T : Any> List<T>.groupContinuouslyBy(
    indexSelector: T.() -> Int,
): List<List<T>> {
    val result = mutableListOf<List<T>>()
    var currentGroup = mutableListOf<T>()
    var previousIndex = -1
    sortedBy { it.indexSelector() }.forEach { item ->
        if (item.indexSelector() != previousIndex + 1) {
            if (currentGroup.isNotEmpty()) {
                result.add(currentGroup)
                currentGroup = mutableListOf()
            }
        }
        currentGroup.add(item)
        previousIndex = item.indexSelector()
    }
    if (currentGroup.isNotEmpty()) {
        result.add(currentGroup)
    }
    return result
}
