package com.sdercolin.vlabeler.ui.editor.labeler.marker

import androidx.compose.runtime.Immutable

@Immutable
data class MarkerMouseState(
    val mouse: Mouse = Mouse.None,
    val pointIndex: Int = NonePointIndex, // starts from 0 for custom points
    val lockedDrag: Boolean = false
) {

    fun startDragging(lockedDrag: Boolean) = copy(mouse = Mouse.Dragging, lockedDrag = lockedDrag)
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
