package plugins

import com.sdercolin.vlabeler.exception.PluginRuntimeException
import com.sdercolin.vlabeler.model.EntrySelector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Integration tests for the bundled "batch-remove-entry" macro plugin.
 */
class MacroPluginBatchRemoveEntryTest : MacroPluginTestBase() {

    @Test
    fun testRemoveSelectedEntries() {
        val plugin = loadMacroPlugin("batch-remove-entry")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "selector" to selectorOf(nameFilter(EntrySelector.TextMatchType.EndsWith, "i")),
        )

        val result = runMacro(plugin, params, project)

        // "- i" and "i ki" are removed
        assertEquals(listOf("- a", "a ka"), result.currentModule.entries.map { it.name })
        // the other module is untouched
        assertEquals(2, result.modules.first { it.name == "A3" }.entries.size)
    }

    @Test
    fun testCurrentIndexIsClampedWhenRemovingTrailingEntries() {
        val plugin = loadMacroPlugin("batch-remove-entry")
        val project = createUtauSingerProject().updateCurrentModule { copy(currentIndex = 3) }
        val params = plugin.paramsWith(
            "selector" to selectorOf(nameFilter(EntrySelector.TextMatchType.Equals, "i ki")),
        )

        val result = runMacro(plugin, params, project)

        assertEquals(listOf("- a", "a ka", "- i"), result.currentModule.entries.map { it.name })
        assertEquals(2, result.currentModule.currentIndex)
    }

    @Test
    fun testRemovingAllEntriesIsRejected() {
        val plugin = loadMacroPlugin("batch-remove-entry")
        val project = createUtauSingerProject()
        // an empty selector selects all entries, which the plugin rejects with an expected error
        val params = plugin.getDefaultParams()

        assertFailsWith<PluginRuntimeException> {
            runMacro(plugin, params, project)
        }
    }
}
