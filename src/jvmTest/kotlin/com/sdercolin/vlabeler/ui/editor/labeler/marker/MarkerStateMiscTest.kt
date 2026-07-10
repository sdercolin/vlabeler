package com.sdercolin.vlabeler.ui.editor.labeler.marker

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.ui.editor.Edition
import com.sdercolin.vlabeler.ui.editor.Tool
import org.junit.jupiter.api.Test
import testutil.TestLabelers
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for tool switching, [MarkerState.getUpdatedEntriesByKeyAction] and [MarkerState.computeCascadeEditions].
 *
 * All states use sampleRate = 1000 Hz / resolution = 1 (1 ms == 1 px, canvas 1000 px).
 */
class MarkerStateMiscTest {

    /**
     * Utau-singer single entry: start = 100, end = 600, points = [fixed = 400, preu = 250, ovl = 150, left = 100].
     * Shortcut indexes: 1 -> "ovl" (field 2), 2 -> "preu" (field 1), 3 -> "fixed" (field 0).
     */
    private fun utauSingerState() = MarkerStateFactory.create(
        labelerConf = TestLabelers.utauSinger,
        allEntries = listOf(
            MarkerStateFactory.entry(100f, 600f, points = listOf(400f, 250f, 150f, 100f)),
        ),
    )

    private fun continuousState(currentIndex: Int = 0) = MarkerStateFactory.create(
        labelerConf = TestLabelers.nnsvsSinger,
        allEntries = listOf(
            MarkerStateFactory.entry(100f, 200f, name = "a"),
            MarkerStateFactory.entry(200f, 350f, name = "b"),
        ),
        currentIndex = currentIndex,
    )

    // region tools

    @Test
    fun switchToolCreatesTheToolStateAndClearsOthers() {
        val state = utauSingerState()
        assertTrue(state.isCursor)

        state.switchTool(Tool.Scissors)
        assertNotNull(state.scissorsState.value)
        assertNull(state.panState.value)
        assertNull(state.playbackState.value)
        assertFalse(state.isCursor)

        state.switchTool(Tool.Pan)
        assertNull(state.scissorsState.value)
        assertNotNull(state.panState.value)
        assertFalse(state.isCursor)

        state.switchTool(Tool.Playback)
        assertNull(state.panState.value)
        assertNotNull(state.playbackState.value)
        assertFalse(state.isCursor)

        state.switchTool(Tool.Cursor)
        assertNull(state.scissorsState.value)
        assertNull(state.panState.value)
        assertNull(state.playbackState.value)
        assertTrue(state.isCursor)
    }

    @Test
    fun switchingToAnotherToolResetsCursorState() {
        val state = utauSingerState()
        state.cursorState.value = MarkerCursorState(pointIndex = 1, pointPosition = 250f, position = 250f)
        state.switchTool(Tool.Scissors)
        assertEquals(MarkerCursorState(), state.cursorState.value)
    }

    // endregion

    // region getUpdatedEntriesByKeyAction

    private fun MarkerState.setValueWithCursorAt(action: KeyAction, position: Float) =
        also { cursorState.value = MarkerCursorState(position = position) }
            .getUpdatedEntriesByKeyAction(action, AppConf(), labelerConf)

    @Test
    fun setValue1SetsStartWithCursorPosition() {
        val state = utauSingerState()
        val result = state.setValueWithCursorAt(KeyAction.SetValue1, 200f)
        assertNotNull(result)
        val (entries, pointIndex) = result
        assertEquals(MarkerCursorState.START_POINT_INDEX, pointIndex)
        // same as getDraggedEntries(START, 200): "left" -> 200, raw start re-synced to min point (150)
        assertEquals(
            state.entriesInPixel[0].copy(start = 150f, points = listOf(400f, 250f, 150f, 200f)),
            entries[0],
        )
    }

    @Test
    fun setValue2SetsFieldWithShortcutIndex1() {
        val state = utauSingerState()
        // shortcutIndex 1 is "ovl" (field 2), which is not a drag base, so this is a plain drag;
        // "ovl" has no lower constraint (min is the canvas border), so it goes to 50 and start follows
        val result = state.setValueWithCursorAt(KeyAction.SetValue2, 50f)
        assertNotNull(result)
        val (entries, pointIndex) = result
        assertEquals(2, pointIndex)
        assertEquals(
            state.entriesInPixel[0].copy(start = 50f, points = listOf(400f, 250f, 50f, 100f)),
            entries[0],
        )
    }

    @Test
    fun setValue3UsesLockedDragForDragBaseField() {
        val state = utauSingerState()
        // shortcutIndex 2 is "preu" (field 1) with dragBase == true, and the labeler enables locked drag on the
        // drag base, so the whole entry is translated: preu 250 -> 300 means dx = +50
        val result = state.setValueWithCursorAt(KeyAction.SetValue3, 300f)
        assertNotNull(result)
        val (entries, pointIndex) = result
        assertEquals(1, pointIndex)
        assertEquals(
            state.entriesInPixel[0].copy(start = 150f, end = 650f, points = listOf(450f, 300f, 200f, 150f)),
            entries[0],
        )
    }

