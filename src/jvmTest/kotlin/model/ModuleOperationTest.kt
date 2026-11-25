package model

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.PluginRuntimeException
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.ModuleOperationDescriptor
import com.sdercolin.vlabeler.model.ModuleOperationType
import com.sdercolin.vlabeler.model.Parameter
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.runModuleOperation
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import com.sdercolin.vlabeler.util.toParamMap
import testutil.TestFixtures
import testutil.TestLabelers
import testutil.TestWav
import testutil.createTestProject
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for the module (subproject) management operations defined by [LabelerConf.ModuleManagement], executed with
 * the real scripts of the bundled `nnsvs-singer-labeler`.
 */
class ModuleOperationTest {

    private lateinit var tempDir: File
    private lateinit var sampleDir: File

    @BeforeTest
    fun setup() {
        Log.muted = true
        tempDir = createTempDirectory("vlabeler-test").toFile()
        sampleDir = TestFixtures.deploy(
            "nnsvs-singer",
            tempDir.resolve("nnsvs-singer"),
            wavFiles = listOf("wav/doremi.wav", "wav/legato.wav"),
            wavDurationMs = 1500,
        )
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
        tempDir.deleteRecursively()
    }

    private fun createProject(): Project = createTestProject(
        labeler = TestLabelers.nnsvsSinger,
        sampleDirectory = sampleDir,
    )

    private fun runOperation(project: Project, type: ModuleOperationType, params: Map<String, Any>): Project =
        runModuleOperation(
            descriptor = ModuleOperationDescriptor(project.labelerConf, type),
            params = params.toParamMap(),
            project = project,
            onReport = {},
        )

    @Test
    fun `labeler declares module management operations`() {
        val moduleManagement = TestLabelers.nnsvsSinger.moduleManagement
        assertTrue(moduleManagement.hasAnyOperation)
        assertNotNull(moduleManagement.add)
        assertNotNull(moduleManagement.rename)
        assertNotNull(moduleManagement.remove)
        assertNull(moduleManagement.duplicate)
        val addParameter = moduleManagement.add?.parameters?.single()
        assertTrue(addParameter is Parameter.RawFileParam)
        assertEquals(listOf("wav"), addParameter.acceptExtensions)
        // rename and remove change files on disk, so they are confirmed and irreversible; add is not
        assertNotNull(moduleManagement.rename?.confirmation)
        assertNotNull(moduleManagement.remove?.confirmation)
        assertNull(moduleManagement.add?.confirmation)
        assertEquals(true, moduleManagement.rename?.irreversible)
        assertEquals(true, moduleManagement.remove?.irreversible)
        assertEquals(false, moduleManagement.add?.irreversible)
    }

    @Test
    fun `module management survives serialization round trip`() {
        val labeler = TestLabelers.nnsvsSinger
        val parsed = labeler.stringifyJson().parseJson<LabelerConf>()

        fun LabelerConf.operations() = listOfNotNull(
            moduleManagement.add,
            moduleManagement.rename,
            moduleManagement.remove,
            moduleManagement.duplicate,
        )
        assertEquals(3, parsed.operations().size)
        labeler.operations().zip(parsed.operations()).forEach { (original, roundTripped) ->
            assertEquals(original.displayedName, roundTripped.displayedName)
            assertEquals(original.description, roundTripped.description)
            assertEquals(original.parameters, roundTripped.parameters)
            // the script content is kept, while the path of the script file may be dropped
            assertEquals(
                original.scripts.getScripts(labeler.directory),
                roundTripped.scripts.getScripts(null),
            )
        }
    }

    @Test
    fun `rename changes the module name and renames the files on disk`() {
        val project = createProject()
        assertEquals(listOf("doremi", "legato"), project.modules.map { it.name })

        val result = runOperation(project, ModuleOperationType.Rename, mapOf("newName" to "renamed"))
        // the modules are sorted by name after the change
        assertEquals(listOf("legato", "renamed"), result.modules.map { it.name })
        assertEquals("renamed", result.currentModule.name)

        val renamed = result.modules.first { it.name == "renamed" }
        // the entries follow the renamed wav file, and the raw file path follows the renamed lab file
        assertTrue(renamed.entries.all { it.sample == "renamed.wav" })
        assertTrue(renamed.rawFilePath!!.endsWith("renamed.lab"))
        assertEquals(project.modules[0].entries.map { it.name }, renamed.entries.map { it.name })

        // the files are renamed on disk
        assertTrue(sampleDir.resolve("wav/renamed.wav").exists())
        assertTrue(sampleDir.resolve("lab/renamed.lab").exists())
        assertFalse(sampleDir.resolve("wav/doremi.wav").exists())
        assertFalse(sampleDir.resolve("lab/doremi.lab").exists())

        // the other module is unchanged
        assertEquals(project.modules[1], result.modules[0])
    }

