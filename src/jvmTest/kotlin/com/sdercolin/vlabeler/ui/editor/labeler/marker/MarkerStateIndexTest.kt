package com.sdercolin.vlabeler.ui.editor.labeler.marker

import org.junit.jupiter.api.Test
import testutil.TestLabelers
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the index math and static geometry of [MarkerState].
 *
 * All states are built via [MarkerStateFactory] with sampleRate = 1000 Hz / resolution = 1, so 1 ms == 1 px and the
 * canvas is 1000 px long.
 *
 * Continuous scenario (nnsvs-singer, no fields): 3 entries at 100..200, 200..350, 350..500. Flattened point indexes:
 * -2 (start = 100), 0 (border = 200), 1 (border = 350), -1 (end = 500).
 *
 * Non-continuous scenario (utau-singer, fields [fixed, preu, ovl, left], "left" replaces start): 1 entry with start =
 * 100, end = 600, points = [400, 250, 150, 100]. Flattened point indexes: -2 (actual start = points[3] = 100), 0
 * (fixed = 400), 1 (preu = 250), 2 (ovl = 150), -1 (end = 600).
 */
class MarkerStateIndexTest {

    private fun continuousState() = MarkerStateFactory.create(
        labelerConf = TestLabelers.nnsvsSinger,
        allEntries = listOf(
            MarkerStateFactory.entry(100f, 200f, name = "a"),
            MarkerStateFactory.entry(200f, 350f, name = "b"),
            MarkerStateFactory.entry(350f, 500f, name = "c"),
        ),
    )

    private fun utauSingerState() = MarkerStateFactory.create(
        labelerConf = TestLabelers.utauSinger,
        allEntries = listOf(
            MarkerStateFactory.entry(100f, 600f, points = listOf(400f, 250f, 150f, 100f)),
        ),
    )

    @Test
    fun entryBordersAreTheSharedBordersOfContinuousEntries() {
        assertEquals(listOf(200f, 350f), continuousState().entryBorders)
    }

    @Test
    fun entryBordersAreEmptyForSingleEntry() {
        assertEquals(emptyList<Float>(), utauSingerState().entryBorders)
    }

    @Test
    fun constructionFailsForNonContinuousEntriesInMultiEntryMode() {
        // entry "b" starts at 250 but entry "a" ends at 200, so the entries cannot be drawn together
        assertFailsWith<IllegalArgumentException> {
            MarkerStateFactory.create(
                labelerConf = TestLabelers.nnsvsSinger,
                allEntries = listOf(
                    MarkerStateFactory.entry(100f, 200f, name = "a"),
                    MarkerStateFactory.entry(250f, 350f, name = "b"),
                ),
            )
        }
    }

    @Test
    fun everyNonNegativeIndexIsBorderIndexWhenLabelerHasNoFields() {
        val state = continuousState()
        assertTrue(state.isBorderIndex(0))
        assertTrue(state.isBorderIndex(1))
        assertTrue(state.isBorderIndex(2))
        assertFalse(state.isBorderIndex(-1))
        assertFalse(state.isBorderIndex(-2))
    }

    @Test
    fun borderIndexesAreEveryFifthIndexWhenLabelerHasFourFields() {
        val state = utauSingerState()
        // fields.size + 1 == 5: indexes 0..3 are field points, 4 is a border, 5..8 field points, 9 a border, ...
        assertFalse(state.isBorderIndex(0))
        assertFalse(state.isBorderIndex(1))
        assertFalse(state.isBorderIndex(2))
        assertFalse(state.isBorderIndex(3))
        assertTrue(state.isBorderIndex(4))
        assertFalse(state.isBorderIndex(5))
        assertFalse(state.isBorderIndex(8))
        assertTrue(state.isBorderIndex(9))
        assertFalse(state.isBorderIndex(-1))
    }

    @Test
    fun getEntryIndexesByBorderIndexReturnsAdjacentEntryPair() {
        val state = continuousState()
        assertEquals(0 to 1, state.getEntryIndexesByBorderIndex(0))
        assertEquals(1 to 2, state.getEntryIndexesByBorderIndex(1))
    }

    @Test
    fun getEntryIndexesByBorderIndexRejectsNonBorderIndex() {
        val state = utauSingerState()
        assertFailsWith<IllegalArgumentException> { state.getEntryIndexesByBorderIndex(2) }
    }

    @Test
    fun getPointPositionResolvesStartEndAndMiddlePointsInContinuousMode() {
        val state = continuousState()
        assertEquals(100f, state.getPointPosition(MarkerCursorState.START_POINT_INDEX))
        assertEquals(500f, state.getPointPosition(MarkerCursorState.END_POINT_INDEX))
        assertEquals(200f, state.getPointPosition(0))
        assertEquals(350f, state.getPointPosition(1))
    }

