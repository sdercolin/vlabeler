package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.ScrollState
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasParams

class ScrollOnResolutionChangeViewModel {
    private var scrollMax: Int? = null
    private var scrollValue: Int = 0
    private var canvasLength: Float? = null
    private var pendingLastCanvasLength: Float? = null

    private var sampleInfo: SampleInfo? = null
    private var skipped = false

    suspend fun scroll(horizontalScrollState: ScrollState, canvasParams: CanvasParams, sampleInfo: SampleInfo) {
        updateCanvasParams(canvasParams, sampleInfo)
        val newValue = getUpdatedValue(horizontalScrollState.maxValue, horizontalScrollState.value) ?: return
        if (skipped) {
            skipped = false
            return
        }
        horizontalScrollState.scrollTo(newValue)
    }

    private fun updateCanvasParams(canvasParams: CanvasParams, sampleInfo: SampleInfo) {
        if (this.sampleInfo != sampleInfo && this.sampleInfo != null) {
            // skip first scroll when switched sample
            this.sampleInfo = sampleInfo
            skipped = true
            return
        }
        if (canvasLength == canvasParams.lengthInPixel) return
        pendingLastCanvasLength = canvasLength
        canvasLength = canvasParams.lengthInPixel
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
        val ratio = (lastValue + lastScreenLength / 2) / lastCanvasLength
        val newScreenLength = canvasLength - max
        val newValue = (ratio * canvasLength - newScreenLength / 2).toInt()
        return newValue.coerceAtLeast(0).coerceAtMost(max).also { scrollValue = it }
    }
}
