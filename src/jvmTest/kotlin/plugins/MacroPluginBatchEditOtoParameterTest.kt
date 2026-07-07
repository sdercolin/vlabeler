package plugins

import com.sdercolin.vlabeler.exception.PluginRuntimeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Integration tests for the bundled "batch-edit-oto-parameter" macro plugin.
 *
 * With the utau-singer-labeler, `points` of an entry is `[fixed, preutterance, overlap, offset]` as absolute
 * positions. The "C4" module contains:
 * - "- a": start=10, end=410, points=[110, 90, 40, 10]
 * - "a ka": start=450, end=800, points=[570, 550, 490, 450]
 * - "- i": start=10, end=410, points=[110, 90, 40, 10]
 * - "i ki": start=430, end=800, points=[570, 550, 430, 450]
 */
class MacroPluginBatchEditOtoParameterTest : MacroPluginTestBase() {

    @Test
    fun testShiftOffsetWithoutKeepingDistance() {
        val plugin = loadMacroPlugin("batch-edit-oto-parameter")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "parameter" to "offset",
            "expression" to "\${Offset}+100",
            "keepDistance" to false,
        )

        val result = runMacro(plugin, params, project)

        val entries = result.currentModule.entries
        // only the offset point moves; start is clamped to the minimum of the points and the original start
        assertEquals(listOf(110f, 90f, 40f, 110f), entries[0].points)
        assertEquals(10f, entries[0].start)
        assertEquals(410f, entries[0].end)
        assertEquals(listOf(570f, 550f, 490f, 550f), entries[1].points)
        assertEquals(450f, entries[1].start)
        assertEquals(800f, entries[1].end)
        assertEquals(listOf(110f, 90f, 40f, 110f), entries[2].points)
        assertEquals(listOf(570f, 550f, 430f, 550f), entries[3].points)
        assertEquals(430f, entries[3].start)
    }

    @Test
    fun testShiftOffsetKeepingDistance() {
        val plugin = loadMacroPlugin("batch-edit-oto-parameter")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "parameter" to "offset",
            "expression" to "\${Offset}+100",
            "keepDistance" to true,
        )

        val result = runMacro(plugin, params, project)

        val entries = result.currentModule.entries
        // everything moves by +100
        assertEquals(110f, entries[0].start)
        assertEquals(510f, entries[0].end)
        assertEquals(listOf(210f, 190f, 140f, 110f), entries[0].points)
        assertEquals(550f, entries[1].start)
        assertEquals(900f, entries[1].end)
        assertEquals(listOf(670f, 650f, 590f, 550f), entries[1].points)
        assertEquals(530f, entries[3].start)
        assertEquals(900f, entries[3].end)
        assertEquals(listOf(670f, 650f, 530f, 550f), entries[3].points)
    }

    @Test
    fun testEditPreutteranceRelativeToOffset() {
        val plugin = loadMacroPlugin("batch-edit-oto-parameter")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "parameter" to "preutterance",
            "expression" to "\${Preutterance}*2",
            "keepDistance" to false,
        )

        val result = runMacro(plugin, params, project)

        val entries = result.currentModule.entries
        // preutterance distance is doubled; the point is stored as offset + value
        assertEquals(listOf(110f, 170f, 40f, 10f), entries[0].points) // 80 -> 160
        assertEquals(listOf(570f, 650f, 490f, 450f), entries[1].points) // 100 -> 200
        assertEquals(listOf(110f, 170f, 40f, 10f), entries[2].points)
        assertEquals(listOf(570f, 650f, 430f, 450f), entries[3].points)
    }

    @Test
    fun testUnknownPlaceholderIsRejected() {
        val plugin = loadMacroPlugin("batch-edit-oto-parameter")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "expression" to "\${Unknown}+1",
        )

        assertFailsWith<PluginRuntimeException> {
            runMacro(plugin, params, project)
        }
    }
}
