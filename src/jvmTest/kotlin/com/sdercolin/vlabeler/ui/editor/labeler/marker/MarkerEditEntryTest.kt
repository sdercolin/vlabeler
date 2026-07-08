package com.sdercolin.vlabeler.ui.editor.labeler.marker

import com.sdercolin.vlabeler.ui.editor.Edition
import testutil.TestLabelers
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [editEntryIfNeeded], which converts a drag result (a list of updated [EntryInPixel]s) into [Edition]s.
 *
 * All states use sampleRate = 1000 Hz / resolution = 1 unless stated otherwise, so 1 ms == 1 px and the canvas is
 * 1000 px long.
 *
 * Continuous scenario (nnsvs-singer, no fields): entries a = 100..200, b = 200..350, c = 350..500 with flattened
 * point indexes -2 (start), 0 (border at 200), 1 (border at 350), -1 (end).
 *
 * Non-continuous scenario (utau-singer): 1 entry with start = 100, end = 600, points = [fixed = 400, preu = 250,
 * ovl = 150, left = 100].
 */
class MarkerEditEntryTest {

    private val start = MarkerCursorState.START_POINT_INDEX
    private val end = MarkerCursorState.END_POINT_INDEX

    private val entryA = MarkerStateFactory.entry(100f, 200f, name = "a")
    private val entryB = MarkerStateFactory.entry(200f, 350f, name = "b")
    private val entryC = MarkerStateFactory.entry(350f, 500f, name = "c")
    private val utauEntry = MarkerStateFactory.entry(100f, 600f, points = listOf(400f, 250f, 150f, 100f))

    private fun continuousState(editedIndexes: List<Int> = listOf(0, 1, 2)) = MarkerStateFactory.create(
        labelerConf = TestLabelers.nnsvsSinger,
        allEntries = listOf(entryA, entryB, entryC),
        editedIndexes = editedIndexes,
    )

    private fun utauSingerState(resolution: Int = 1) = MarkerStateFactory.create(
        labelerConf = TestLabelers.utauSinger,
        allEntries = listOf(utauEntry),
        resolution = resolution,
    )

    private fun MarkerState.collectEditions(
        updated: List<EntryInPixel>,
        pointIndex: Int,
        method: Edition.Method = Edition.Method.Dragging,
    ): List<List<Edition>> {
        val recorded = mutableListOf<List<Edition>>()
        editEntryIfNeeded(
            updated = updated,
            editEntries = { recorded.add(it) },
            method = method,
            pointIndex = pointIndex,
        )
        return recorded
    }

    // region no-op

    @Test
    fun noEditionWhenUpdatedEqualsEntriesInPixel() {
        val state = continuousState()
        val recorded = state.collectEditions(state.entriesInPixel, start)
        assertEquals(emptyList(), recorded)
    }

    @Test
    fun noEditionWhenDiffSetIsEmpty() {
        val state = continuousState()
        // the reversed list is not equal to entriesInPixel, but no single entry has changed
        val recorded = state.collectEditions(state.entriesInPixel.reversed(), start)
        assertEquals(emptyList(), recorded)
    }

    // endregion

    // region single entry (utau-singer, non-continuous)

    @Test
    fun startEditProducesStartEdition() {
        val state = utauSingerState()
        val updated = listOf(state.entriesInPixel[0].copy(start = 150f))
        val recorded = state.collectEditions(updated, start)
        assertEquals(
            listOf(
                listOf(
                    Edition(0, utauEntry.copy(start = 150f), fieldNames = listOf("start"), Edition.Method.Dragging),
                ),
            ),
            recorded,
        )
    }

    @Test
    fun endEditProducesEndEditionWithMethodPassedThrough() {
        val state = utauSingerState()
        val updated = listOf(state.entriesInPixel[0].copy(end = 550f))
        val recorded = state.collectEditions(updated, end, method = Edition.Method.SetWithCursor)
        assertEquals(
            listOf(
                listOf(
                    Edition(0, utauEntry.copy(end = 550f), fieldNames = listOf("end"), Edition.Method.SetWithCursor),
                ),
            ),
            recorded,
        )
    }

