package com.sdercolin.vlabeler.ui.editor.labeler

import androidx.compose.foundation.ScrollState
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
            if (scrollState.value to scrollState.maxValue == valuePair && valuePair != 0 to 0) {
                // if valuePair is 0 to 0, it means the data is too short to enable scrolling,
                // so it never changes.
                // in other cases, when it's not changing, it means scrollState has not been updated.
                return getRange(this.canvasLength, scrollState.value, scrollState.maxValue)
            }
            this.canvasLength = canvasLength
        }
        valuePair = scrollState.value to scrollState.maxValue
        return getRange(canvasLength, scrollState.value, scrollState.maxValue)
    }
}
