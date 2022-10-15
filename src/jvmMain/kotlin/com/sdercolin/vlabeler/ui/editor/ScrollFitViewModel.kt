package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Stable
import com.sdercolin.vlabeler.ui.editor.labeler.marker.EntryInPixel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

@Stable
class ScrollFitViewModel(private val coroutineScope: CoroutineScope) {

    private val _eventFlow = MutableSharedFlow<Int>(replay = 0)
    val eventFlow = _eventFlow.asSharedFlow()

    private var pendingValue: Int = 0
    private var waitingNext = false

    fun update(
        showLeftSide: Boolean,
        horizontalScrollState: ScrollState,
        canvasLength: Float,
        entriesInPixel: List<EntryInPixel>,
        currentIndex: Int,
    ) {
        val scrollMax = horizontalScrollState.maxValue
        val screenLength = canvasLength - scrollMax
        val entry = entriesInPixel.find { it.index == currentIndex } ?: return
        val start = entry.start
        val end = entry.end
        val center = if (end - start <= screenLength - (2 * screenLength / MinScreenRatioOffset)) {
            (start + end) / 2
        } else {
            if (showLeftSide) {
                start + screenLength / 2 - screenLength / MinScreenRatioOffset
            } else {
                end - screenLength / 2 + screenLength / MinScreenRatioOffset
            }
        }
        val target = (center - screenLength / 2).toInt().coerceAtMost(scrollMax).coerceAtLeast(0)
        update(target)
    }

    private fun update(value: Int) {
        pendingValue = value
        if (waitingNext) {
            waitingNext = false
            emit()
        }
    }

    fun emit() {
        coroutineScope.launch {
            _eventFlow.emit(pendingValue)
        }
    }

    fun emitNext() {
        waitingNext = true
    }

    companion object {
        private const val MinScreenRatioOffset = 20
    }
}
