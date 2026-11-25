package ui

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.ipc.IpcState
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.repository.ChartRepository
import com.sdercolin.vlabeler.repository.ConvertedAudioRepository
import com.sdercolin.vlabeler.repository.SampleInfoRepository
import com.sdercolin.vlabeler.ui.AppErrorState
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.Screen
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogPurpose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import testutil.FakeIpcState
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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for the app-level glue in [AppState] that is not covered by `EditorStateTest`: screen switching, project
 * open/close wiring, plugin/labeler filtering against the disabled lists in the app record, error passthrough, and
 * the shutdown path. A real [AppState] is built with [TestAppState.create] (IPC disabled) and an unconfined
 * [mainScope] so that launched work runs synchronously up to its first suspension point.
 */
class AppStateGlueTest {

    private lateinit var scope: CoroutineScope
    private val loadedProjects = mutableSetOf<Project>()

    @BeforeTest
    fun setup() {
        Log.muted = true
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    }

    @AfterTest
    fun teardown() {
        scope.cancel()
        loadedProjects.forEach {
            ChartRepository.clear(it)
            SampleInfoRepository.clear(it)
            ConvertedAudioRepository.clear(it)
        }
        loadedProjects.clear()
        Log.muted = false
    }

    private fun create(plugins: List<Plugin> = emptyList()) = TestAppState.create(scope = scope, plugins = plugins)

    private fun openProject(appState: AppState) {
        appState.openEditor(otoProject)
        loadedProjects += otoProject
    }

    private fun macroPlugin(name: String) = plugin(name, Plugin.Type.Macro)
    private fun templatePlugin(name: String) = plugin(name, Plugin.Type.Template)
    private fun plugin(name: String, type: Plugin.Type) = Plugin(
        name = name,
        type = type,
        author = "tester",
        supportedLabelFileExtension = "*",
        scriptFiles = emptyList(),
    )

    /* region screen switching and project open/close */

    @Test
    fun `a fresh app has no project and starts on the starter screen`() {
        val appState = create()
        assertIs<Screen.Starter>(appState.screen)
        assertFalse(appState.hasProject)
        assertNull(appState.project)
        assertFalse(appState.shouldExit)
        assertFailsWith<IllegalArgumentException> { appState.requireProject() }
    }

    @Test
    fun `requestOpenProjectCreator switches to the creator screen and closes open dialogs`() {
        val appState = create()
        appState.openProjectSettingDialog()

        appState.requestOpenProjectCreator()

        assertIs<Screen.ProjectCreator>(appState.screen)
        // switching the screen closes the project setting dialog
        assertFalse(appState.isShowingProjectSettingDialog)
    }

    @Test
    fun `requestOpenProjectCreator asks to save first when there are unsaved changes`() {
        val appState = create()
        appState.projectContentChanged()
        assertTrue(appState.hasUnsavedChanges)

        appState.requestOpenProjectCreator()

        // instead of switching immediately, the ask-if-save dialog is queued and the screen is unchanged
        val request = assertNotNull(appState.embeddedDialog)
        assertIs<AskIfSaveDialogPurpose.IsCreatingNew>(request.args)
        assertIs<Screen.Starter>(appState.screen)
    }

    @Test
    fun `closeProjectCreator resets to the starter screen`() {
        val appState = create()
        appState.requestOpenProjectCreator()
        assertIs<Screen.ProjectCreator>(appState.screen)

        appState.closeProjectCreator()
        assertIs<Screen.Starter>(appState.screen)
    }

    @Test
    fun `openEditor loads the project and switches to the editor screen`() {
        val appState = create()
        openProject(appState)

        assertTrue(appState.hasProject)
        val editorScreen = assertIs<Screen.Editor>(appState.screen)
        assertEquals("test-project", appState.requireProject().projectName)
        assertEquals("test-project", editorScreen.state.project.projectName)
        // the editor exposed by the screen state is the one created for the loaded project
        assertSame(editorScreen.state, appState.editor)
    }

    @Test
    fun `requestCloseProject resets the project and screen when there are no unsaved changes`() {
        val appState = create()
        openProject(appState)
        assertTrue(appState.hasProject)

        appState.requestCloseProject()

        assertFalse(appState.hasProject)
        assertNull(appState.project)
        assertIs<Screen.Starter>(appState.screen)
    }

