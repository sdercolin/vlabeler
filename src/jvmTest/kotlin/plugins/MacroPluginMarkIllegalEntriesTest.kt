package plugins

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests for the bundled "mark-illegal-entries" macro plugin.
 */
class MacroPluginMarkIllegalEntriesTest : MacroPluginTestBase() {

    @Test
    fun testMarkIllegalEntriesInTagAndReport() {
        val plugin = loadMacroPlugin("mark-illegal-entries")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "legalLabels" to "- a,a ka",
            "separator" to ",",
            "markInTag" to true,
            "illegalMark" to "*",
            "showReport" to true,
        )

        val result = runMacro(plugin, params, project)

        val tags = result.currentModule.entries.map { it.notes.tag }
        assertEquals(listOf("", "", "*", "*"), tags)
        assertEquals(1, reports.size)
        assertEquals("Illegal entries:\n- i\ni ki", reports.single().en)
    }

    @Test
    fun testNoIllegalEntries() {
        val plugin = loadMacroPlugin("mark-illegal-entries")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "legalLabels" to "- a\na ka\n- i\ni ki",
            "separator" to "\\n",
            "markInTag" to true,
            "illegalMark" to "*",
            "showReport" to true,
        )

        val result = runMacro(plugin, params, project)

        assertEquals(listOf("", "", "", ""), result.currentModule.entries.map { it.notes.tag })
        assertEquals(1, reports.size)
        assertEquals("No illegal entries found.", reports.single().en)
    }

    @Test
    fun testMarkWithoutTagging() {
        val plugin = loadMacroPlugin("mark-illegal-entries")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "legalLabels" to "- a,a ka",
            "separator" to ",",
            "markInTag" to false,
            "illegalMark" to "*",
            "showReport" to true,
        )

        val result = runMacro(plugin, params, project)

        // tags are untouched, but the report still lists the illegal entries
        assertEquals(listOf("", "", "", ""), result.currentModule.entries.map { it.notes.tag })
        assertEquals("Illegal entries:\n- i\ni ki", reports.single().en)
    }
}
