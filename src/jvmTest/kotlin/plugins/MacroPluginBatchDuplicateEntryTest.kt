package plugins

import com.sdercolin.vlabeler.model.EntrySelector
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests for the bundled "batch-duplicate-entry" macro plugin.
 */
class MacroPluginBatchDuplicateEntryTest : MacroPluginTestBase() {

    @Test
    fun testDuplicateAllEntriesWithRenaming() {
        val plugin = loadMacroPlugin("batch-duplicate-entry")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "from" to "(.*)",
            "to" to "$1_copy",
        )

        val result = runMacro(plugin, params, project)

        val entries = result.currentModule.entries
        // each entry is duplicated in place, with the copy inserted right after the original
        assertEquals(
            listOf("- a", "- a_copy", "a ka", "a ka_copy", "- i", "- i_copy", "i ki", "i ki_copy"),
            entries.map { it.name },
        )
        // the duplicate keeps all values of the original except the name
        val original = entries[0]
        val duplicate = entries[1]
        assertEquals(original.sample, duplicate.sample)
        assertEquals(original.start, duplicate.start)
        assertEquals(original.end, duplicate.end)
        assertEquals(original.points, duplicate.points)
        assertEquals(original.extras, duplicate.extras)
        // the other module is untouched
        assertEquals(
            listOf("- aA3", "a kaA3"),
            result.modules.first { it.name == "A3" }.entries.map { it.name },
        )
    }

    @Test
    fun testDuplicateSelectedEntryOnly() {
        val plugin = loadMacroPlugin("batch-duplicate-entry")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "selector" to selectorOf(nameFilter(EntrySelector.TextMatchType.Equals, "- a")),
            "from" to "(.*)",
            "to" to "$1 2",
        )

        val result = runMacro(plugin, params, project)

        assertEquals(
            listOf("- a", "- a 2", "a ka", "- i", "i ki"),
            result.currentModule.entries.map { it.name },
        )
    }
}
