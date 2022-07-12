package com.sdercolin.vlabeler.util

import androidx.compose.foundation.lazy.LazyListState

suspend fun LazyListState.animateScrollToShowItem(selectedIndex: Int) {
    val height = layoutInfo.visibleItemsInfo.first().size
    val first = firstVisibleItemIndex
    val last = layoutInfo.visibleItemsInfo.size + first - 1
    if (selectedIndex >= last) {
        val target = first + (selectedIndex - last)
        animateScrollToItem(target, height)
    } else if (selectedIndex < first) {
        animateScrollToItem(selectedIndex)
    }
}
