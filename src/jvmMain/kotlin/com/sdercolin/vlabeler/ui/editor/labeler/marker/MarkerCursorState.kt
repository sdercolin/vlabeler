package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.runtime.Immutable

@Immutable
data class MarkerCursorState(
    val mouse: Mouse = Mouse.None,
    /**
     * Flattened controllable point index.
     * For multi-entry mode, we have indexes of [-2, 0, 1, 2, 3, ..., -1] for
     * [entries[0].start, entries[0].points[0], entries[0].points[1], entries[0].end, entries[1].points[1], ... ]
     * where the size of points is 2.
     * It's ensured that: entry's end is equal to the next entry's start.
     */
    val pointIndex: Int = NonePointIndex,
    val lockedDrag: Boolean = false,
    val previewOnDragging: Boolean = false,
    val forcedDrag: Boolean = false,
    val position: Float? = null
) {

    fun startDragging(lockedDrag: Boolean, withPreview: Boolean, forcedDrag: Boolean) = copy(
        mouse = Mouse.Dragging,
        lockedDrag = lockedDrag,
        previewOnDragging = withPreview,
        forcedDrag = forcedDrag,
    )

    fun finishDragging() = copy(mouse = Mouse.None, lockedDrag = false)

    fun moveToNothing() = copy(pointIndex = NonePointIndex, mouse = Mouse.None)
    fun moveToHover(index: Int) = copy(pointIndex = index, mouse = Mouse.Hovering)

    enum class Mouse {
        Dragging,
        Hovering,
        None
    }

    val usingStartPoint get() = pointIndex == StartPointIndex
    val usingEndPoint get() = pointIndex == EndPointIndex

    companion object {
        const val NonePointIndex = -3
        const val StartPointIndex = -2
        const val EndPointIndex = -1
    }
}
