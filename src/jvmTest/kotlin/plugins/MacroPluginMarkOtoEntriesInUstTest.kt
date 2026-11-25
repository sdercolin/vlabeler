package plugins

import com.sdercolin.vlabeler.model.FileWithEncoding
import testutil.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Integration tests for the bundled "mark-oto-entries-in-ust" macro plugin (project scope).
 *
 * The UST fixture contains two notes: "a ka" at NoteNum 57 (A3) and "- i" at NoteNum 60 (C4). The prefix map fixture
 * maps A3 to the suffix "A3".
 */
class MacroPluginMarkOtoEntriesInUstTest : MacroPluginTestBase() {

    private fun fixture(name: String) = TestFixtures.getDir("plugins/mark-oto-entries-in-ust").resolve(name)

    @Test
    fun testMarkCurrentModuleOnly() {
        val plugin = loadMacroPlugin("mark-oto-entries-in-ust")
        val project = createUtauSingerProject(currentModuleName = "C4")
        val params = plugin.paramsWith(
            "input" to FileWithEncoding(file = fixture("notes.ust").absolutePath),
            "allModules" to false,
            "mark" to "*",
        )

        val result = runMacro(plugin, params, project)

        val c4Tags = result.modules.first { it.name == "C4" }.entries.associate { it.name to it.notes.tag }
        assertEquals(mapOf("- a" to "", "a ka" to "*", "- i" to "*", "i ki" to ""), c4Tags)
        // the A3 module is not the current module, so it is untouched
        val a3Tags = result.modules.first { it.name == "A3" }.entries.map { it.notes.tag }
        assertEquals(listOf("", ""), a3Tags)
    }

    @Test
    fun testMarkAllModulesWithPrefixMap() {
        val plugin = loadMacroPlugin("mark-oto-entries-in-ust")
        val project = createUtauSingerProject(currentModuleName = "C4")
        val params = plugin.paramsWith(
            "input" to FileWithEncoding(file = fixture("notes.ust").absolutePath),
            "prefixMap" to FileWithEncoding(file = fixture("prefix.map").absolutePath),
            "allModules" to true,
            "mark" to "*",
        )

        val result = runMacro(plugin, params, project)

        val c4Tags = result.modules.first { it.name == "C4" }.entries.associate { it.name to it.notes.tag }
        assertEquals(mapOf("- a" to "", "a ka" to "*", "- i" to "*", "i ki" to ""), c4Tags)
        // "a ka" at NoteNum 57 with the prefix map produces the suffixed lyric "a kaA3"
        val a3Tags = result.modules.first { it.name == "A3" }.entries.associate { it.name to it.notes.tag }
        assertEquals(mapOf("- aA3" to "", "a kaA3" to "*"), a3Tags)
    }
}
