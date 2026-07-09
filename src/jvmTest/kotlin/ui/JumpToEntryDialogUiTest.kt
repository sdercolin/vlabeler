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
    fun testSelectingFilteredEntrySubmitsItsIndex() {
        var jumped: Int? = null
        // Run the whole sequence inside one mutable snapshot so the search filter write, the state construction that
        // reads it, and the updateSearch/submitCurrent reads all share a consistent snapshot. Otherwise, on CI the
        // filter write (a mutableStateOf write made outside any snapshot) can read back stale inside the state,
        // leaving the list unfiltered and selecting the wrong entry.
        Snapshot.withMutableSnapshot {
            val filterState = EntryListFilterState()
            filterState.editFilter { copy(searchText = "e2") }
            val state = EntryListState(
                viewConf = AppConf().view,
                filterState = filterState,
                project = project(),
                jumpToEntry = { jumped = it },
                dialogState = null,
                enableContextMenu = false,
            )
            // typing focuses the search bar, so the first result is selected; pressing Enter submits it
            state.hasFocus = true
            state.updateSearch()
            state.submitCurrent()
        }
        assertEquals(2, jumped)
    }
}
