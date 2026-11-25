package plugins

import com.sdercolin.vlabeler.exception.PluginRuntimeException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Integration tests for the bundled "generate-reclist" macro plugin (project scope).
 */
class MacroPluginGenerateReclistTest : MacroPluginTestBase() {

    @Test
    fun testGenerateReclistPerModule() {
        val plugin = loadMacroPlugin("generate-reclist")
        val project = createUtauSingerProject()
        val outputFolder = tempDir.resolve("reclist-output").apply { mkdirs() }
        val params = plugin.paramsWith(
            "outputFolder" to outputFolder.absolutePath,
            "extensions" to "wav",
            "encoding" to "UTF-8",
        )

        val result = runMacro(plugin, params, project)

        // one reclist file per module, containing the sorted sample names without extension
        val a3File = outputFolder.resolve("A3.txt")
        val c4File = outputFolder.resolve("C4.txt")
        assertEquals("_a_ka", a3File.readText())
        assertEquals("_a_ka\n_i_ki", c4File.readText())
        // the project itself is not modified
        assertEquals(
            project.modules.associate { it.name to it.entries },
            result.modules.associate { it.name to it.entries },
        )
        // the report lists the generated files
        val report = reports.single().en
        assertTrue(report.startsWith("Generated reclists:"))
        assertTrue(report.contains(a3File.absolutePath))
        assertTrue(report.contains(c4File.absolutePath))
    }

    @Test
    fun testMissingOutputFolderIsRejected() {
        val plugin = loadMacroPlugin("generate-reclist")
        val project = createUtauSingerProject()
        val params = plugin.paramsWith(
            "outputFolder" to tempDir.resolve("does-not-exist").absolutePath,
        )

        assertFailsWith<PluginRuntimeException> {
            runMacro(plugin, params, project)
        }
    }
}
