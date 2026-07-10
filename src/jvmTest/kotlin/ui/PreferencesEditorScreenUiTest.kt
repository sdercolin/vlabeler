package ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.repository.ColorPaletteRepository
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesEditor
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesEditorState
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesPage
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesPages
import com.sdercolin.vlabeler.ui.theme.AppTheme
import testutil.TestEnv
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Screen-level Compose UI tests for [PreferencesEditor], mounting the real editor over an explicitly built
 * [PreferencesEditorState] (proven safe against an uninitialized [AppState] in [PreferencesEditorStateTest]) so that
 * the render blocks execute and their behavior can be asserted through semantics.
 *
 * The [AppState] is dereferenced only by [com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesItem.Button]
 * `onClick` handlers, which these tests never trigger. The `submit`/`apply`/`onViewPage`/`showSnackbar` callbacks are
 * captured so that the wiring from the button bar down to the state can be verified.
 */
@OptIn(ExperimentalTestApi::class)
class PreferencesEditorScreenUiTest {

    private val submitted = mutableListOf<AppConf?>()
    private val applied = mutableListOf<AppConf>()
    private val viewedPages = mutableListOf<PreferencesPage>()
    private val snackbars = mutableListOf<String>()

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
        ColorPaletteRepository.directory.mkdirs()
    }

    @AfterTest
    fun teardown() {
        submitted.clear()
        applied.clear()
        viewedPages.clear()
        snackbars.clear()
        Log.muted = false
    }

    private fun uninitializedAppState(): AppState {
        val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(AppState::class.java) as AppState
    }

    private fun createState(initConf: AppConf = AppConf()) = PreferencesEditorState(
        appState = uninitializedAppState(),
        initConf = initConf,
        submit = { submitted += it },
        apply = { applied += it },
        initialPage = null,
        onViewPage = { viewedPages += it },
        showSnackbar = { snackbars += it },
        launchArgs = null,
    )

    /**
     * Mounts the editor over the given [state]. The [submit]/[apply]/[onViewPage]/[showSnackbar] arguments passed
     * here are ignored because an explicit [state] is provided; the state's own captured callbacks are asserted on.
     */
    @Composable
    private fun Editor(state: PreferencesEditorState) {
        PreferencesEditor(
            appState = uninitializedAppState(),
            currentConf = AppConf(),
            submit = { submitted += it },
            apply = { applied += it },
            initialPage = null,
            onViewPage = { viewedPages += it },
            showSnackbar = { snackbars += it },
            launchArgs = null,
            state = state,
        )
    }

    /** Matches the icon-only "settings" [androidx.compose.material.IconButton] in the button bar. */
    private val settingsButton = SemanticsMatcher("icon-only button") { node ->
        node.config.getOrNull(SemanticsProperties.Role) == Role.Button &&
            node.config.getOrNull(SemanticsProperties.Text).isNullOrEmpty()
    }

    @Test
    fun `the page tree renders the root pages`() = runComposeUiTest {
        val state = createState()
        setContent { AppTheme { Editor(state) } }

        // pages that are not selected appear exactly once, in the page list
        onNodeWithText("Edit history").assertExists()
        onNodeWithText("Playback").assertExists()
        onNodeWithText("Editor").assertExists()
    }

    @Test
    fun `selecting a page shows its items and reports the viewed page`() = runComposeUiTest {
        val state = createState()
        setContent { AppTheme { Editor(state) } }

        onNodeWithText("Edit history").performClick()

        assertEquals(PreferencesPages.History, state.selectedPage.model)
        assertTrue(PreferencesPages.History in viewedPages)
        onNodeWithText("Maximum retained size").assertExists()
        onNodeWithText("Squash index changes").assertExists()
    }

    @Test
    fun `toggling a switch updates the working conf`() = runComposeUiTest {
        val state = createState()
        setContent { AppTheme { Editor(state) } }
        onNodeWithText("Edit history").performClick()

        // the history page has a single switch (squash index changes), on by default
        onNode(isToggleable()).assertIsOn()
        assertTrue(state.conf.history.squashIndex)

        onNode(isToggleable()).performClick()

        onNode(isToggleable()).assertIsOff()
        assertFalse(state.conf.history.squashIndex)
        assertTrue(state.needSave)
    }

    @Test
    fun `editing an integer item updates the working conf`() = runComposeUiTest {
        val state = createState()
        setContent { AppTheme { Editor(state) } }
        onNodeWithText("Edit history").performClick()

        // the history page has a single integer input (maximum retained size)
        onNode(hasSetTextAction()).performTextReplacement("150")

        assertEquals(150, state.conf.history.maxSize)
        assertTrue(state.needSave)
    }

    @Test
    fun `apply is disabled without changes and applies the working conf once edited`() = runComposeUiTest {
        val state = createState()
        setContent { AppTheme { Editor(state) } }

        onNodeWithText("Apply").assertIsNotEnabled()

        onNodeWithText("Edit history").performClick()
        onNode(hasSetTextAction()).performTextReplacement("150")

        onNodeWithText("Apply").assertIsEnabled()
        onNodeWithText("Apply").performClick()

        assertEquals(listOf(state.conf), applied)
        assertEquals(150, applied.single().history.maxSize)
        assertEquals(state.conf, state.savedConf)
        assertFalse(state.needSave)
    }

    @Test
    fun `the OK control submits the edited conf`() = runComposeUiTest {
        val state = createState()
        setContent { AppTheme { Editor(state) } }
        onNodeWithText("Edit history").performClick()
        onNode(hasSetTextAction()).performTextReplacement("150")

        onNodeWithText("OK").performClick()

        assertEquals(listOf<AppConf?>(state.conf), submitted)
        assertEquals(150, submitted.single()?.history?.maxSize)
    }

    @Test
    fun `the cancel control submits null`() = runComposeUiTest {
        val state = createState()
        setContent { AppTheme { Editor(state) } }

        onNodeWithText("Cancel").performClick()

        assertEquals(listOf<AppConf?>(null), submitted)
    }

    @Test
    fun `resetting all items via the settings menu restores the default conf`() = runComposeUiTest {
        val state = createState()
        setContent { AppTheme { Editor(state) } }
        onNodeWithText("Edit history").performClick()
        onNode(hasSetTextAction()).performTextReplacement("150")
        assertTrue(state.needSave)

        onNode(settingsButton).performClick()
        onNodeWithText("Reset all items").assertExists()
        onNodeWithText("Reset all items").performClick()

        assertEquals(AppConf(), state.conf)
        assertFalse(state.needSave)
    }

    @Test
    fun `importing and exporting are reachable from the settings menu`() = runComposeUiTest {
        val state = createState()
        setContent { AppTheme { Editor(state) } }

        onNode(settingsButton).performClick()

        onNodeWithText("Import").assertExists()
        onNodeWithText("Export").assertExists()
        onNodeWithText("Reset items in this page").assertExists()
        // opening the import picker sets the current file picker without touching the conf
        onNodeWithText("Import").performClick()
        assertEquals(PreferencesEditorState.FilePicker.Import, state.currentFilePicker)
        assertNull(submitted.firstOrNull())
    }
}
