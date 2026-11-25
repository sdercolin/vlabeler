package plugins

import com.sdercolin.vlabeler.model.EntrySelector
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests for the bundled "batch-edit-prefix-suffix" macro plugin.
 */
class MacroPluginBatchEditPrefixSuffixTest : MacroPluginTestBase() {

    @Test
    fun testAddSuffixToAllEntries() {
        val plugin = loadMacroPlugin("batch-edit-prefix-suffix")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "position" to "suffix",
            "process" to "add",
            "text" to "_C4",
        )

        val result = runMacro(plugin, params, project)

        assertEquals(
            listOf("- a_C4", "a ka_C4", "- i_C4", "i ki_C4"),
            result.currentModule.entries.map { it.name },
        )
    }

    @Test
    fun testAddPrefixToSelectedEntries() {
        val plugin = loadMacroPlugin("batch-edit-prefix-suffix")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "selector" to selectorOf(nameFilter(EntrySelector.TextMatchType.Equals, "a ka")),
            "position" to "prefix",
            "process" to "add",
            "text" to "C4_",
        )

        val result = runMacro(plugin, params, project)

        assertEquals(
            listOf("- a", "C4_a ka", "- i", "i ki"),
            result.currentModule.entries.map { it.name },
        )
    }

    @Test
    fun testRemovePrefix() {
        val plugin = loadMacroPlugin("batch-edit-prefix-suffix")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "position" to "prefix",
            "process" to "remove",
            "text" to "- ",
        )

        val result = runMacro(plugin, params, project)

        // only names starting with "- " are changed
        assertEquals(
            listOf("a", "a ka", "i", "i ki"),
            result.currentModule.entries.map { it.name },
        )
    }
}