    @Test
    fun fieldPointEditUsesFieldNameFromLabeler() {
        val state = utauSingerState()
        // point index 1 is "preu"
        val updated = listOf(state.entriesInPixel[0].copy(points = listOf(400f, 350f, 150f, 100f)))
        val recorded = state.collectEditions(updated, pointIndex = 1)
        assertEquals(
            listOf(
                listOf(
                    Edition(
                        0,
                        utauEntry.copy(points = listOf(400f, 350f, 150f, 100f)),
                        fieldNames = listOf("preu"),
                        Edition.Method.Dragging,
                    ),
                ),
            ),
            recorded,
        )
    }

    @Test
    fun lastFieldPointEditUsesFieldNameFromLabeler() {
        val state = utauSingerState()
        // point index 3 is "left"
        val updated = listOf(state.entriesInPixel[0].copy(points = listOf(400f, 250f, 150f, 120f)))
        val recorded = state.collectEditions(updated, pointIndex = 3)
        assertEquals(
            listOf(
                listOf(
                    Edition(
                        0,
                        utauEntry.copy(points = listOf(400f, 250f, 150f, 120f)),
                        fieldNames = listOf("left"),
                        Edition.Method.Dragging,
                    ),
                ),
            ),
            recorded,
        )
    }

    @Test
    fun editionValuesAreConvertedToMillisByResolution() {
        // resolution 2: 1 px == 2 ms, so the entry (100..600 ms) is at 50..300 px
        val state = utauSingerState(resolution = 2)
        assertEquals(50f, state.entriesInPixel[0].start)
        assertEquals(300f, state.entriesInPixel[0].end)
        // dragging end to 350 px means 700 ms
        val updated = listOf(state.entriesInPixel[0].copy(end = 350f))
        val recorded = state.collectEditions(updated, end)
        assertEquals(
            listOf(
                listOf(
                    Edition(0, utauEntry.copy(end = 700f), fieldNames = listOf("end"), Edition.Method.Dragging),
                ),
            ),
            recorded,
        )
    }

    // endregion

    // region continuous mode (nnsvs-singer)

    @Test
    fun borderDragProducesEndAndStartEditionsForAdjacentEntries() {
        val state = continuousState()
        // border 0 (between "a" and "b") is dragged from 200 to 250; "c" is unchanged and produces no Edition
        val updated = listOf(
            state.entriesInPixel[0].copy(end = 250f),
            state.entriesInPixel[1].copy(start = 250f),
            state.entriesInPixel[2],
        )
        val recorded = state.collectEditions(updated, pointIndex = 0)
        assertEquals(
            listOf(
                listOf(
                    Edition(0, entryA.copy(end = 250f), fieldNames = listOf("end"), Edition.Method.Dragging),
                    Edition(1, entryB.copy(start = 250f), fieldNames = listOf("start"), Edition.Method.Dragging),
                ),
            ),
            recorded,
        )
    }

    @Test
    fun startDragWithoutLeftNeighborAddsNoCascadeEdition() {
        val state = continuousState()
        // "a" is the first entry of the group, so its moved start has no left neighbor to sync
        val updated = listOf(
            state.entriesInPixel[0].copy(start = 150f),
            state.entriesInPixel[1],
            state.entriesInPixel[2],
        )
        val recorded = state.collectEditions(updated, start)
        assertEquals(
            listOf(
                listOf(
                    Edition(0, entryA.copy(start = 150f), fieldNames = listOf("start"), Edition.Method.Dragging),
                ),
            ),
            recorded,
        )
    }

    @Test
    fun endDragWithoutRightNeighborAddsNoCascadeEdition() {
        val state = continuousState()
        // "c" is the last entry of the group, so its moved end has no right neighbor to sync
        val updated = listOf(
            state.entriesInPixel[0],
            state.entriesInPixel[1],
            state.entriesInPixel[2].copy(end = 550f),
        )
        val recorded = state.collectEditions(updated, end)
        assertEquals(
            listOf(
                listOf(
                    Edition(2, entryC.copy(end = 550f), fieldNames = listOf("end"), Edition.Method.Dragging),
                ),
            ),
            recorded,
        )
    }

