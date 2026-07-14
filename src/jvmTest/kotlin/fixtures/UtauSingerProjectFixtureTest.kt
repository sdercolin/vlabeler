package fixtures

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.PluginRuntimeException
import com.sdercolin.vlabeler.model.ModuleOperationDescriptor
import com.sdercolin.vlabeler.model.ModuleOperationType
import com.sdercolin.vlabeler.model.runModuleOperation
import com.sdercolin.vlabeler.util.ParamMap
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
import kotlin.test.assertTrue

/**
 * Smoke test: creates a project from the `utau-singer` fixture with the bundled `utau-singer-labeler`, going through
 * the real project constructor and oto parser scripts.
 */
class UtauSingerProjectFixtureTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        Log.muted = true
        tempDir = createTempDirectory("vlabeler-test").toFile()
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
        tempDir.deleteRecursively()
    }

    @Test
    fun testCreateProject() {
        val sampleDir = TestFixtures.deploy(
            "utau-singer",
            tempDir.resolve("utau-singer"),
            wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
        )
        val project = createTestProject(
            labeler = TestLabelers.utauSinger,
            sampleDirectory = sampleDir,
        )

        // one module per pitch folder containing samples; the root folder is excluded by default
        assertEquals(listOf("A3", "C4"), project.modules.map { it.name }.sorted())

        val c4 = project.modules.first { it.name == "C4" }
        assertEquals(4, c4.entries.size)

        // _a_ka.wav=- a,10.0,100.0,-400.0,80.0,30.0
        val first = c4.entries.first()
        assertEquals("_a_ka.wav", first.sample)
        assertEquals("- a", first.name)
        assertEquals(10f, first.start)
        assertEquals(410f, first.end) // cutoff -400 means 400ms after offset
        // points: [fixed, preutterance, overlap, offset] as absolute positions
        assertEquals(listOf(110f, 90f, 40f, 10f), first.points)
        assertFalse(first.needSync)

        // _i_ki.wav=i ki,450.0,120.0,-350.0,100.0,-20.0: negative overlap shifts the start
        val negativeOvl = c4.entries.first { it.name == "i ki" }
        assertEquals(430f, negativeOvl.start)
        assertEquals(800f, negativeOvl.end)
        assertEquals(listOf(570f, 550f, 430f, 450f), negativeOvl.points)

        val a3 = project.modules.first { it.name == "A3" }
        assertEquals(listOf("- aA3", "a kaA3"), a3.entries.map { it.name })
    }

    @Test
    fun testAddModuleOperation() {
        val sampleDir = TestFixtures.deploy(
            "utau-singer",
            tempDir.resolve("utau-singer"),
            wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
        )
        val project = createTestProject(
            labeler = TestLabelers.utauSinger,
            sampleDirectory = sampleDir,
        )
        // create a new pitch folder after the project is created
        val newFolder = sampleDir.resolve("B3")
        TestWav.write(newFolder.resolve("_a_ka.wav"))

        val descriptor = ModuleOperationDescriptor(project.labelerConf, ModuleOperationType.Add)
        val result = runModuleOperation(
            descriptor = descriptor,
            params = mapOf<String, Any>("folder" to newFolder.absolutePath).toParamMap(),
            project = project,
            onReport = {},
        )

        assertEquals(listOf("A3", "B3", "C4"), result.modules.map { it.name }.sorted())
        val added = result.modules.first { it.name == "B3" }
        assertEquals("B3", result.currentModule.name)
        assertTrue(added.rawFilePath!!.endsWith("oto.ini"))

        // entries are created from the labeler's default values, to be synced with the sample length later
        val entry = added.entries.single()
        assertEquals("_a_ka.wav", entry.sample)
        assertEquals(100f, entry.start)
        assertEquals(500f, entry.end)
        assertTrue(entry.needSync)

        // adding the same folder again is rejected
        assertFailsWith<PluginRuntimeException> {
            runModuleOperation(
                descriptor = descriptor,
                params = mapOf<String, Any>("folder" to newFolder.absolutePath).toParamMap(),
                project = result,
                onReport = {},
            )
        }
    }

    private fun runOperation(
        project: com.sdercolin.vlabeler.model.Project,
        type: ModuleOperationType,
        params: Map<String, Any>,
    ) = runModuleOperation(
        descriptor = ModuleOperationDescriptor(project.labelerConf, type),
        params = params.toParamMap(),
        project = project,
        onReport = {},
    )

    @Test
    fun testRenameModuleOperation() {
        val sampleDir = TestFixtures.deploy(
            "utau-singer",
            tempDir.resolve("utau-singer"),
            wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
        )
        val project = createTestProject(
            labeler = TestLabelers.utauSinger,
            sampleDirectory = sampleDir,
        )
        assertEquals("A3", project.currentModule.name)

        val result = runOperation(project, ModuleOperationType.Rename, mapOf("newName" to "B3"))
        assertEquals(listOf("B3", "C4"), result.modules.map { it.name })
        assertEquals("B3", result.currentModule.name)

        // the folder is renamed on disk, and the module follows it
        assertTrue(sampleDir.resolve("B3/_a_ka.wav").exists())
        assertTrue(sampleDir.resolve("B3/oto.ini").exists())
        assertFalse(sampleDir.resolve("A3").exists())
        val renamed = result.modules.first { it.name == "B3" }
        assertEquals("B3", renamed.sampleDirectoryPath)
        assertTrue(renamed.rawFilePath!!.replace('\\', '/').endsWith("B3/oto.ini"))
        assertEquals(project.modules.first { it.name == "A3" }.entries, renamed.entries)
    }

    @Test
    fun testRemoveModuleOperation() {
        val sampleDir = TestFixtures.deploy(
            "utau-singer",
            tempDir.resolve("utau-singer"),
            wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
        )
        val project = createTestProject(
            labeler = TestLabelers.utauSinger,
            sampleDirectory = sampleDir,
        )
        assertEquals("A3", project.currentModule.name)

        val result = runOperation(project, ModuleOperationType.Remove, mapOf())
        assertEquals(listOf("C4"), result.modules.map { it.name })
        // the folder is deleted from disk, including all the files inside it
        assertFalse(sampleDir.resolve("A3").exists())
        assertTrue(sampleDir.resolve("C4/_a_ka.wav").exists())
    }

    @Test
    fun testModuleOperationsBlockedForNestedSubprojects() {
        val sampleDir = TestFixtures.deploy(
            "utau-singer",
            tempDir.resolve("utau-singer"),
            wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
        )
        // a nested folder with an oto.ini is constructed as a subproject of its own
        TestWav.write(sampleDir.resolve("C4/sub/_a_ka.wav"))
        sampleDir.resolve("C4/sub/oto.ini").writeText("_a_ka.wav=- a,10.0,100.0,-400.0,80.0,30.0")
        val project = createTestProject(
            labeler = TestLabelers.utauSinger,
            sampleDirectory = sampleDir,
        )
        assertEquals(listOf("A3", "C4", "C4/sub"), project.modules.map { it.name }.sorted())

        // renaming or removing a subproject whose folder contains another subproject is blocked
        val onC4 = project.copy(currentModuleIndex = project.modules.indexOfFirst { it.name == "C4" })
        assertFailsWith<PluginRuntimeException> {
            runOperation(onC4, ModuleOperationType.Rename, mapOf("newName" to "B3"))
        }
        assertFailsWith<PluginRuntimeException> {
            runOperation(onC4, ModuleOperationType.Remove, mapOf())
        }
        assertTrue(sampleDir.resolve("C4/sub/oto.ini").exists())

        // the nested subproject itself can be renamed, moving its folder
        val onSub = project.copy(currentModuleIndex = project.modules.indexOfFirst { it.name == "C4/sub" })
        val result = runOperation(onSub, ModuleOperationType.Rename, mapOf("newName" to "C4/sub2"))
        assertTrue(result.modules.any { it.name == "C4/sub2" })
        assertTrue(sampleDir.resolve("C4/sub2/_a_ka.wav").exists())
        assertFalse(sampleDir.resolve("C4/sub").exists())
    }

    @Test
    fun testModuleOperationsBlockedForRootSubproject() {
        val sampleDir = TestFixtures.deploy(
            "utau-singer",
            tempDir.resolve("utau-singer"),
            wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav", "_root.wav"),
        )
        val labeler = TestLabelers.utauSinger
        val project = createTestProject(
            labeler = labeler,
            sampleDirectory = sampleDir,
            labelerParams = ParamMap(labeler.getDefaultParams() + ("useRootDirectory" to true)),
        )
        val onRoot = project.copy(currentModuleIndex = project.modules.indexOfFirst { it.name == "" })

        assertFailsWith<PluginRuntimeException> {
            runOperation(onRoot, ModuleOperationType.Rename, mapOf("newName" to "renamed"))
        }
        assertFailsWith<PluginRuntimeException> {
            runOperation(onRoot, ModuleOperationType.Remove, mapOf())
        }
        assertTrue(sampleDir.resolve("_root.wav").exists())
    }
}
