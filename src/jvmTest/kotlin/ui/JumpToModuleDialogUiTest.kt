package ui

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.Module
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.dialog.JumpToModuleDialog
import com.sdercolin.vlabeler.ui.dialog.JumpToModuleDialogArgs
import com.sdercolin.vlabeler.ui.editor.ModuleListState
import com.sdercolin.vlabeler.ui.theme.AppTheme
import testutil.TestLabelers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [JumpToModuleDialog] wraps [com.sdercolin.vlabeler.ui.editor.ModuleList]. The dialog rendering is covered by a mount
 * test; the search-and-submit logic is covered through the public [ModuleListState], whose `jumpToModule` callback
 * becomes the dialog result. See [JumpToEntryDialogUiTest] for the rationale.
 */
@OptIn(ExperimentalTestApi::class)
class JumpToModuleDialogUiTest {

    @BeforeTest
    fun setup() {
        Log.muted = true
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    private fun module(name: String) = Module(
        name = name,
        sampleDirectoryPath = name,
        entries = listOf(
            Entry(sample = "a.wav", name = "e", start = 0f, end = 100f, points = emptyList(), extras = emptyList()),
        ),
        currentIndex = 0,
    )

    private fun project() = Project(
        rootSampleDirectoryPath = "/tmp",
        workingDirectoryPath = ".",
        projectName = "test",
        cacheDirectoryPath = "cache",
        originalLabelerConf = TestLabelers.utauOto,
        modules = listOf("alpha", "beta", "gamma").map(::module),
        currentModuleIndex = 0,
        autoExport = false,
    )

    @Test
    fun testShowsModules() = runComposeUiTest {
        setContent {
            AppTheme {
                JumpToModuleDialog(
                    JumpToModuleDialogArgs(project(), AppConf().editor, AppConf().view),
                    finish = {},
                )
            }
        }
        onNodeWithText("alpha").assertExists()
        onNodeWithText("gamma").assertExists()
    }

    @Test
    fun testSearchFiltersModules() {
        val state = ModuleListState(project(), jumpToModule = {})
        state.searchText = "bet"
        state.updateSearch()
        assertEquals(listOf(1), state.searchResult.map { it.index })
    }

    @Test
    fun testSelectingFilteredModuleSubmitsItsIndex() {
        var jumped: Int? = null
        val state = ModuleListState(project(), jumpToModule = { jumped = it })
        // run inside a mutable snapshot so the selectedIndex written by updateSearch is observed by
        // submitCurrent regardless of any global snapshot state left by earlier runComposeUiTest tests
        Snapshot.withMutableSnapshot {
            state.searchText = "bet"
            state.hasFocus = true
            state.updateSearch()
            state.submitCurrent()
        }
        assertEquals(1, jumped)
    }
}
