package ui

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.project.ProjectSettingDialogState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import testutil.TestAppState
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
import kotlin.test.assertTrue

/**
 * Tests for [ProjectSettingDialogState], built on a real [AppState] holding a single-module UTAU "oto" project (so the
 * output file is editable and auto-export can be changed).
 */
class ProjectSettingDialogStateTest {

    private lateinit var scope: CoroutineScope
    private lateinit var tempDir: File
    private lateinit var appState: AppState

    @BeforeTest
    fun setup() {
        Log.muted = true
        scope = CoroutineScope(SupervisorJob())
        tempDir = createTempDirectory("vlabeler-test").toFile()
        val voicebank = TestFixtures.deploy(
            "oto",
            tempDir.resolve("voice"),
            wavFiles = listOf("_a_ka.wav"),
        )
        val project = createTestProject(
            labeler = TestLabelers.utauOto,
            sampleDirectory = voicebank,
            inputFilePath = voicebank.resolve("oto.ini").absolutePath,
        )
        appState = TestAppState.create(scope = scope, availableLabelerConfs = listOf(TestLabelers.utauOto))
        appState.openEditor(project)
    }

    @AfterTest
    fun teardown() {
        scope.cancel()
        Log.muted = false
        tempDir.deleteRecursively()
    }

    private fun createState(onFinish: () -> Unit = {}) = ProjectSettingDialogState(appState, onFinish)

    @Test
    fun testInitialValuesFromProject() {
        val project = appState.requireProject()
        val state = createState()

        assertEquals(project.encoding, state.encoding)
        assertEquals(project.rootSampleDirectoryPath, state.rootDirectory)
        assertEquals(project.cacheDirectory.absolutePath, state.cacheDirectory)
        assertEquals(project.autoExport, state.autoExport)
    }

    @Test
    fun testSingleModuleOtoProjectHasEditableOutputFile() {
        val state = createState()

        assertTrue(state.isOutputFileEditable)
        assertEquals(
            appState.requireProject().currentModule.getRawFile(appState.requireProject())?.absolutePath,
            state.outputFile,
        )
        // oto labeler defines a defaultInputFilePath, so auto-export can always be toggled
        assertTrue(state.canChangeAutoExport)
    }

    @Test
    fun testRootDirectoryValidation() {
        val state = createState()

        // the project's sample directory exists, so the initial value is valid
        assertTrue(state.isRootDirectoryValid)

        state.updateRootDirectory(tempDir.absolutePath)
        assertTrue(state.isRootDirectoryValid)

        state.updateRootDirectory(tempDir.resolve("does-not-exist").absolutePath)
        assertFalse(state.isRootDirectoryValid)
    }

    @Test
    fun testCacheDirectoryValidation() {
        val state = createState()

        // default cache directory has an existing parent and is not itself a file
        assertTrue(state.isCacheDirectoryValid)

        state.updateCacheDirectory(tempDir.resolve("new-cache").absolutePath)
        assertTrue(state.isCacheDirectoryValid)

        val existingFile = tempDir.resolve("a-file.txt").apply { writeText("x") }
        state.updateCacheDirectory(existingFile.absolutePath)
        assertFalse(state.isCacheDirectoryValid)
    }

    @Test
    fun testOutputFileValidationAndErrorState() {
        val state = createState()

        assertTrue(state.isOutputFileValid)
        assertFalse(state.isError)

        state.updateOutputFile(tempDir.resolve("output.ini").absolutePath)
        assertTrue(state.isOutputFileValid)
        assertFalse(state.isError)

        state.updateOutputFile(tempDir.resolve("missing-parent").resolve("output.ini").absolutePath)
        assertFalse(state.isOutputFileValid)
        assertTrue(state.isError)
    }

    @Test
    fun testSubmitUpdatesProjectAndFinishes() {
        var finished = false
        val state = createState { finished = true }
        state.encoding = "Shift-JIS"
        state.autoExport = true

        state.submit()

        assertTrue(finished)
        assertEquals("Shift-JIS", appState.requireProject().encoding)
        assertTrue(appState.requireProject().autoExport)
    }

    @Test
    fun testCancelFinishesWithoutChangingProject() {
        val before = appState.requireProject()
        var finished = false
        val state = createState { finished = true }
        state.encoding = "UTF-16"

        state.cancel()

        assertTrue(finished)
        // cancel does not commit the edited encoding
        assertEquals(before, appState.requireProject())
    }
}
