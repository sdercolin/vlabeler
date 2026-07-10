package io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.ProjectImportException
import com.sdercolin.vlabeler.io.awaitOpenCreatedProject
import com.sdercolin.vlabeler.io.importProjectFile
import com.sdercolin.vlabeler.io.openCreatedProject
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.util.stringifyJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [awaitOpenCreatedProject]/[openCreatedProject] (opening a freshly created project) and [importProjectFile]
 * (importing entries from another project file).
 */
class OpenCreatedProjectTest {

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

    private fun createProject(workingDirectory: File = tempDir.resolve("working")): Project = createTestProject(
        labeler = TestLabelers.utauOto,
        sampleDirectory = sampleDir,
        workingDirectory = workingDirectory,
        inputFilePath = sampleDir.resolve("oto.ini").absolutePath,
    )

    @Test
    fun testAwaitOpenCreatedProjectSavesAndOpens() {
        val project = createProject()

        runBlocking { awaitOpenCreatedProject(project, appState) }

        assertTrue(appState.hasProject)
        assertEquals(project.projectName, appState.project?.projectName)
        // the project file was written to disk
        assertTrue(project.projectFile.isFile)
        // it was registered as a recent project
        assertTrue(appState.appRecordFlow.value.recentProjects.contains(project.projectFile.absolutePath))
    }

    @Test
    fun testOpenCreatedProjectAsync() {
        val project = createProject(workingDirectory = tempDir.resolve("working-async"))

        runBlocking {
            openCreatedProject(this, project, appState)
            withTimeout(15_000) {
                while (!appState.hasProject) delay(5)
            }
        }

        assertEquals(project.projectName, appState.project?.projectName)
        assertTrue(project.projectFile.isFile)
    }

    @Test
    fun testImportProjectFileOpensDialog() {
        val project = createProject()
        val file = tempDir.resolve("to-import.lbp")
        file.writeText(project.stringifyJson())

        runBlocking {
            importProjectFile(this, file, appState)
            withTimeout(15_000) {
                while (appState.importEntriesDialogArgs == null) delay(5)
            }
        }

        val args = assertNotNull(appState.importEntriesDialogArgs)
        assertTrue(args.importedModules.isNotEmpty())
        assertFalse(appState.isBusy)
    }

    @Test
    fun testImportProjectFileShowsErrorOnUnreadableFile() {
        val file = tempDir.resolve("does-not-exist.lbp")

        runBlocking {
            importProjectFile(this, file, appState)
            withTimeout(15_000) {
                while (appState.error == null) delay(5)
            }
        }

        assertIs<ProjectImportException>(appState.error)
        assertFalse(appState.isBusy)
    }

    @Test
    fun testImportProjectFileShowsErrorOnMalformedFile() {
        // a readable but structurally invalid project file surfaces an import error instead of silently opening the
        // import dialog with no modules
        val file = tempDir.resolve("malformed.lbp")
        file.writeText("not a valid project")

        runBlocking {
            importProjectFile(this, file, appState)
            withTimeout(15_000) {
                while (appState.error == null) delay(5)
            }
        }

        assertIs<ProjectImportException>(appState.error)
        assertNull(appState.importEntriesDialogArgs)
        assertFalse(appState.isBusy)
    }
}