    @Test
    fun middleEntryStartDragSyncsLeftNeighborEnd() {
        val state = continuousState(editedIndexes = listOf(1))
        // editing only "b": moving its start adds a leading Edition setting "a".end to the same value
        val updated = listOf(state.entriesInPixel[0].copy(start = 150f))
        val recorded = state.collectEditions(updated, start)
        assertEquals(
            listOf(
                listOf(
                    Edition(0, entryA.copy(end = 150f), fieldNames = listOf("end"), Edition.Method.Dragging),
                    Edition(1, entryB.copy(start = 150f), fieldNames = listOf("start"), Edition.Method.Dragging),
                ),
            ),
            recorded,
        )
    }

    @Test
    fun middleEntryEndDragSyncsRightNeighborStart() {
        val state = continuousState(editedIndexes = listOf(1))
        // editing only "b": moving its end appends an Edition setting "c".start to the same value
        val updated = listOf(state.entriesInPixel[0].copy(end = 400f))
        val recorded = state.collectEditions(updated, end)
        assertEquals(
            listOf(
                listOf(
                    Edition(1, entryB.copy(end = 400f), fieldNames = listOf("end"), Edition.Method.Dragging),
                    Edition(2, entryC.copy(start = 400f), fieldNames = listOf("start"), Edition.Method.Dragging),
                ),
            ),
            recorded,
        )
    }

    @Test
    fun innerBorderDragWithRightNeighborPresentAddsNoCascadeWhenOuterBordersUnchanged() {
        val state = continuousState(editedIndexes = listOf(0, 1))
        // editing "a" and "b" with "c" as right neighbor: dragging the inner border (200 -> 250) keeps the
        // group's outer borders (100 and 350) unchanged, so no cascade Edition for "c" is added
        val updated = listOf(
            state.entriesInPixel[0].copy(end = 250f),
            state.entriesInPixel[1].copy(start = 250f),
        )
        val recorded = state.collectEditions(updated, pointIndex = 0)
        assertEquals(
            listOf(
                listOf(
                    Edition(0, entryA.copy(end = 250f), fieldNames = listOf("end"), Edition.Method.Dragging),
                    Edition(1, entryB.copy(start = 250f), fieldNames = listOf("start"), Edition.Method.Dragging),
                ),
            ),
            recorded,
        )
    }

    // endregion

    // region multi-entry locked drag

    @Test
    fun lockedDragOfAllEntriesProducesOneEditionPerEntry() {
        val state = continuousState()
        // all three entries are moved by +10 px; no neighbor exists outside the group
        val updated = state.entriesInPixel.map { it.moved(10f) }
        val recorded = state.collectEditions(updated, start)
        assertEquals(
            listOf(
                listOf(
                    Edition(
                        0,
                        entryA.copy(start = 110f, end = 210f),
                        fieldNames = listOf("start"),
                        Edition.Method.Dragging,
                    ),
                    // for entries other than the one owning the dragged point, the field name resolution falls
                    // back to "end" (see getPointIndexAsSingleEntry returning -1 == END_POINT_INDEX)
                    Edition(
                        1,
                        entryB.copy(start = 210f, end = 360f),
                        fieldNames = listOf("end"),
                        Edition.Method.Dragging,
                    ),
                    Edition(
                        2,
                        entryC.copy(start = 360f, end = 510f),
                        fieldNames = listOf("end"),
                        Edition.Method.Dragging,
                    ),
                ),
            ),
            recorded,
        )
    }

    @Test
    fun lockedDragOfMiddleEntrySyncsBothNeighbors() {
        val state = continuousState(editedIndexes = listOf(1))
        // "b" is moved by +10 px as a whole: both outer borders moved, so both neighbors are synced
        val updated = listOf(state.entriesInPixel[0].moved(10f))
        val recorded = state.collectEditions(updated, start)
        assertEquals(
            listOf(
                listOf(
                    Edition(0, entryA.copy(end = 210f), fieldNames = listOf("end"), Edition.Method.Dragging),
                    Edition(
                        1,
                        entryB.copy(start = 210f, end = 360f),
                        fieldNames = listOf("start"),
                        Edition.Method.Dragging,
                    ),
                    Edition(2, entryC.copy(start = 360f), fieldNames = listOf("start"), Edition.Method.Dragging),
                ),
            ),
            recorded,
        )
    }

    // endregion
}