    @Test
    fun `rename to an existing name fails`() {
        val project = createProject()
        assertFailsWith<PluginRuntimeException> {
            runOperation(project, ModuleOperationType.Rename, mapOf("newName" to "legato"))
        }
        assertTrue(sampleDir.resolve("wav/doremi.wav").exists())
    }

    @Test
    fun `rename to an empty name fails`() {
        val project = createProject()
        assertFailsWith<PluginRuntimeException> {
            runOperation(project, ModuleOperationType.Rename, mapOf("newName" to " "))
        }
    }

    @Test
    fun `rename to a name whose files exist on disk fails`() {
        val project = createProject()
        TestWav.write(sampleDir.resolve("wav/stray.wav"), durationMs = 1000)
        assertFailsWith<PluginRuntimeException> {
            runOperation(project, ModuleOperationType.Rename, mapOf("newName" to "stray"))
        }
        assertTrue(sampleDir.resolve("wav/doremi.wav").exists())
    }

    @Test
    fun `remove deletes the current module and its files`() {
        val project = createProject()
        val result = runOperation(project, ModuleOperationType.Remove, mapOf())
        assertEquals(listOf("legato"), result.modules.map { it.name })
        assertEquals(0, result.currentModuleIndex)
        assertFalse(sampleDir.resolve("wav/doremi.wav").exists())
        assertFalse(sampleDir.resolve("lab/doremi.lab").exists())
        assertTrue(sampleDir.resolve("wav/legato.wav").exists())
        assertTrue(sampleDir.resolve("lab/legato.lab").exists())
    }

    @Test
    fun `remove keeps the current module index valid`() {
        val project = createProject().copy(currentModuleIndex = 1)
        val result = runOperation(project, ModuleOperationType.Remove, mapOf())
        assertEquals(listOf("doremi"), result.modules.map { it.name })
        assertEquals(0, result.currentModuleIndex)
        assertFalse(sampleDir.resolve("wav/legato.wav").exists())
    }

    @Test
    fun `remove the only module fails`() {
        val project = createProject()
        val removedOnce = runOperation(project, ModuleOperationType.Remove, mapOf())
        assertFailsWith<PluginRuntimeException> {
            runOperation(removedOnce, ModuleOperationType.Remove, mapOf())
        }
    }

    @Test
    fun `add loads a new wav file with its lab file`() {
        val project = createProject()
        val newWav = sampleDir.resolve("wav/added.wav")
        TestWav.write(newWav, durationMs = 1000)
        sampleDir.resolve("lab/added.lab").writeText(
            """
            0 5000000 pau
            5000000 10000000 a
            """.trimIndent(),
        )

        val result = runOperation(project, ModuleOperationType.Add, mapOf("wavFile" to newWav.absolutePath))
        assertEquals(listOf("added", "doremi", "legato"), result.modules.map { it.name })

        val added = result.modules.first { it.name == "added" }
        assertEquals(listOf("pau", "a"), added.entries.map { it.name })
        assertEquals(0f, added.entries[0].start)
        assertEquals(500f, added.entries[0].end)
        assertEquals(1000f, added.entries[1].end)
        assertTrue(added.entries.all { it.sample == "added.wav" })
        // the current module is switched to the added one
        assertEquals("added", result.currentModule.name)
    }

    @Test
    fun `add creates a default entry when no lab file exists`() {
        val project = createProject()
        val newWav = sampleDir.resolve("wav/nolabel.wav")
        TestWav.write(newWav, durationMs = 1000)

        val result = runOperation(project, ModuleOperationType.Add, mapOf("wavFile" to newWav.absolutePath))
        val added = result.modules.first { it.name == "nolabel" }
        assertEquals(1, added.entries.size)
        // the default entry name of the labeler is used, and the entry is synced with the sample length later
        assertEquals("pau", added.entries.single().name)
        assertTrue(added.entries.single().needSync)
    }

    @Test
    fun `add a wav file outside the wav folder fails`() {
        val project = createProject()
        val outsideWav = sampleDir.resolve("outside.wav")
        TestWav.write(outsideWav, durationMs = 1000)
        assertFailsWith<PluginRuntimeException> {
            runOperation(project, ModuleOperationType.Add, mapOf("wavFile" to outsideWav.absolutePath))
        }
    }

    @Test
    fun `add an already included wav file fails`() {
        val project = createProject()
        val existingWav = sampleDir.resolve("wav/doremi.wav")
        assertFailsWith<PluginRuntimeException> {
            runOperation(project, ModuleOperationType.Add, mapOf("wavFile" to existingWav.absolutePath))
        }
    }
}
