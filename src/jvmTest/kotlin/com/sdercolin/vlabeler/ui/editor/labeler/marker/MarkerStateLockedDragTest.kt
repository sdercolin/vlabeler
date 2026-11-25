package com.sdercolin.vlabeler.ui.editor.labeler.marker

import org.junit.jupiter.api.Test
import testutil.TestLabelers
import kotlin.test.assertEquals
import kotlin.test.assertSame

/**
 * Tests for [MarkerState.getLockedDraggedEntries], which translates all points of all entries together.
 *
 * All states use sampleRate = 1000 Hz / resolution = 1, so 1 ms == 1 px and the canvas is 1000 px long.
 *
 * The utau-singer scenario has 1 entry with start = 100, end = 600, points = [fixed = 400, preu = 250, ovl = 150,
 * left = 100]; its minimum pixel is 100 and maximum pixel is 600, so without forced drag the translation is limited
 * to dx in [-100, 399] (leftBorder = 0, rightBorder = 1000).
 */
class MarkerStateLockedDragTest {

    private val start = MarkerCursorState.START_POINT_INDEX

    private fun utauSingerState() = MarkerStateFactory.create(
        labelerConf = TestLabelers.utauSinger,
        allEntries = listOf(
            MarkerStateFactory.entry(100f, 600f, points = listOf(400f, 250f, 150f, 100f)),
        ),
    )

    private fun continuousState() = MarkerStateFactory.create(
        labelerConf = TestLabelers.nnsvsSinger,
        allEntries = listOf(
            MarkerStateFactory.entry(100f, 200f, name = "a"),
            MarkerStateFactory.entry(200f, 350f, name = "b"),
            MarkerStateFactory.entry(350f, 500f, name = "c"),
        ),
    )

    @Test
    fun nonePointIndexReturnsEntriesUnchanged() {
        val state = utauSingerState()
        assertSame(
            state.entriesInPixel,
            state.getLockedDraggedEntries(MarkerCursorState.NONE_POINT_INDEX, 300f, false),
        )
    }

    @Test
    fun lockedDragOnStartTranslatesWholeEntry() {
        val state = utauSingerState()
        // start point is at 100 (implicit start = "left"); dragging to 300 translates everything by +200
        val result = state.getLockedDraggedEntries(start, 300f, forcedDrag = false)
        assertEquals(
            state.entriesInPixel[0].copy(start = 300f, end = 800f, points = listOf(600f, 450f, 350f, 300f)),
            result[0],
        )
    }

    @Test
    fun lockedDragOnFieldPointTranslatesWholeEntry() {
        val state = utauSingerState()
        // "preu" (index 1) is at 250; dragging to 270 translates everything by +20
        val result = state.getLockedDraggedEntries(1, 270f, forcedDrag = false)
        assertEquals(
            state.entriesInPixel[0].copy(start = 120f, end = 620f, points = listOf(420f, 270f, 170f, 120f)),
            result[0],
        )
    }

    @Test
    fun lockedDragIsClampedByLeftBorder() {
        val state = utauSingerState()
        // requested dx = -300, but the minimum pixel (100) may only move to the left border (0): dx = -100
        val result = state.getLockedDraggedEntries(start, -200f, forcedDrag = false)
        assertEquals(
            state.entriesInPixel[0].copy(start = 0f, end = 500f, points = listOf(300f, 150f, 50f, 0f)),
            result[0],
        )
    }

    @Test
    fun lockedDragIsClampedByRightBorder() {
        val state = utauSingerState()
        // requested dx = +500, but the maximum pixel (600) may only move to rightBorder - 1 (999): dx = +399
        val result = state.getLockedDraggedEntries(start, 600f, forcedDrag = false)
        assertEquals(
            state.entriesInPixel[0].copy(start = 499f, end = 999f, points = listOf(799f, 649f, 549f, 499f)),
            result[0],
        )
    }

    @Test
    fun lockedDragReturnsEntriesUnchangedWhenNoRoomToMove() {
        // the single entry spans the whole canvas, so dxMax (0) <= dxMin (0)
        val state = MarkerStateFactory.create(
            labelerConf = TestLabelers.nnsvsSinger,
            allEntries = listOf(MarkerStateFactory.entry(0f, 1000f)),
        )
        assertSame(state.entriesInPixel, state.getLockedDraggedEntries(start, 500f, forcedDrag = false))
    }

    @Test
    fun lockedDragTranslatesAllEntriesInContinuousMode() {
        val state = continuousState()
        // border 0 is at 200; dragging to 250 translates all three entries by +50
        val result = state.getLockedDraggedEntries(0, 250f, forcedDrag = false)
        assertEquals(
            listOf(
                state.entriesInPixel[0].copy(start = 150f, end = 250f),
                state.entriesInPixel[1].copy(start = 250f, end = 400f),
                state.entriesInPixel[2].copy(start = 400f, end = 550f),
            ),
            result,
        )
    }

    @Test
    fun forcedLockedDragCollapsesEntryAgainstRightBorder() {
        val state = utauSingerState()
        // forced drag allows dx up to rightBorder - 1 - currentX = 899; requested dx = +600 is applied, then the
        // moved entry (700..1200) is collapsed into the canvas: end becomes 1000 while points stay within it
        val result = state.getLockedDraggedEntries(start, 700f, forcedDrag = true)
        assertEquals(
            state.entriesInPixel[0].copy(start = 700f, end = 1000f, points = listOf(1000f, 850f, 750f, 700f)),
            result[0],
        )
    }

    @Test
    fun forcedLockedDragCollapsesEntryAgainstLeftBorder() {
        val state = utauSingerState()
        // in forced mode the range is based on the dragged point itself: dxMin = leftBorder - currentX = -100;
        // the requested dx = -150 is clamped to -100, so the entry moves to 0..500
        val result = state.getLockedDraggedEntries(start, -50f, forcedDrag = true)
        assertEquals(
            state.entriesInPixel[0].copy(start = 0f, end = 500f, points = listOf(300f, 150f, 50f, 0f)),
            result[0],
        )
    }
}
