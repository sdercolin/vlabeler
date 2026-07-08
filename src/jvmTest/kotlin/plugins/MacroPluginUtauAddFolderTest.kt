package plugins

import com.sdercolin.vlabeler.exception.PluginRuntimeException
import testutil.TestWav
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val UTAU_ADD_FOLDER_PLUGIN_NAME = "utau-add-folder"

/**
 * Integration tests for the bundled "utau-add-folder" macro plugin (project scope).
 */
class MacroPluginUtauAddFolderTest : MacroPluginTestBase() {

    @Test
    fun testAddFolderAsNewModule() {
        val plugin = loadMacroPlugin(UTAU_ADD_FOLDER_PLUGIN_NAME)
        val project = createUtauSingerProject()
        // create a new pitch folder inside the project root after the project has been created
        val newFolder = project.rootSampleDirectory.resolve("D4")
        TestWav.write(newFolder.resolve("_u_ku.wav"))
        val params = plugin.paramsWith(
            "folder" to newFolder.absolutePath,
        )

        val result = runMacro(plugin, params, project)

        assertEquals(listOf("A3", "C4", "D4"), result.modules.map { it.name }.sorted())
        val module = result.modules.first { it.name == "D4" }
        assertEquals(newFolder, module.getSampleDirectory(result))
        assertEquals(newFolder.resolve("oto.ini"), module.getRawFile(result))
        assertEquals(0, module.currentIndex)
        // one entry per wav file, created from the labeler's default values [100, 400, 300, 200, 100, 500]
        val entry = module.entries.single()
        assertEquals("_u_ku.wav", entry.sample)
        assertEquals("_u_ku", entry.name)
        assertEquals(100f, entry.start)
        assertEquals(500f, entry.end)
        assertEquals(listOf(400f, 300f, 200f, 100f), entry.points)
        assertEquals(listOf<String?>("500"), entry.extras)
        assertTrue(entry.needSync)
        // existing modules are untouched
        assertEquals(4, result.modules.first { it.name == "C4" }.entries.size)
    }

    @Test
    fun testFolderOutsideProjectRootIsRejected() {
        val plugin = loadMacroPlugin(UTAU_ADD_FOLDER_PLUGIN_NAME)
        val project = createUtauSingerProject()
        val outsideFolder = tempDir.resolve("outside")
        TestWav.write(outsideFolder.resolve("_u_ku.wav"))
        val params = plugin.paramsWith(
            "folder" to outsideFolder.absolutePath,
        )

        assertFailsWith<PluginRuntimeException> {
            runMacro(plugin, params, project)
        }
    }

    @Test
    fun testExistingModuleFolderIsRejected() {
        val plugin = loadMacroPlugin(UTAU_ADD_FOLDER_PLUGIN_NAME)
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "folder" to project.rootSampleDirectory.resolve("C4").absolutePath,
        )

        assertFailsWith<PluginRuntimeException> {
            runMacro(plugin, params, project)
        }
    }

    @Test
    fun testFolderWithoutWavFilesIsRejected() {
        val plugin = loadMacroPlugin(UTAU_ADD_FOLDER_PLUGIN_NAME)
        val project = createUtauSingerProject()
        val emptyFolder = project.rootSampleDirectory.resolve("Empty").apply { mkdirs() }
        val params = plugin.paramsWith(
            "folder" to emptyFolder.absolutePath,
        )

        assertFailsWith<PluginRuntimeException> {
            runMacro(plugin, params, project)
        }
    }
}
