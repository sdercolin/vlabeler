package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.Module
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.dialog.JumpToEntryDialog
import com.sdercolin.vlabeler.ui.dialog.JumpToEntryDialogArgs
import com.sdercolin.vlabeler.ui.editor.EntryListFilterState
import com.sdercolin.vlabeler.ui.editor.EntryListState
import com.sdercolin.vlabeler.ui.theme.AppTheme
import testutil.TestLabelers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * [JumpToEntryDialog] is a thin wrapper around [com.sdercolin.vlabeler.ui.editor.EntryList] which drives selection
 * through pointer/focus events that cannot be replayed deterministically in [runComposeUiTest]. The dialog rendering is
 * covered by a mount test; the search-and-submit logic (equivalent to typing then pressing Enter) is covered through the
 * public [EntryListState], which is the state the dialog hosts and whose `jumpToEntry` callback becomes the dialog
 * result.
 */
@OptIn(ExperimentalTestApi::class)
class JumpToEntryDialogUiTest {

    @BeforeTest
    fun setup() {
        Log.muted = true
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    private fun entry(name: String) = Entry(
        sample = "a.wav",
        name = name,
        start = 0f,
        end = 100f,
        points = emptyList(),
        extras = emptyList(),
    )

    private fun project() = Project(
        rootSampleDirectoryPath = "/tmp",
        workingDirectoryPath = ".",
        projectName = "test",
        cacheDirectoryPath = "cache",
        originalLabelerConf = TestLabelers.utauOto,
        modules = listOf(
            Module(
                name = "",
                sampleDirectoryPath = "",
                entries = listOf("e0", "e1", "e2", "e3", "e4").map(::entry),
                currentIndex = 0,
            ),
        ),
        currentModuleIndex = 0,
        autoExport = false,
    )

    @Test
    fun testShowsEntries() = runComposeUiTest {
        setContent {
            AppTheme {
                JumpToEntryDialog(
                    JumpToEntryDialogArgs(project(), AppConf().editor, AppConf().view),
                    finish = {},
                )
            }
        }
        onNodeWithText("e0").assertExists()
        onNodeWithText("e4").assertExists()
    }

    @Test
    fun testSearchFiltersEntries() {
        val filterState = EntryListFilterState()
        filterState.editFilter { copy(searchText = "e2") }
        val state = EntryListState(
            viewConf = AppConf().view,
            filterState = filterState,
            project = project(),
            jumpToEntry = {},
            dialogState = null,
            enableContextMenu = false,
        )
        assertEquals(listOf(2), state.searchResult.map { it.index })
    }

    @Test
    fun testSubmitJumpsToTheGivenEntryIndex() {
        // The dialog turns a selected entry into its result via EntryListState.submit, which is the callback the
        // dialog wires to its finish result. (The focus-driven selectedIndex path is a generic NavigatorList detail
        // whose UI interaction is not deterministically replayable in runComposeUiTest.)
        var jumped: Int? = null
        val state = EntryListState(
            viewConf = AppConf().view,
            filterState = EntryListFilterState(),
            project = project(),
            jumpToEntry = { jumped = it },
            dialogState = null,
            enableContextMenu = false,
        )
        state.submit(2)
        assertEquals(2, jumped)
    }
}
