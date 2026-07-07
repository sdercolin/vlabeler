package plugins

import com.sdercolin.vlabeler.model.EntrySelector
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests for the bundled "batch-edit-entry-name" macro plugin.
 */
class MacroPluginBatchEditEntryNameTest : MacroPluginTestBase() {

    @Test
    fun testRenameAllEntriesWithCapturedGroups() {
        val plugin = loadMacroPlugin("batch-edit-entry-name")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "from" to "(.*) (.*)",
            "to" to "$1_$2",
        )

        val result = runMacro(plugin, params, project)

        assertEquals(
            listOf("-_a", "a_ka", "-_i", "i_ki"),
            result.currentModule.entries.map { it.name },
        )
        // entry count and other values are unchanged
        assertEquals(4, result.currentModule.entries.size)
        assertEquals(project.currentModule.entries.map { it.points }, result.currentModule.entries.map { it.points })
    }

    @Test
    fun testRenameSelectedEntriesOnly() {
        val plugin = loadMacroPlugin("batch-edit-entry-name")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "selector" to selectorOf(nameFilter(EntrySelector.TextMatchType.StartsWith, "- ")),
            "from" to "^- ",
            "to" to "* ",
        )

        val result = runMacro(plugin, params, project)

        assertEquals(
            listOf("* a", "a ka", "* i", "i ki"),
            result.currentModule.entries.map { it.name },
        )
    }
}
