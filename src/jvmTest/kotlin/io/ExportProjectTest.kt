package io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.exportProject
import com.sdercolin.vlabeler.io.exportProjectModule
import com.sdercolin.vlabeler.io.singleModuleToRawLabels
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import testutil.TestEnv
import testutil.TestFixtures
import testutil.TestLabelers
import testutil.createTestProject
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for [ProjectStore.exportProject] and [ProjectStore.exportProjectModule]: the raw label file written by the
 * labeler writer, and the creation of missing output directories.
 */
class ExportProjectTest {

    private lateinit var tempDir: File
    private lateinit var sampleDir: File
    private lateinit var scope: CoroutineScope
    private lateinit var appState: AppState

    @BeforeTest
    fun setup() {
        Log.muted = true
        TestEnv.ensureLogDirectory()
        scope = CoroutineScope(SupervisorJob())
        tempDir = createTempDirectory("vlabeler-test").toFile()
        sampleDir = TestFixtures.deploy(
            "oto",
            tempDir.resolve("oto"),
            wavFiles = listOf("_a_ka.wav"),
        )
        appState = testutil.TestAppState.create(scope = scope)
    }

    @AfterTest
    fun teardown() {
        scope.cancel()
        Log.muted = false
        tempDir.deleteRecursively()
    }

    private fun createProject(): Project = createTestProject(
        labeler = TestLabelers.utauOto,
        sampleDirectory = sampleDir,
        inputFilePath = sampleDir.resolve("oto.ini").absolutePath,
    )

    @Test
    fun testExportProjectModuleWritesRawLabels() {
        val project = createProject()
        val output = tempDir.resolve("out/exported.ini")

        runBlocking { appState.exportProjectModule(project, moduleIndex = 0, outputFile = output) }

        // parent directory is created on demand
        assertTrue(output.parentFile.isDirectory)
        val expected = project.singleModuleToRawLabels(0)
        assertEquals(expected, output.readText())
        assertTrue(output.readText().isNotBlank())
    }

    @Test
    fun testExportProjectWritesToModuleRawFile() {
        val project = createProject()
        val rawFile = requireNotNull(project.modules[0].getRawFile(project))

        runBlocking { appState.exportProject(project) }

        val expected = project.singleModuleToRawLabels(0)
        assertEquals(expected, rawFile.readText())
    }

    @Test
    fun testExportProjectSkipsModulesWithoutRawFile() {
        // a module whose rawFilePath is null must be skipped without error
        val project = createProject()
        val withoutRaw = project.copy(
            modules = project.modules.map { it.copy(rawFilePath = null) },
        )

        // no exception thrown and nothing written
        runBlocking { appState.exportProject(withoutRaw) }
    }
}