    @Test
    fun setValue4SetsFieldWithShortcutIndex3() {
        val state = utauSingerState()
        // shortcutIndex 3 is "fixed" (field 0), not a drag base: plain drag within [250, 600]
        val result = state.setValueWithCursorAt(KeyAction.SetValue4, 500f)
        assertNotNull(result)
        val (entries, pointIndex) = result
        assertEquals(0, pointIndex)
        assertEquals(
            state.entriesInPixel[0].copy(points = listOf(500f, 250f, 150f, 100f)),
            entries[0],
        )
    }

    @Test
    fun setValue5SetsEndWhenAllShortcutsAreUsed() {
        val state = utauSingerState()
        // 3 fields have shortcut indexes, so paramIndex 4 == fieldCount + 1 maps to the end point
        val result = state.setValueWithCursorAt(KeyAction.SetValue5, 700f)
        assertNotNull(result)
        val (entries, pointIndex) = result
        assertEquals(MarkerCursorState.END_POINT_INDEX, pointIndex)
        assertEquals(state.entriesInPixel[0].copy(end = 700f), entries[0])
    }

    @Test
    fun setValueBeyondAvailablePointsReturnsNull() {
        val state = utauSingerState()
        assertNull(state.setValueWithCursorAt(KeyAction.SetValue6, 300f))
    }

    @Test
    fun setValueWithoutCursorPositionReturnsNull() {
        val state = utauSingerState()
        assertNull(state.getUpdatedEntriesByKeyAction(KeyAction.SetValue1, AppConf(), state.labelerConf))
    }

    @Test
    fun nonSetValueActionReturnsNull() {
        val state = utauSingerState()
        assertNull(state.setValueWithCursorAt(KeyAction.NewProject, 150f))
    }

    // endregion

    // region getUpdatedEntriesByKeyAction in multiple entry edit mode

    @Test
    fun setValue1InMultiEntryModeSetsLeftBorderOfEntryUnderCursor() {
        val state = continuousState()
        // cursor 150 is inside entry "a" (100..200), the first entry, so its left border is the group start
        val result = state.setValueWithCursorAt(KeyAction.SetValue1, 150f)
        assertNotNull(result)
        val (entries, pointIndex) = result
        assertEquals(MarkerCursorState.START_POINT_INDEX, pointIndex)
        assertEquals(state.entriesInPixel[0].copy(start = 150f), entries[0])
    }

    @Test
    fun setValue1InMultiEntryModeSetsInnerBorderWhenCursorInSecondEntry() {
        val state = continuousState()
        // cursor 250 is inside entry "b" (200..350); its left border is the inner border between "a" and "b"
        val result = state.setValueWithCursorAt(KeyAction.SetValue1, 250f)
        assertNotNull(result)
        val (entries, pointIndex) = result
        assertEquals(0, pointIndex)
        assertEquals(state.entriesInPixel[0].copy(end = 250f), entries[0])
        assertEquals(state.entriesInPixel[1].copy(start = 250f), entries[1])
    }

    @Test
    fun setValue2InMultiEntryModeSetsRightBorderOfEntryUnderCursor() {
        val state = continuousState()
        // cursor 150 is inside entry "a"; its right border is the inner border between "a" and "b"
        val result = state.setValueWithCursorAt(KeyAction.SetValue2, 150f)
        assertNotNull(result)
        val (entries, pointIndex) = result
        assertEquals(0, pointIndex)
        assertEquals(state.entriesInPixel[0].copy(end = 150f), entries[0])
        assertEquals(state.entriesInPixel[1].copy(start = 150f), entries[1])
    }

    @Test
    fun setValue2InMultiEntryModeSetsGroupEndWhenCursorInLastEntry() {
        val state = continuousState()
        // cursor 300 is inside entry "b" (200..350), the last entry, so its right border is the group end
        val result = state.setValueWithCursorAt(KeyAction.SetValue2, 300f)
        assertNotNull(result)
        val (entries, pointIndex) = result
        assertEquals(MarkerCursorState.END_POINT_INDEX, pointIndex)
        assertEquals(state.entriesInPixel[1].copy(end = 300f), entries[1])
    }

    @Test
    fun setValueInMultiEntryModeWithCursorOutsideAllEntriesReturnsNull() {
        val state = continuousState()
        // 900 is past the last entry's end (350), so no entry is under the cursor
        assertNull(state.setValueWithCursorAt(KeyAction.SetValue1, 900f))
    }

    @Test
    fun setValue3AndBeyondAreNotSupportedInMultiEntryMode() {
        val state = continuousState()
        assertNull(state.setValueWithCursorAt(KeyAction.SetValue3, 250f))
    }

