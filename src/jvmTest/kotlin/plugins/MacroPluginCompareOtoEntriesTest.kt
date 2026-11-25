package plugins

import com.sdercolin.vlabeler.model.EntryNotes
import com.sdercolin.vlabeler.model.FileWithEncoding
import testutil.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/**
 * Integration tests for the bundled "compare-oto-entries" macro plugin.
 *
 * The base oto fixture contains "- a" (also in the project) and "u ku" (not in the project), while the "C4" module of
 * the project also contains "a ka", "- i" and "i ki" which are not in the base file.
 */
class MacroPluginCompareOtoEntriesTest : MacroPluginTestBase() {

    private fun fixture(name: String) = TestFixtures.getDir("plugins/compare-oto-entries").resolve(name)

    @Test
    fun testReportDifferencesWithoutAppending() {
        val plugin = loadMacroPlugin("compare-oto-entries")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "base" to FileWithEncoding(file = fixture("base-oto.ini").absolutePath),
            "append" to false,
        )

        val result = runMacro(plugin, params, project)

        // the project is not changed
        assertEquals(listOf("- a", "a ka", "- i", "i ki"), result.currentModule.entries.map { it.name })
        assertEquals(1, reports.size)
        assertEquals(
            "The following entries are missing in the project:\nu ku\n\n" +
                "The following entries are only in the project:\na ka\n- i\ni ki\n\n",
            reports.single().en,
        )
    }

    @Test
    fun testAppendMissingEntries() {
        val plugin = loadMacroPlugin("compare-oto-entries")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "base" to FileWithEncoding(file = fixture("base-oto.ini").absolutePath),
            "append" to true,
        )

        val result = runMacro(plugin, params, project)

        val entries = result.currentModule.entries
        assertEquals(listOf("- a", "a ka", "- i", "i ki", "u ku"), entries.map { it.name })
        // the appended entry is parsed from "_u_ku.wav=u ku,10.0,100.0,-400.0,80.0,30.0"
        val appended = entries.last()
        assertEquals("_u_ku.wav", appended.sample)
        assertEquals(10f, appended.start)
        assertEquals(410f, appended.end)
        assertEquals(listOf(110f, 90f, 40f, 10f), appended.points)
        assertEquals(listOf<String?>("-400"), appended.extras)
        assertEquals(EntryNotes(), appended.notes)
        assertFalse(appended.needSync)
        assertEquals(
            "The following entries were missing in the project and have been appended:\nu ku\n\n" +
                "The following entries are only in the project:\na ka\n- i\ni ki\n\n",
            reports.single().en,
        )
    }

    @Test
    fun testInSyncReport() {
        val plugin = loadMacroPlugin("compare-oto-entries")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "base" to FileWithEncoding(file = fixture("base-oto-full.ini").absolutePath),
            "append" to false,
        )

        val result = runMacro(plugin, params, project)

        assertEquals(listOf("- a", "a ka", "- i", "i ki"), result.currentModule.entries.map { it.name })
        assertEquals("The project and the input oto file are in sync.", reports.single().en)
    }
}
