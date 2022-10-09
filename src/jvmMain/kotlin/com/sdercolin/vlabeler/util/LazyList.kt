package com.sdercolin.vlabeler.util

import androidx.compose.foundation.lazy.LazyListState

suspend fun LazyListState.animateScrollToShowItem(selectedIndex: Int) {
    val itemSize = layoutInfo.visibleItemsInfo.firstOrNull()?.size ?: return
    val first = firstVisibleItemIndex
    val last = layoutInfo.visibleItemsInfo.size + first - 1
    if (selectedIndex >= last) {
        val target = first + (selectedIndex - last)
        animateScrollToItem(target, itemSize)
    } else if (selectedIndex < first) {
        animateScrollToItem(selectedIndex)
    }
}