    @Test
    fun getPointPositionUsesImplicitStartAndFieldPoints() {
        val state = utauSingerState()
        // actual start is the "left" point (points[3]); middle points are fields 0..2 in field order
        assertEquals(100f, state.getPointPosition(MarkerCursorState.START_POINT_INDEX))
        assertEquals(600f, state.getPointPosition(MarkerCursorState.END_POINT_INDEX))
        assertEquals(400f, state.getPointPosition(0))
        assertEquals(250f, state.getPointPosition(1))
        assertEquals(150f, state.getPointPosition(2))
    }

    @Test
    fun getPointIndexAsSingleEntryForFirstEntryInContinuousMode() {
        val state = continuousState()
        // entry 0: its start is the global start; its end is border 0
        assertEquals(
            MarkerCursorState.START_POINT_INDEX,
            state.getPointIndexAsSingleEntry(0, MarkerCursorState.START_POINT_INDEX),
        )
        assertEquals(MarkerCursorState.END_POINT_INDEX, state.getPointIndexAsSingleEntry(0, 0))
    }

    @Test
    fun getPointIndexAsSingleEntryForMiddleEntryInContinuousMode() {
        val state = continuousState()
        // entry 1: border 0 is its start, border 1 is its end
        assertEquals(MarkerCursorState.START_POINT_INDEX, state.getPointIndexAsSingleEntry(1, 0))
        assertEquals(MarkerCursorState.END_POINT_INDEX, state.getPointIndexAsSingleEntry(1, 1))
    }

    @Test
    fun getPointIndexAsSingleEntryForLastEntryInContinuousMode() {
        val state = continuousState()
        // entry 2: border 1 is its start, the global end is its end
        assertEquals(MarkerCursorState.START_POINT_INDEX, state.getPointIndexAsSingleEntry(2, 1))
        assertEquals(
            MarkerCursorState.END_POINT_INDEX,
            state.getPointIndexAsSingleEntry(2, MarkerCursorState.END_POINT_INDEX),
        )
    }

    @Test
    fun getPointIndexAsSingleEntryKeepsFieldPointIndexesForSingleEntry() {
        val state = utauSingerState()
        assertEquals(
            MarkerCursorState.START_POINT_INDEX,
            state.getPointIndexAsSingleEntry(0, MarkerCursorState.START_POINT_INDEX),
        )
        assertEquals(
            MarkerCursorState.END_POINT_INDEX,
            state.getPointIndexAsSingleEntry(0, MarkerCursorState.END_POINT_INDEX),
        )
        assertEquals(0, state.getPointIndexAsSingleEntry(0, 0))
        assertEquals(1, state.getPointIndexAsSingleEntry(0, 1))
        assertEquals(2, state.getPointIndexAsSingleEntry(0, 2))
        assertEquals(3, state.getPointIndexAsSingleEntry(0, 3))
    }

    @Test
    fun isValidCutPositionOnlyInsideAnEntry() {
        val state = continuousState()
        assertTrue(state.isValidCutPosition(150f))
        assertTrue(state.isValidCutPosition(250f))
        // exactly on a border or outside all entries is not cuttable
        assertFalse(state.isValidCutPosition(200f))
        assertFalse(state.isValidCutPosition(100f))
        assertFalse(state.isValidCutPosition(500f))
        assertFalse(state.isValidCutPosition(50f))
        assertFalse(state.isValidCutPosition(700f))
    }

    @Test
    fun getEntryIndexByCutPositionReturnsIndexOfContainingEntry() {
        val state = continuousState()
        assertEquals(0, state.getEntryIndexByCutPosition(150f))
        assertEquals(1, state.getEntryIndexByCutPosition(250f))
        assertEquals(2, state.getEntryIndexByCutPosition(499f))
    }

    @Test
    fun isValidPlaybackPositionIsBoundedBySampleLength() {
        val state = continuousState()
        // sample length is 1000 ms == 1000 px
        assertTrue(state.isValidPlaybackPosition(999f))
        assertFalse(state.isValidPlaybackPosition(1000f))
        assertFalse(state.isValidPlaybackPosition(1500f))
    }

    @Test
    fun getClickedAudioRangeReturnsSurroundingBorders() {
        val state = continuousState()
        // borders (sorted, distinct): [0, 100, 200, 350, 500, 1000]
        assertEquals(0f to 100f, state.getClickedAudioRange(50f, leftBorder = 0f, rightBorder = 1000f))
        assertEquals(200f to 350f, state.getClickedAudioRange(250f, leftBorder = 0f, rightBorder = 1000f))
        assertEquals(500f to 1000f, state.getClickedAudioRange(700f, leftBorder = 0f, rightBorder = 1000f))
    }

    @Test
    fun getClickedAudioRangeReturnsOpenRangeOutsideBorders() {
        val state = continuousState()
        assertEquals(null to 0f, state.getClickedAudioRange(-10f, leftBorder = 0f, rightBorder = 1000f))
        assertEquals(1000f to null, state.getClickedAudioRange(1100f, leftBorder = 0f, rightBorder = 1000f))
    }

    @Test
    fun getClickedAudioRangeReturnsNullExactlyOnBorder() {
        val state = continuousState()
        assertNull(state.getClickedAudioRange(200f, leftBorder = 0f, rightBorder = 1000f))
    }
}
