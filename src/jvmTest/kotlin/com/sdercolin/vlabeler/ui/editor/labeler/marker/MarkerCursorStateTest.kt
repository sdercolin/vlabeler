package com.sdercolin.vlabeler.ui.editor.labeler.marker

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarkerCursorStateTest {

    @Test
    fun startDraggingSetsCascadingDragToTrue() {
        val state = MarkerCursorState(
            mouse = MarkerCursorState.Mouse.Hovering,
            pointIndex = 0,
            pointPosition = 100f,
            position = 100f,
        )

        val dragging = state.startDragging(
            lockedDrag = false,
            withPreview = false,
            forcedDrag = false,
            cascadingDrag = true,
        )

        assertEquals(MarkerCursorState.Mouse.Dragging, dragging.mouse)
        assertTrue(dragging.cascadingDrag)
        assertFalse(dragging.lockedDrag)
        assertFalse(dragging.forcedDrag)
        assertFalse(dragging.previewOnDragging)
    }

    @Test
    fun startDraggingSetsCascadingDragToFalse() {
        val state = MarkerCursorState(
            mouse = MarkerCursorState.Mouse.Hovering,
            pointIndex = 0,
            pointPosition = 100f,
            position = 100f,
        )

        val dragging = state.startDragging(
            lockedDrag = false,
            withPreview = false,
            forcedDrag = false,
            cascadingDrag = false,
        )

        assertEquals(MarkerCursorState.Mouse.Dragging, dragging.mouse)
        assertFalse(dragging.cascadingDrag)
    }

    @Test
    fun startDraggingCanCombineCascadingDragWithLockedDrag() {
        val state = MarkerCursorState(
            mouse = MarkerCursorState.Mouse.Hovering,
            pointIndex = 0,
            pointPosition = 100f,
            position = 100f,
        )

        val dragging = state.startDragging(
            lockedDrag = true,
            withPreview = false,
            forcedDrag = false,
            cascadingDrag = true,
        )

        assertTrue(dragging.cascadingDrag)
        assertTrue(dragging.lockedDrag)
    }

    @Test
    fun startDraggingIgnoresCascadingWhenForced() {
        val state = MarkerCursorState(
            mouse = MarkerCursorState.Mouse.Hovering,
            pointIndex = 0,
            pointPosition = 100f,
            position = 100f,
        )

        val dragging = state.startDragging(
            lockedDrag = false,
            withPreview = false,
            forcedDrag = true,
            cascadingDrag = false,
        )

        assertTrue(dragging.forcedDrag)
        assertFalse(dragging.cascadingDrag)
    }

    @Test
    fun finishDraggingResetsCascadingDrag() {
        val state = MarkerCursorState(
            mouse = MarkerCursorState.Mouse.Dragging,
            pointIndex = 0,
            pointPosition = 100f,
            lockedDrag = true,
            cascadingDrag = true,
            forcedDrag = false,
            relativeDraggingIndexOffset = 1,
        )

        val finished = state.finishDragging()

        assertEquals(MarkerCursorState.Mouse.None, finished.mouse)
        assertFalse(finished.cascadingDrag)
        assertFalse(finished.lockedDrag)
        assertEquals(null, finished.relativeDraggingIndexOffset)
    }

    @Test
    fun cascadingDragDefaultsToFalse() {
        val state = MarkerCursorState()
        assertFalse(state.cascadingDrag)
    }

    @Test
    fun startDraggingComputesRelativeDraggingIndexOffsetWhenDraggingRight() {
        val state = MarkerCursorState(
            mouse = MarkerCursorState.Mouse.Hovering,
            pointIndex = 0,
            pointPosition = 100f,
            position = 105f, // position > pointPosition => offset 1
        )

        val dragging = state.startDragging(
            lockedDrag = false,
            withPreview = false,
            forcedDrag = false,
            cascadingDrag = true,
        )

        assertEquals(1, dragging.relativeDraggingIndexOffset)
    }

    @Test
    fun startDraggingComputesRelativeDraggingIndexOffsetWhenDraggingLeft() {
        val state = MarkerCursorState(
            mouse = MarkerCursorState.Mouse.Hovering,
            pointIndex = 0,
            pointPosition = 105f,
            position = 100f, // position < pointPosition => offset 0
        )

        val dragging = state.startDragging(
            lockedDrag = false,
            withPreview = false,
            forcedDrag = false,
            cascadingDrag = true,
        )

        assertEquals(0, dragging.relativeDraggingIndexOffset)
    }
}
