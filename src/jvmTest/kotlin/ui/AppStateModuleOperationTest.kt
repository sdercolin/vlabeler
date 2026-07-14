package ui

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.ModuleOperationDescriptor
import com.sdercolin.vlabeler.model.ModuleOperationType
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.util.toParamMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import testutil.TestAppState
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [AppState.executeModuleOperation], especially the undo history handling around operations declared as
 * irreversible (i.e. operations changing files on disk).
 */
class AppStateModuleOperationTest {

    private lateinit var scope: CoroutineScope
    private lateinit var appState: AppState
    private lateinit var tempDir: File
    private lateinit var sampleDir: File

    @BeforeTest
    fun setup() {
        Log.muted = true
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        appState = TestAppState.create(scope)
        tempDir = createTempDirectory("vlabeler-test").toFile()
        sampleDir = TestFixtures.deploy(
            "nnsvs-singer",
            tempDir.resolve("nnsvs-singer"),
            wavFiles = listOf("wav/doremi.wav", "wav/legato.wav"),
            wavDurationMs = 1500,
        )
        appState.newProject(
            createTestProject(
                labeler = TestLabelers.nnsvsSinger,
                sampleDirectory = sampleDir,
            ),
        )
    }

    @AfterTest
    fun teardown() {
        scope.cancel()
        tempDir.deleteRecursively()
        Log.muted = false
    }

    @Test
    fun `an irreversible operation clears the undo history`() {
        appState.editCurrentEntryTag("edited")
        assertTrue(appState.canUndo)

        val descriptor = ModuleOperationDescriptor(appState.requireProject().labelerConf, ModuleOperationType.Remove)
        appState.executeModuleOperation(descriptor, mapOf<String, Any>().toParamMap())

        assertEquals(listOf("legato"), appState.requireProject().modules.map { it.name })
        // the operation deleted files on disk, so the undo history is cleared
        assertFalse(appState.canUndo)
        assertFalse(appState.canRedo)
    }

    @Test
    fun `a reversible operation keeps the undo history`() {
        appState.editCurrentEntryTag("edited")
        assertTrue(appState.canUndo)

        val newWav = sampleDir.resolve("wav/added.wav")
        TestWav.write(newWav, durationMs = 1000)
        val descriptor = ModuleOperationDescriptor(appState.requireProject().labelerConf, ModuleOperationType.Add)
        appState.executeModuleOperation(descriptor, mapOf<String, Any>("wavFile" to newWav.absolutePath).toParamMap())

        assertEquals(listOf("added", "doremi", "legato"), appState.requireProject().modules.map { it.name })
        assertTrue(appState.canUndo)
    }

    @Test
    fun `a failed operation keeps the project and the undo history`() {
        appState.editCurrentEntryTag("edited")
        assertTrue(appState.canUndo)
        val before = appState.requireProject()

        val descriptor = ModuleOperationDescriptor(appState.requireProject().labelerConf, ModuleOperationType.Rename)
        // renaming to an existing name fails inside the script
        appState.executeModuleOperation(descriptor, mapOf<String, Any>("newName" to "legato").toParamMap())

        assertEquals(before, appState.requireProject())
        assertTrue(appState.canUndo)
    }
}
