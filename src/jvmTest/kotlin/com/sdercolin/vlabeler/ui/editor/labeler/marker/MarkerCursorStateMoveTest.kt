package com.sdercolin.vlabeler.ui.editor.labeler.marker

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the parts of [MarkerCursorState] that are not covered by [MarkerCursorStateTest].
 */
class MarkerCursorStateMoveTest {

    @Test
    fun moveToHoverSetsPointAndHoveringMouse() {
        val state = MarkerCursorState().moveToHover(index = 2, position = 300f)
        assertEquals(2, state.pointIndex)
        assertEquals(300f, state.pointPosition)
        assertEquals(MarkerCursorState.Mouse.Hovering, state.mouse)
    }

    @Test
    fun moveToNothingClearsPointAndMouse() {
        val state = MarkerCursorState(
            mouse = MarkerCursorState.Mouse.Hovering,
            pointIndex = 2,
            pointPosition = 300f,
            position = 305f,
        ).moveToNothing()
        assertEquals(MarkerCursorState.NONE_POINT_INDEX, state.pointIndex)
        assertNull(state.pointPosition)
        assertEquals(MarkerCursorState.Mouse.None, state.mouse)
        // the raw cursor position is kept
        assertEquals(305f, state.position)
    }

    @Test
    fun usingStartPointAndUsingEndPointReflectPointIndex() {
        assertTrue(MarkerCursorState(pointIndex = MarkerCursorState.START_POINT_INDEX).usingStartPoint)
        assertFalse(MarkerCursorState(pointIndex = MarkerCursorState.START_POINT_INDEX).usingEndPoint)
        assertTrue(MarkerCursorState(pointIndex = MarkerCursorState.END_POINT_INDEX).usingEndPoint)
        assertFalse(MarkerCursorState(pointIndex = MarkerCursorState.END_POINT_INDEX).usingStartPoint)
        assertFalse(MarkerCursorState().usingStartPoint)
        assertFalse(MarkerCursorState().usingEndPoint)
    }

    @Test
    fun startDraggingWithoutPositionsLeavesOffsetNull() {
        val state = MarkerCursorState(pointIndex = 0).startDragging(
            lockedDrag = false,
            withPreview = false,
            forcedDrag = false,
            cascadingDrag = false,
        )
        assertNull(state.relativeDraggingIndexOffset)
    }

    @Test
    fun startDraggingAtExactPointPositionGivesOffsetZero() {
        val state = MarkerCursorState(
            pointIndex = 0,
            pointPosition = 100f,
            position = 100f,
        ).startDragging(
            lockedDrag = false,
            withPreview = false,
            forcedDrag = false,
            cascadingDrag = false,
        )
        assertEquals(0, state.relativeDraggingIndexOffset)
    }
}
