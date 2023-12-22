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

    private val _eventFlow = MutableSharedFlow<Event>(replay = 0)
    val eventFlow = _eventFlow.asSharedFlow()

    private var pendingValue: Int = 0
    private var waitingNext = false

    data class Event(val value: Int, val mode: Mode)

    /**
     * Fitting mode
     */
    enum class Mode {
        /**
         * Normal mode, place the entry in the center of the screen. If the entry is too long, place the specified side
         * of the entry on the same side of the screen with some margin.
         */
        NORMAL,

        /**
         * Forward mode, place the entry's start in the center of the screen. Only forward (right) movement is emitted.
         */
        FORWARD,
    }

    private var mode: Mode = Mode.NORMAL

    /**
     * Set the fitting mode before calling [update]. Any [emit] call will reset the mode to [Mode.NORMAL].
     */
    fun setMode(mode: Mode) {
        this.mode = mode
    }

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
        val center = if (end - start <= screenLength - (2 * screenLength / MIN_SCREEN_RATIO_OFFSET)) {
            (start + end) / 2
        } else {
            if (showLeftSide) {
                start + screenLength / 2 - screenLength / MIN_SCREEN_RATIO_OFFSET
            } else {
                end - screenLength / 2 + screenLength / MIN_SCREEN_RATIO_OFFSET
            }
        }
        val target = when (mode) {
            Mode.NORMAL -> center - screenLength / 2
            Mode.FORWARD -> start - screenLength / 2
        }
        update(target.toInt().coerceAtMost(scrollMax).coerceAtLeast(0))
    }

    private fun update(value: Int) {
        pendingValue = value
        if (waitingNext) {
            waitingNext = false
            emit()
        }
    }

    fun emit() {
        val eventMode = mode
        mode = Mode.NORMAL
        coroutineScope.launch {
            _eventFlow.emit(Event(pendingValue, eventMode))
        }
    }

    fun emitNext() {
        waitingNext = true
    }

    companion object {
        private const val MIN_SCREEN_RATIO_OFFSET = 20
    }
}
