package ui.common

import androidx.compose.runtime.Composable
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.common.ContextMenuSubject
import com.sdercolin.vlabeler.ui.common.NavigatorListState
import com.sdercolin.vlabeler.ui.common.NoOpContextMenuAction
import testutil.TestLabelers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NavigatorListStateTest {

    private class FakeItem(override val index: Int) : ContextMenuSubject<NoOpContextMenuAction> {
        @Composable
        override fun getContextMenuActions(): List<NoOpContextMenuAction> = emptyList()
    }

    /**
     * Minimal [NavigatorListState] backed by plain vars so the pure selection logic in [updateSearch] can be
     * exercised without any Compose/Project machinery.
     */
    private class FakeNavigatorListState(
        override val currentIndex: Int,
        private val filteredIndexes: List<Int>,
        private val filterActive: Boolean = false,
    ) : NavigatorListState<FakeItem, NoOpContextMenuAction> {
        override var selectedIndex: Int? = null
        override var isFiltered: Boolean = false
        override var searchResult: List<FakeItem> = emptyList()
        override var hasFocus: Boolean = false
        override val labelerConf: LabelerConf = TestLabelers.audacity

        override fun submit(index: Int) {}
        override fun updateProject(project: Project) {}
        override fun calculateResult(): Pair<Boolean, List<FakeItem>> =
            filterActive to filteredIndexes.map { FakeItem(it) }
    }

    @Test
    fun `opening the list pre-selects the current item`() {
        val state = FakeNavigatorListState(currentIndex = 3, filteredIndexes = (0..4).toList())
        state.updateSearch()
        // selectedIndex is a row index into searchResult; the current item (index 3) sits at row 3.
        assertEquals(3, state.selectedIndex)
    }

    @Test
    fun `pre-selection follows current item even when the search bar is focused`() {
        // Regression: the go-to dialog auto-focuses its search bar, which used to force selection to the first row.
        val state = FakeNavigatorListState(currentIndex = 2, filteredIndexes = (0..4).toList())
        state.hasFocus = true
        state.updateSearch()
        assertEquals(2, state.selectedIndex)
    }

    @Test
    fun `typing a query selects the first result`() {
        val state = FakeNavigatorListState(currentIndex = 3, filteredIndexes = listOf(1, 3, 4), filterActive = true)
        state.updateSearch(selectFirst = true)
        assertEquals(0, state.selectedIndex)
    }

    @Test
    fun `no selection when the current item is filtered out`() {
        val state = FakeNavigatorListState(currentIndex = 3, filteredIndexes = listOf(0, 1, 2), filterActive = true)
        state.updateSearch()
        assertNull(state.selectedIndex)
    }

    @Test
    fun `no selection when there are no results`() {
        val state = FakeNavigatorListState(currentIndex = 0, filteredIndexes = emptyList(), filterActive = true)
        state.updateSearch(selectFirst = true)
        assertNull(state.selectedIndex)
    }
}
