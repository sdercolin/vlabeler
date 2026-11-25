package plugins

import com.sdercolin.vlabeler.model.EntryNotes
import com.sdercolin.vlabeler.model.EntrySelector
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests for the bundled "batch-edit-entry-meta" macro plugin.
 */
class MacroPluginBatchEditEntryMetaTest : MacroPluginTestBase() {

    @Test
    fun testSetStarDoneAndTagOnSelectedEntries() {
        val plugin = loadMacroPlugin("batch-edit-entry-meta")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "selector" to selectorOf(nameFilter(EntrySelector.TextMatchType.Contains, "a")),
            "star" to "on",
            "done" to "on",
            "tag" to true,
            "tagValue" to "vowel",
        )

        val result = runMacro(plugin, params, project)

        val notes = result.currentModule.entries.map { it.notes }
        // "- a" and "a ka" contain "a"; "- i" and "i ki" do not
        assertEquals(EntryNotes(done = true, star = true, tag = "vowel"), notes[0])
        assertEquals(EntryNotes(done = true, star = true, tag = "vowel"), notes[1])
        assertEquals(EntryNotes(), notes[2])
        assertEquals(EntryNotes(), notes[3])
        // names and values are untouched
        assertEquals(listOf("- a", "a ka", "- i", "i ki"), result.currentModule.entries.map { it.name })
    }

    @Test
    fun testUnsetNotesAndRemoveTag() {
        val plugin = loadMacroPlugin("batch-edit-entry-meta")
        val project = createUtauSingerProject().updateCurrentModule {
            copy(entries = entries.map { it.copy(notes = EntryNotes(done = true, star = true, tag = "old")) })
        }
        val params = plugin.paramsWith(
            "star" to "off",
            "done" to "off",
            "tag" to true,
            "tagValue" to "",
        )

        val result = runMacro(plugin, params, project)

        result.currentModule.entries.forEach { entry ->
            assertEquals(EntryNotes(done = false, star = false, tag = ""), entry.notes)
        }
    }

    @Test
    fun testKeepDoesNotChangeNotes() {
        val plugin = loadMacroPlugin("batch-edit-entry-meta")
        val project = createUtauSingerProject().updateCurrentModule {
            copy(entries = entries.map { it.copy(notes = EntryNotes(done = true, star = false, tag = "keep-me")) })
        }
        // all defaults: star = "keep", done = "keep", tag = false
        val params = plugin.getDefaultParams()

        val result = runMacro(plugin, params, project)

        result.currentModule.entries.forEach { entry ->
            assertEquals(EntryNotes(done = true, star = false, tag = "keep-me"), entry.notes)
        }
    }
}
