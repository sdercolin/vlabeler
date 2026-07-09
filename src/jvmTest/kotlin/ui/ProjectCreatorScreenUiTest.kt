package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.starter.ProjectCreator
import com.sdercolin.vlabeler.ui.starter.ProjectCreatorState
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import testutil.TestEnv
import testutil.TestLabelers
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Screen-level Compose UI tests for [ProjectCreator], mounting the real wizard over an explicitly built
 * [ProjectCreatorState] (proven safe against an uninitialized [AppState] in [ProjectCreatorStateTest]) so that the
 * large render blocks execute and their behavior can be asserted through semantics.
 *
 * The [AppState] is allocated without running its constructor: the screen only dereferences it inside the labeler
 * and template-plugin settings dialogs (gated behind icon clicks that these tests never perform) and in `create()`
 * (only reached by clicking "Finish", which the tests avoid). The [AppRecordStore] uses an already-cancelled scope
 * so nothing is written to the real application directory.
 */
@OptIn(ExperimentalTestApi::class)
class ProjectCreatorScreenUiTest {

    private lateinit var scope: CoroutineScope
    private val tempDirs = mutableListOf<File>()

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @AfterTest
    fun teardown() {
        scope.cancel()
        tempDirs.forEach { it.deleteRecursively() }
        tempDirs.clear()
        Log.muted = false
    }

    private fun newTempDir(): File = createTempDirectory("vlabeler-test").toFile().also { tempDirs += it }

    private fun uninitializedAppState(): AppState {
        val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(AppState::class.java) as AppState
    }

    private fun cancelledScope(): CoroutineScope = CoroutineScope(Job().apply { cancel() })

    /**
     * Builds a [ProjectCreatorState] whose directory page is already valid (an existing sample directory and a valid
     * project name), so that the "Next" control is enabled and page navigation can be exercised.
     */
    private fun createValidState(
        labelers: List<LabelerConf> = defaultLabelers,
        plugins: List<Plugin> = emptyList(),
    ): ProjectCreatorState = ProjectCreatorState(
        uninitializedAppState(),
        scope,
        labelers,
        plugins,
        AppRecordStore(AppRecord(), cancelledScope()),
        null,
    ).apply {
        updateSampleDirectory(newTempDir().absolutePath)
        updateProjectName("proj")
    }

    @Test
    fun `initial directory page renders the title and the directory fields`() = runComposeUiTest {
        val state = createValidState()
        setContent {
            AppTheme {
                ProjectCreator(
                    appState = uninitializedAppState(),
                    cancel = {},
                    activeLabelerConfs = defaultLabelers,
                    activeTemplatePlugins = emptyList(),
                    appRecordStore = AppRecordStore(AppRecord(), cancelledScope()),
                    initialFile = null,
                    coroutineScope = scope,
                    state = state,
                )
            }
        }

        onNodeWithText("New Project - Directory Settings").assertExists()
        onNodeWithText("Sample directory").assertExists()
        onNodeWithText("Project name").assertExists()
        onNodeWithText("Advanced Settings").assertExists()
        // a valid directory page enables the Next control
        onNodeWithText("Next").assertIsEnabled()
    }

    @Test
    fun `next and previous navigate between the wizard pages and change the rendered content`() = runComposeUiTest {
        val state = createValidState()
        setContent {
            AppTheme {
                ProjectCreator(
                    appState = uninitializedAppState(),
                    cancel = {},
                    activeLabelerConfs = defaultLabelers,
                    activeTemplatePlugins = emptyList(),
                    appRecordStore = AppRecordStore(AppRecord(), cancelledScope()),
                    initialFile = null,
                    coroutineScope = scope,
                    state = state,
                )
            }
        }

        onNodeWithText("New Project - Directory Settings").assertExists()

        onNodeWithText("Next").performClick()
        assertEquals(ProjectCreatorState.Page.Labeler, state.page)
        onNodeWithText("New Project - Labeler Settings").assertExists()
        // the labeler page renders the category selector and the built-in categories
        onNodeWithText("Category").assertExists()
        onNodeWithText("UTAU").assertExists()

        onNodeWithText("Next").performClick()
        assertEquals(ProjectCreatorState.Page.DataSource, state.page)
        onNodeWithText("New Project - Data Source Settings").assertExists()
        // the data source page renders the content-type selector
        onNodeWithText("Create by...").assertExists()
        onNodeWithText("Default").assertExists()
        // the last page turns the confirm control into "Finish"
        onNodeWithText("Finish").assertExists()

        onNodeWithText("Previous").performClick()
        assertEquals(ProjectCreatorState.Page.Labeler, state.page)
        onNodeWithText("New Project - Labeler Settings").assertExists()
    }

    @Test
    fun `an invalid project name surfaces the error state by disabling the next control`() = runComposeUiTest {
        val state = createValidState()
        setContent {
            AppTheme {
                ProjectCreator(
                    appState = uninitializedAppState(),
                    cancel = {},
                    activeLabelerConfs = defaultLabelers,
                    activeTemplatePlugins = emptyList(),
                    appRecordStore = AppRecordStore(AppRecord(), cancelledScope()),
                    initialFile = null,
                    coroutineScope = scope,
                    state = state,
                )
            }
        }

        onNodeWithText("Next").assertIsEnabled()

        // the directory page has two text fields; the second one is the project name field
        onAllNodes(hasSetTextAction())[1].performTextReplacement("in/valid")

        assertFalse(state.isProjectNameValid())
        onNodeWithText("Next").assertIsNotEnabled()
    }

    @Test
    fun `the cancel control invokes the cancel callback`() = runComposeUiTest {
        var cancelled = false
        val state = createValidState()
        setContent {
            AppTheme {
                ProjectCreator(
                    appState = uninitializedAppState(),
                    cancel = { cancelled = true },
                    activeLabelerConfs = defaultLabelers,
                    activeTemplatePlugins = emptyList(),
                    appRecordStore = AppRecordStore(AppRecord(), cancelledScope()),
                    initialFile = null,
                    coroutineScope = scope,
                    state = state,
                )
            }
        }

        onNodeWithText("Cancel").performClick()
        assertTrue(cancelled)
    }

    companion object {

        private val defaultLabelers: List<LabelerConf> by lazy {
            listOf(
                TestLabelers.utauSinger,
                TestLabelers.utauOto,
                TestLabelers.nnsvsSinger,
                TestLabelers.audacity,
                TestLabelers.sinsy,
            )
        }
    }
}
