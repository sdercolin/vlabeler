package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.ScrollState
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams

class ScrollOnResolutionChangeViewModel {
    private var scrollMax: Int? = null
    private var scrollValue: Int = 0
    private var canvasLength: Int? = null
    private var pendingLastCanvasLength: Int? = null

    fun updateCanvasParams(canvasParams: CanvasParams) {
        if (canvasLength == canvasParams.lengthInPixel) return
        pendingLastCanvasLength = canvasLength
        canvasLength = canvasParams.lengthInPixel
    }

    suspend fun scroll(horizontalScrollState: ScrollState) {
        val newValue = getUpdatedValue(horizontalScrollState.maxValue, horizontalScrollState.value) ?: return
        horizontalScrollState.scrollTo(newValue)
    }

    private fun getUpdatedValue(max: Int, value: Int): Int? {
        if (max == Int.MAX_VALUE) {
            scrollMax = null
            return null
        }
        val lastMax = scrollMax
        scrollMax = max
        if (lastMax == null) return null
        if (max == lastMax) {
            scrollValue = value
            return null
        }
        val lastCanvasLength = pendingLastCanvasLength ?: return null
        val canvasLength = canvasLength ?: return null
        pendingLastCanvasLength = null
        val lastValue = scrollValue
        val lastScreenLength = lastCanvasLength - lastMax
        val ratio = (lastValue + lastScreenLength.toFloat() / 2) / lastCanvasLength
        val newScreenLength = canvasLength - max
        val newValue = (ratio * canvasLength - newScreenLength.toFloat() / 2).toInt()
        return newValue.coerceAtLeast(0).coerceAtMost(max).also { scrollValue = it }
    }
}
