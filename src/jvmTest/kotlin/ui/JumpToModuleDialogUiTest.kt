package ui

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
    fun testSubmitJumpsToTheGivenModuleIndex() {
        // The dialog turns a selected module into its result via ModuleListState.submit, which is the callback the
        // dialog wires to its finish result. (The focus-driven selectedIndex path is a generic NavigatorList detail
        // whose UI interaction is not deterministically replayable in runComposeUiTest.)
        var jumped: Int? = null
        val state = ModuleListState(project(), jumpToModule = { jumped = it })
        state.submit(1)
        assertEquals(1, jumped)
    }
}
