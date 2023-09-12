package com.sdercolin.vlabeler.ui.editor.labeler

import androidx.compose.foundation.ScrollState
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.util.FloatRange

/**
 * Helper class to calculate the range of the screen.
 */
class ScreenRangeHelper {

    private var valuePair = 0 to 0
    private var canvasLength = 0f

    private fun getRange(canvasLength: Float, value: Int, maxValue: Int): FloatRange? {
        if (maxValue == Int.MAX_VALUE) return null
        val screenLength = canvasLength - maxValue
        if (screenLength <= 0) return null
        val start = value.toFloat()
        val end = value.toFloat() + screenLength
        return start..end
    }

    fun get(canvasLength: Float, scrollState: ScrollState): FloatRange? {
        if (canvasLength != this.canvasLength) {
            if (scrollState.value to scrollState.maxValue == valuePair) {
                // scroll state is not up-to-date, return previous value
                return getRange(this.canvasLength, scrollState.value, scrollState.maxValue)
            }
            this.canvasLength = canvasLength
        }
        valuePair = scrollState.value to scrollState.maxValue
        return getRange(canvasLength, scrollState.value, scrollState.maxValue)
    }
}