    /* endregion */

    /* region plugin / labeler filtering */

    @Test
    fun `getPlugins and getActivePlugins filter by type and disabled names`() {
        val template = templatePlugin("t1")
        val macroEnabled = macroPlugin("m1")
        val macroDisabled = macroPlugin("m2")
        val appState = create(plugins = listOf(template, macroEnabled, macroDisabled))

        assertEquals(listOf(template), appState.getPlugins(Plugin.Type.Template))
        assertEquals(listOf(macroEnabled, macroDisabled), appState.getPlugins(Plugin.Type.Macro))

        // all plugins are active until disabled in the app record
        assertEquals(listOf(macroEnabled, macroDisabled), appState.getActivePlugins(Plugin.Type.Macro))

        appState.appRecordStore.update { setPluginDisabled("m2", true) }
        assertEquals(listOf(macroEnabled), appState.getActivePlugins(Plugin.Type.Macro))
        // getPlugins ignores the disabled list
        assertEquals(listOf(macroEnabled, macroDisabled), appState.getPlugins(Plugin.Type.Macro))
    }

    @Test
    fun `activeLabelerConfs excludes labelers disabled in the app record`() {
        val appState = create()
        val available = appState.availableLabelerConfs
        assertEquals(2, available.size)
        assertEquals(available, appState.activeLabelerConfs)

        val disabledName = available.first().name
        appState.appRecordStore.update { setLabelerDisabled(disabledName, true) }

        assertEquals(available.drop(1), appState.activeLabelerConfs)
        // the available list itself is unchanged
        assertEquals(available, appState.availableLabelerConfs)
    }

    /* endregion */

    /* region error passthrough and shutdown */

    @Test
    fun `showError stores the error and pending action and clearError resets them`() {
        val appState = create()
        val error = IllegalStateException("boom")

        appState.showError(error, AppErrorState.ErrorPendingAction.ExitProject)
        assertSame(error, appState.error)
        assertEquals(AppErrorState.ErrorPendingAction.ExitProject, appState.errorPendingAction)

        appState.clearError()
        assertNull(appState.error)
        assertNull(appState.errorPendingAction)
    }

    @Test
    fun `handleErrorPendingAction ExitProject resets the project`() {
        val appState = create()
        openProject(appState)
        assertTrue(appState.hasProject)

        appState.handleErrorPendingAction(AppErrorState.ErrorPendingAction.ExitProject)

        assertFalse(appState.hasProject)
        assertIs<Screen.Starter>(appState.screen)
    }

    @Test
    fun `handleErrorPendingAction Exit triggers the shutdown flag`() {
        val appState = create()
        appState.handleErrorPendingAction(AppErrorState.ErrorPendingAction.Exit)
        assertTrue(appState.shouldExit)
    }

    @Test
    fun `handleErrorPendingAction null is a no-op`() {
        val appState = create()
        appState.handleErrorPendingAction(null)
        assertFalse(appState.shouldExit)
        assertIs<Screen.Starter>(appState.screen)
    }

    @Test
    fun `exit closes the ipc state and flags shouldExit`() {
        val appState = create()
        assertFalse(appState.shouldExit)

        appState.exit()

        assertTrue(appState.shouldExit)
        val ipcState = readIpcState(appState)
        val fake = assertIs<FakeIpcState>(ipcState)
        assertTrue(fake.closed)
    }

    private fun readIpcState(appState: AppState): IpcState {
        val field = AppState::class.java.getDeclaredField("ipcState")
        field.isAccessible = true
        return field.get(appState) as IpcState
    }

    /* endregion */

    companion object {

        private val sharedTempDir: File by lazy {
            createTempDirectory("vlabeler-glue-test").toFile().also { dir ->
                Runtime.getRuntime().addShutdownHook(Thread { dir.deleteRecursively() })
            }
        }

        private val otoProject: Project by lazy {
            val sampleDir = TestFixtures.deploy(
                "oto",
                sharedTempDir.resolve("oto"),
                wavFiles = listOf("_a_ka.wav"),
            )
            createTestProject(
                labeler = TestLabelers.utauOto,
                sampleDirectory = sampleDir,
                inputFilePath = sampleDir.resolve("oto.ini").absolutePath,
            )
        }
    }
}
