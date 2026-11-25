package plugins

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests for the bundled "convert-tempo" macro plugin.
 *
 * The plugin scales oto values from a source tempo to a target tempo. With source=60 and target=120 the ratio is
 * exactly 0.5, so all expected values are exact.
 *
 * With the utau-singer-labeler, `points` of an entry is `[fixed, preutterance, overlap, offset]` as absolute
 * positions. See [MacroPluginBatchEditOtoParameterTest] for the fixture values.
 */
class MacroPluginConvertTempoTest : MacroPluginTestBase() {

    @Test
    fun testScaleAllParameters() {
        val plugin = loadMacroPlugin("convert-tempo")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "source" to 60f,
            "target" to 120f,
            "parameters" to "all",
        )

        val result = runMacro(plugin, params, project)

        val entries = result.currentModule.entries
        // "- a": offset 10 -> 5, fixed 100 -> 50, preutter 80 -> 40, overlap 30 -> 15, cutoff -400 -> -200
        assertEquals(5f, entries[0].start)
        assertEquals(205f, entries[0].end)
        assertEquals(listOf(55f, 45f, 20f, 5f), entries[0].points)
        // "a ka": offset 450 -> 225, fixed 120 -> 60, preutter 100 -> 50, overlap 40 -> 20, cutoff -350 -> -175
        assertEquals(225f, entries[1].start)
        assertEquals(400f, entries[1].end)
        assertEquals(listOf(285f, 275f, 245f, 225f), entries[1].points)
        // "- i" equals "- a"
        assertEquals(listOf(55f, 45f, 20f, 5f), entries[2].points)
        // "i ki": overlap -20 -> -10, start scales from 430 to 215
        assertEquals(215f, entries[3].start)
        assertEquals(400f, entries[3].end)
        assertEquals(listOf(285f, 275f, 215f, 225f), entries[3].points)
    }

    @Test
    fun testScaleOffsetOnlyKeepsRelativeValues() {
        val plugin = loadMacroPlugin("convert-tempo")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "source" to 60f,
            "target" to 120f,
            "parameters" to "offset",
        )

        val result = runMacro(plugin, params, project)

        val entries = result.currentModule.entries
        // "- a": offset 10 -> 5; distances (fixed 100, preutter 80, overlap 30, cutoff -400) are unchanged
        assertEquals(5f, entries[0].start)
        assertEquals(405f, entries[0].end)
        assertEquals(listOf(105f, 85f, 35f, 5f), entries[0].points)
        // "a ka": offset 450 -> 225; distances (fixed 120, preutter 100, overlap 40, cutoff -350) are unchanged
        assertEquals(225f, entries[1].start)
        assertEquals(575f, entries[1].end)
        assertEquals(listOf(345f, 325f, 265f, 225f), entries[1].points)
        // "i ki": overlap distance is -20, so the scaled overlap point (225 - 20 = 205) is smaller than the scaled
        // start (215); the project validation (overflowBeforeStart = AdjustBorder) pulls the start down to 205
        assertEquals(205f, entries[3].start)
        assertEquals(575f, entries[3].end)
        assertEquals(listOf(345f, 325f, 205f, 225f), entries[3].points)
    }
}
