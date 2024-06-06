package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.runtime.Immutable

@Immutable
data class MarkerCursorState(
    val mouse: Mouse = Mouse.None,
    /**
     * Flattened controllable point index. For multi-entry mode, we have indexes of [-2, 0, 1, 2, 3, ..., -1] for
     * [entries[0].start, entries[0].points[0], entries[0].points[1], entries[0].end, entries[1].points[1], ... ] where
     * the size of points is 2. It's ensured that: entry's end is equal to the next entry's start.
     */
    val pointIndex: Int = NONE_POINT_INDEX,
    val lockedDrag: LockedDrag? = null,
    val previewOnDragging: Boolean = false,
    val forcedDrag: Boolean = false,
    val position: Float? = null,
) {

    fun startDragging(lockedDrag: LockedDrag, withPreview: Boolean, forcedDrag: Boolean) = copy(
        mouse = Mouse.Dragging,
        lockedDrag = lockedDrag,
        previewOnDragging = withPreview,
        forcedDrag = forcedDrag,
    )

    fun finishDragging() = copy(mouse = Mouse.None, lockedDrag = null, previewOnDragging = false, forcedDrag = false)

    fun moveToNothing() = copy(pointIndex = NONE_POINT_INDEX, mouse = Mouse.None)
    fun moveToHover(index: Int) = copy(pointIndex = index, mouse = Mouse.Hovering)

    enum class Mouse {
        Dragging,
        Hovering,
        None
    }

    enum class LockedDrag {
        Forward,
        Backward,
        All,
    }

    val usingStartPoint get() = pointIndex == START_POINT_INDEX
    val usingEndPoint get() = pointIndex == END_POINT_INDEX

    companion object {
        const val NONE_POINT_INDEX = -3
        const val START_POINT_INDEX = -2
        const val END_POINT_INDEX = -1
    }
}
