package ui

import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.loadPlugins
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.dialog.plugin.BasePluginDialogState
import com.sdercolin.vlabeler.ui.dialog.plugin.PluginDialogState
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.util.ParamMap
import com.sdercolin.vlabeler.util.RecordDir
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import testutil.TestEnv
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Compose UI tests for the plugin parameter dialog in
 * [com.sdercolin.vlabeler.ui.dialog.plugin.PluginDialog], mounted over a real [PluginDialogState] built from a bundled
 * plugin (the same construction as [PluginDialogStateTest]).
 *
 * The public entry points ([com.sdercolin.vlabeler.ui.dialog.plugin.TemplatePluginDialog] etc.) wrap the content in a
 * `DialogWindow`, which spawns a separate top-level AWT window that is not part of the test scene (and fails on headless
 * CI). The dialog body is rendered by the file-private `Content(state, appRecordStore)` composable, so it is invoked
 * here reflectively by handing it the current [Composer]. Recomposition afterwards goes through the compiler-generated
 * restart scope, so the reflective call only bootstraps the first composition.
 */
@OptIn(ExperimentalTestApi::class)
class PluginDialogUiTest {

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
        RecordDir.mkdirs()
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    private class Callbacks {
        val submitted = mutableListOf<ParamMap?>()
        val loaded = mutableListOf<ParamMap>()
        val saved = mutableListOf<ParamMap>()
    }

    private fun cancelledScope(): CoroutineScope = CoroutineScope(Job().apply { cancel() })

    private fun store(): AppRecordStore = AppRecordStore(AppRecord(), cancelledScope())

    private fun createState(
        plugin: Plugin,
        callbacks: Callbacks = Callbacks(),
        appRecordStore: AppRecordStore = store(),
        paramMap: ParamMap = plugin.getDefaultParams(),
        savedParamMap: ParamMap? = plugin.getDefaultParams(),
    ): PluginDialogState = PluginDialogState(
        plugin = plugin,
        appRecordStore = appRecordStore,
        snackbarHostState = SnackbarHostState(),
        paramMap = paramMap,
        savedParamMap = savedParamMap,
        project = null,
        submit = { callbacks.submitted += it },
        load = { callbacks.loaded += it },
        save = { callbacks.saved += it },
        executable = false,
        slot = null,
    )

    private fun PluginDialogState.indexOf(name: String): Int = paramDefs.indexOfFirst { it.name == name }

    /**
     * Invokes the file-private `Content(state, appRecordStore)` composable, passing the current composer so the
     * reflective call participates in the composition. See the class doc for why this is necessary.
     */
    @Composable
    private fun PluginDialogContent(state: BasePluginDialogState, appRecordStore: AppRecordStore) {
        val composer = currentComposer
        contentMethod.invoke(null, state, appRecordStore, composer, 0)
    }

    @Test
    fun rendersTitleParamsAndControls() = runComposeUiTest {
        val plugin = templatePlugin("cvvc-oto-gen")
        val appRecordStore = store()
        val state = createState(plugin, appRecordStore = appRecordStore)
        setContent {
            AppTheme {
                PluginDialogContent(state, appRecordStore)
            }
        }
        // the plugin title and a parameter label render
        onNodeWithText(plugin.displayedName.getCertain(Language.English)).assertExists()
        onNodeWithText("BPM").assertExists()
        // parameter inputs render, and the confirm button is enabled with the default (valid) params
        assertTrue(onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty())
        onNodeWithText("OK").assert(isEnabled())
        // export/import/reset/save icon controls plus cancel and confirm are all clickable
        assertTrue(onAllNodes(hasClickAction()).fetchSemanticsNodes().size >= 6)
    }

    @Test
    fun invalidParamDisablesApply() = runComposeUiTest {
        val plugin = templatePlugin("cvvc-oto-gen")
        val appRecordStore = store()
        val state = createState(plugin, appRecordStore = appRecordStore)
        setContent {
            AppTheme {
                PluginDialogContent(state, appRecordStore)
            }
        }
        // bpm is the first editable field; below its minimum (0) it is invalid
        onAllNodes(hasSetTextAction())[0].performTextClearance()
        onAllNodes(hasSetTextAction())[0].performTextInput("-1")
        assertFalse(state.isValid(state.indexOf("bpm")))
        assertFalse(state.isAllValid())
        onNodeWithText("OK").assert(isNotEnabled())
    }

    @Test
    fun editingValidParamUpdatesValueAndKeepsApplyEnabled() = runComposeUiTest {
        val plugin = templatePlugin("cvvc-oto-gen")
        val appRecordStore = store()
        val state = createState(plugin, appRecordStore = appRecordStore)
        setContent {
            AppTheme {
                PluginDialogContent(state, appRecordStore)
            }
        }
        onAllNodes(hasSetTextAction())[0].performTextClearance()
        onAllNodes(hasSetTextAction())[0].performTextInput("90")
        assertEquals(90f, state.getCurrentParamMap()["bpm"])
        assertTrue(state.isAllValid())
        onNodeWithText("OK").assert(isEnabled())
    }

    @Test
    fun applyFiresWithCurrentParams() = runComposeUiTest {
        val plugin = templatePlugin("cvvc-oto-gen")
        val callbacks = Callbacks()
        val appRecordStore = store()
        val state = createState(plugin, callbacks, appRecordStore = appRecordStore)
        setContent {
            AppTheme {
                PluginDialogContent(state, appRecordStore)
            }
        }
        onAllNodes(hasSetTextAction())[0].performTextClearance()
        onAllNodes(hasSetTextAction())[0].performTextInput("90")
        // the confirm button sits below the scrollable parameter list
        onNodeWithText("OK").performScrollTo().performClick()

        assertEquals(1, callbacks.submitted.size)
        assertEquals(90f, callbacks.submitted.single()?.get("bpm"))
    }

    @Test
    fun cancelFiresWithNull() = runComposeUiTest {
        val plugin = templatePlugin("cvvc-oto-gen")
        val callbacks = Callbacks()
        val appRecordStore = store()
        val state = createState(plugin, callbacks, appRecordStore = appRecordStore)
        setContent {
            AppTheme {
                PluginDialogContent(state, appRecordStore)
            }
        }
        onNodeWithText("Cancel").performScrollTo().performClick()

        assertEquals(listOf<ParamMap?>(null), callbacks.submitted)
    }

    @Test
    fun exportControlOpensPresetMenu() = runComposeUiTest {
        val plugin = templatePlugin("cvvc-oto-gen")
        val appRecordStore = store()
        val state = createState(plugin, appRecordStore = appRecordStore)
        setContent {
            AppTheme {
                PluginDialogContent(state, appRecordStore)
            }
        }
        // the export button is the first clickable control; opening it lists the preset targets
        onAllNodes(hasClickAction())[0].performClick()
        onNodeWithText("Save parameters as default").assertExists()
        onNodeWithText("Export parameters to file").assertExists()
    }

    companion object {

        private val contentMethod by lazy {
            Class.forName("com.sdercolin.vlabeler.ui.dialog.plugin.PluginDialogKt")
                .declaredMethods
                .first { it.name == "Content" && it.parameterCount == 4 }
                .apply { isAccessible = true }
        }

        private val templatePlugins: List<Plugin> by lazy {
            TestEnv.ensureLogDirectory()
            loadPlugins(Plugin.Type.Template, Language.English)
        }

        private fun templatePlugin(name: String): Plugin = templatePlugins.first { it.name == name }
    }
}