    @Test
    fun setCurrentEntryLeftUsesCurrentEntryRegardlessOfCursor() {
        // current entry is "b" (index 1) while the cursor is inside "a"
        val state = continuousState(currentIndex = 1)
        val result = state.setValueWithCursorAt(KeyAction.SetCurrentEntryLeft, 150f)
        assertNotNull(result)
        val (entries, pointIndex) = result
        // "b"'s left border is the inner border, not the group start that a cursor-based lookup on "a" would give
        assertEquals(0, pointIndex)
        assertEquals(state.entriesInPixel[0].copy(end = 150f), entries[0])
        assertEquals(state.entriesInPixel[1].copy(start = 150f), entries[1])
    }

    @Test
    fun setCurrentEntryRightUsesCurrentEntryRegardlessOfCursor() {
        // current entry is "a" (index 0) while the cursor is inside "b"
        val state = continuousState(currentIndex = 0)
        val result = state.setValueWithCursorAt(KeyAction.SetCurrentEntryRight, 250f)
        assertNotNull(result)
        val (entries, pointIndex) = result
        // "a"'s right border is the inner border, not the group end that a cursor-based lookup on "b" would give
        assertEquals(0, pointIndex)
        assertEquals(state.entriesInPixel[0].copy(end = 250f), entries[0])
        assertEquals(state.entriesInPixel[1].copy(start = 250f), entries[1])
    }

    @Test
    fun setCurrentEntryActionsAreIgnoredInSingleEntryMode() {
        val state = utauSingerState()
        assertNull(state.setValueWithCursorAt(KeyAction.SetCurrentEntryLeft, 200f))
        assertNull(state.setValueWithCursorAt(KeyAction.SetCurrentEntryRight, 200f))
    }

    // endregion

    // region getEntryIndexByCursorPosition

    @Test
    fun getEntryIndexByCursorPositionReturnsEntryContainingPosition() {
        val state = continuousState()
        assertEquals(0, state.getEntryIndexByCursorPosition(150f))
        assertEquals(1, state.getEntryIndexByCursorPosition(300f))
    }

    @Test
    fun getEntryIndexByCursorPositionReturnsFirstMatchOnSharedBorder() {
        val state = continuousState()
        // 200 is both entry "a"'s end and entry "b"'s start; the first containing entry wins
        assertEquals(0, state.getEntryIndexByCursorPosition(200f))
    }

    @Test
    fun getEntryIndexByCursorPositionReturnsNullWhenOutsideAllEntries() {
        val state = continuousState()
        assertNull(state.getEntryIndexByCursorPosition(50f))
        assertNull(state.getEntryIndexByCursorPosition(900f))
    }

    // endregion

    // region computeCascadeEditions

    private fun cascadeState(): Pair<MarkerState, List<Entry>> {
        val parallelEntries = listOf(
            MarkerStateFactory.entry(0f, 150f, name = "p0"),
            MarkerStateFactory.entry(150f, 300f, name = "p1"),
        )
        val state = MarkerStateFactory.create(
            labelerConf = TestLabelers.nnsvsSinger,
            allEntries = listOf(
                MarkerStateFactory.entry(0f, 150f, name = "a"),
                MarkerStateFactory.entry(150f, 300f, name = "b"),
            ),
            parallelModules = listOf(MarkerStateFactory.parallelModule("tier2", parallelEntries)),
        )
        return state to parallelEntries
    }

    @Test
    fun computeCascadeEditionsEditsMatchingBordersOfParallelModules() {
        val (state, parallelEntries) = cascadeState()
        // the border at 150 px matches the border of "tier2" at 150 ms; the new border is 155 px == 155 ms
        val result = state.computeCascadeEditions(snappedPosition = 155f, originalPosition = 150f)
        assertEquals(
            mapOf(
                "tier2" to listOf(
                    Edition(
                        index = 0,
                        newValue = parallelEntries[0].copy(end = 155f),
                        fieldNames = listOf("end"),
                        method = Edition.Method.Dragging,
                    ),
                    Edition(
                        index = 1,
                        newValue = parallelEntries[1].copy(start = 155f),
                        fieldNames = listOf("start"),
                        method = Edition.Method.Dragging,
                    ),
                ),
            ),
            result,
        )
    }

    @Test
    fun computeCascadeEditionsReturnsEmptyWhenPositionUnchanged() {
        val (state, _) = cascadeState()
        val result = state.computeCascadeEditions(snappedPosition = 150f, originalPosition = 150f)
        assertEquals(emptyMap<String, List<Edition>>(), result)
    }

    @Test
    fun computeCascadeEditionsReturnsEmptyWhenNoBorderMatches() {
        val (state, _) = cascadeState()
        // no parallel border is within 1 px of 50
        val result = state.computeCascadeEditions(snappedPosition = 55f, originalPosition = 50f)
        assertEquals(emptyMap<String, List<Edition>>(), result)
    }

    @Test
    fun computeCascadeEditionsDropsModuleWhenNewBorderIsOutOfRange() {
        val (state, _) = cascadeState()
        // the new border (310 ms) is beyond the end (300 ms) of the right entry in "tier2"
        val result = state.computeCascadeEditions(snappedPosition = 310f, originalPosition = 150f)
        assertEquals(emptyMap<String, List<Edition>>(), result)
    }

    // endregion
}
