package com.sdercolin.vlabeler.ui.editor.labeler.marker

import com.sdercolin.vlabeler.model.EntryNotes
import org.junit.jupiter.api.Test
import testutil.TestLabelers
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntryInPixelTest {

    /**
     * The `utau-singer-labeler` has fields [fixed, preu, ovl, left] where "left" (index 3) has `replaceStart == true`,
     * so the labeler uses an implicit start.
     */
    private val utauSinger get() = TestLabelers.utauSinger

    /**
     * The `nnsvs-singer-labeler` is continuous and has no fields, so start/end are explicit.
     */
    private val nnsvsSinger get() = TestLabelers.nnsvsSinger

    private fun entry(
        start: Float,
        end: Float,
        points: List<Float> = emptyList(),
    ) = EntryInPixel(
        index = 0,
        sample = "sample.wav",
        name = "entry",
        start = start,
        end = end,
        points = points,
        extras = emptyList(),
        notes = EntryNotes(),
    )

    @Test
    fun movedShiftsStartEndAndAllPoints() {
        val moved = entry(start = 100f, end = 600f, points = listOf(400f, 250f)).moved(50f)
        assertEquals(150f, moved.start)
        assertEquals(650f, moved.end)
        assertEquals(listOf(450f, 300f), moved.points)
    }

    @Test
    fun movedWithNegativeDeltaShiftsLeft() {
        val moved = entry(start = 100f, end = 600f, points = listOf(400f)).moved(-100f)
        assertEquals(0f, moved.start)
        assertEquals(500f, moved.end)
        assertEquals(listOf(300f), moved.points)
    }

    @Test
    fun validateCoercesAllValuesToCanvasWidth() {
        val validated = entry(start = 100f, end = 1200f, points = listOf(1100f, 500f)).validate(1000f)
        assertEquals(100f, validated.start)
        assertEquals(1000f, validated.end)
        assertEquals(listOf(1000f, 500f), validated.points)
    }

    @Test
    fun collapsedCoercesEntryIntoBorders() {
        // start is pulled up to the left border; points are then coerced into [start, end]
        val collapsed = entry(start = 100f, end = 600f, points = listOf(150f, 400f))
            .collapsed(leftBorder = 200f, rightBorder = 500f)
        assertEquals(200f, collapsed.start)
        assertEquals(500f, collapsed.end)
        assertEquals(listOf(200f, 400f), collapsed.points)
    }

    @Test
    fun collapsedProducesZeroLengthEntryWhenFullyLeftOfBorder() {
        // The whole entry is left of the left border, so it collapses to [border, border]
        val collapsed = entry(start = 100f, end = 200f, points = listOf(150f)).collapsed(leftBorder = 300f)
        assertEquals(300f, collapsed.start)
        assertEquals(300f, collapsed.end)
        assertEquals(listOf(300f), collapsed.points)
    }

    @Test
    fun getPointResolvesStartEndAndFieldIndexes() {
        val target = entry(start = 100f, end = 600f, points = listOf(400f, 250f))
        assertEquals(100f, target.getPoint(MarkerCursorState.START_POINT_INDEX))
        assertEquals(600f, target.getPoint(MarkerCursorState.END_POINT_INDEX))
        assertEquals(400f, target.getPoint(0))
        assertEquals(250f, target.getPoint(1))
    }

    @Test
    fun isValidCutPositionIsExclusiveOnBothEnds() {
        val target = entry(start = 100f, end = 600f)
        assertTrue(target.isValidCutPosition(100.5f))
        assertTrue(target.isValidCutPosition(599.5f))
        assertFalse(target.isValidCutPosition(100f))
        assertFalse(target.isValidCutPosition(600f))
        assertFalse(target.isValidCutPosition(50f))
        assertFalse(target.isValidCutPosition(700f))
    }

    @Test
    fun getActualStartUsesReplaceStartFieldForImplicitStartLabeler() {
        // utau-singer: field 3 ("left") replaces start
        val target = entry(start = 100f, end = 600f, points = listOf(400f, 250f, 150f, 120f))
        assertEquals(120f, target.getActualStart(utauSinger))
    }

    @Test
    fun getActualStartUsesStartForExplicitStartLabeler() {
        val target = entry(start = 100f, end = 600f)
        assertEquals(100f, target.getActualStart(nnsvsSinger))
    }

    @Test
    fun setActualStartWritesReplaceStartFieldForImplicitStartLabeler() {
        val target = entry(start = 100f, end = 600f, points = listOf(400f, 250f, 150f, 120f))
        val updated = target.setActualStart(utauSinger, 200f)
        assertEquals(listOf(400f, 250f, 150f, 200f), updated.points)
        // The raw start is not touched; it is only synced later by validateImplicit
        assertEquals(100f, updated.start)
    }

    @Test
    fun setActualStartWritesStartForExplicitStartLabeler() {
        val updated = entry(start = 100f, end = 600f).setActualStart(nnsvsSinger, 200f)
        assertEquals(200f, updated.start)
    }

    @Test
    fun getAndSetActualEndUseEndWhenNoReplaceEndField() {
        // Neither labeler defines a replaceEnd field
        val target = entry(start = 100f, end = 600f, points = listOf(400f, 250f, 150f, 120f))
        assertEquals(600f, target.getActualEnd(utauSinger))
        assertEquals(650f, target.setActualEnd(utauSinger, 650f).end)
        assertEquals(600f, target.getActualEnd(nnsvsSinger))
        assertEquals(650f, target.setActualEnd(nnsvsSinger, 650f).end)
    }

    @Test
    fun validateImplicitSetsStartToMinimumPointForImplicitStartLabeler() {
        // points min is 120 ("left"), so start becomes 120; end stays because utau-singer has no implicit end
        val target = entry(start = 100f, end = 600f, points = listOf(400f, 250f, 150f, 120f))
        val validated = target.validateImplicit(utauSinger)
        assertEquals(120f, validated.start)
        assertEquals(600f, validated.end)
    }

    @Test
    fun validateImplicitUsesMinimumAcrossAllPoints() {
        // "ovl" (index 2) was dragged below "left" (index 3): min point is 50
        val target = entry(start = 100f, end = 600f, points = listOf(400f, 250f, 50f, 120f))
        assertEquals(50f, target.validateImplicit(utauSinger).start)
    }

    @Test
    fun validateImplicitKeepsEntryUntouchedForExplicitLabeler() {
        val target = entry(start = 100f, end = 600f)
        assertEquals(target, target.validateImplicit(nnsvsSinger))
    }

    @Test
    fun getActualMiddlePointsExcludesReplaceStartField() {
        // Fields 0..2 (fixed, preu, ovl) are middle points; field 3 (left) replaces start and is excluded
        val target = entry(start = 100f, end = 600f, points = listOf(400f, 250f, 150f, 120f))
        assertEquals(listOf(400f, 250f, 150f), target.getActualMiddlePoints(utauSinger))
    }

    @Test
    fun getActualMiddlePointsIsEmptyForLabelerWithoutFields() {
        val target = entry(start = 100f, end = 600f)
        assertEquals(emptyList<Float>(), target.getActualMiddlePoints(nnsvsSinger))
    }
}
