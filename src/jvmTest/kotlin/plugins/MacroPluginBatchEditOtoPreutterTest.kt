package plugins

import com.sdercolin.vlabeler.exception.PluginRuntimeException
import com.sdercolin.vlabeler.model.EntrySelector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Integration tests for the bundled "batch-edit-oto-preutter" macro plugin.
 *
 * The plugin moves the offset so that the preutterance distance becomes the given value (keeping the absolute
 * preutterance position), and optionally re-places the overlap at `offset + preutterance / overlapRatio`.
 *
 * With the utau-singer-labeler, `points` of an entry is `[fixed, preutterance, overlap, offset]` as absolute
 * positions. See [MacroPluginBatchEditOtoParameterTest] for the fixture values.
 */
class MacroPluginBatchEditOtoPreutterTest : MacroPluginTestBase() {

    @Test
    fun testSetAbsolutePreutteranceOnAllEntries() {
        val plugin = loadMacroPlugin("batch-edit-oto-preutter")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "selector" to EntrySelector(filters = emptyList()),
            "preutterance" to "50",
            "overlap" to "",
        )

        val result = runMacro(plugin, params, project)

        val entries = result.currentModule.entries
        // "- a": preutter was 80, so the offset moves from 10 to 40 to make it 50
        assertEquals(listOf(110f, 90f, 40f, 40f), entries[0].points)
        assertEquals(10f, entries[0].start)
        // "a ka": preutter was 100, so the offset moves from 450 to 500
        assertEquals(listOf(570f, 550f, 490f, 500f), entries[1].points)
        assertEquals(450f, entries[1].start)
        assertEquals(listOf(110f, 90f, 40f, 40f), entries[2].points)
        // "i ki": preutter was 100, so the offset moves from 450 to 500
        assertEquals(listOf(570f, 550f, 430f, 500f), entries[3].points)
        assertEquals(430f, entries[3].start)
    }

    @Test
    fun testRelativePreutteranceAndOverlapRatioWithDefaultSelector() {
        val plugin = loadMacroPlugin("batch-edit-oto-preutter")
        val project = createUtauSingerProject()
        // the default selector only selects names matching "[aiueonN].+", i.e. "a ka" and "i ki"
        val params = plugin.paramsWith(
            "preutterance" to "+20",
            "overlap" to "2",
        )

        val result = runMacro(plugin, params, project)

        val entries = result.currentModule.entries
        // "- a" and "- i" are not selected
        assertEquals(listOf(110f, 90f, 40f, 10f), entries[0].points)
        assertEquals(listOf(110f, 90f, 40f, 10f), entries[2].points)
        // "a ka": offset 450 -> 430, new preutter distance = 550 - 430 = 120, overlap = 430 + 120 / 2 = 490
        assertEquals(listOf(570f, 550f, 490f, 430f), entries[1].points)
        assertEquals(430f, entries[1].start)
        // "i ki": offset 450 -> 430, overlap = 430 + 120 / 2 = 490
        assertEquals(listOf(570f, 550f, 490f, 430f), entries[3].points)
        assertEquals(430f, entries[3].start)
    }

    @Test
    fun testOffsetIsClampedAtZeroAndStartFollows() {
        val plugin = loadMacroPlugin("batch-edit-oto-preutter")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "selector" to selectorOf(nameFilter(EntrySelector.TextMatchType.Equals, "- a")),
            "preutterance" to "+20",
            "overlap" to "",
        )

        val result = runMacro(plugin, params, project)

        val entry = result.currentModule.entries[0]
        // offset 10 - 20 would be negative, so it is clamped at 0; start is pulled down to the minimum point
        assertEquals(listOf(110f, 90f, 40f, 0f), entry.points)
        assertEquals(0f, entry.start)
    }

    @Test
    fun testNonNumericInputIsRejected() {
        val plugin = loadMacroPlugin("batch-edit-oto-preutter")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "selector" to EntrySelector(filters = emptyList()),
            "preutterance" to "abc",
            "overlap" to "",
        )

        assertFailsWith<PluginRuntimeException> {
            runMacro(plugin, params, project)
        }
    }
}
