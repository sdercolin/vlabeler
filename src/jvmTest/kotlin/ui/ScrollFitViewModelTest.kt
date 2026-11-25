package ui

import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ScrollFitViewModelTest {

    @Test
    fun `emit delivers the pending value with the current mode`() = runTest {
        val viewModel = ScrollFitViewModel(this)
        val events = mutableListOf<ScrollFitViewModel.Event>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events.add(it) }
        }

        viewModel.setMode(ScrollFitViewModel.Mode.FORWARD)
        viewModel.emit()
        advanceUntilIdle()
        assertEquals(listOf(ScrollFitViewModel.Event(0, ScrollFitViewModel.Mode.FORWARD)), events)

        collector.cancel()
    }

    @Test
    fun `emit resets the mode to normal`() = runTest {
        val viewModel = ScrollFitViewModel(this)
        val events = mutableListOf<ScrollFitViewModel.Event>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events.add(it) }
        }

        viewModel.setMode(ScrollFitViewModel.Mode.FORWARD)
        viewModel.emit()
        viewModel.emit()
        advanceUntilIdle()
        assertEquals(
            listOf(
                ScrollFitViewModel.Event(0, ScrollFitViewModel.Mode.FORWARD),
                ScrollFitViewModel.Event(0, ScrollFitViewModel.Mode.NORMAL),
            ),
            events,
        )

        collector.cancel()
    }

    @Test
    fun `emitNext alone does not emit an event`() = runTest {
        val viewModel = ScrollFitViewModel(this)
        val events = mutableListOf<ScrollFitViewModel.Event>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events.add(it) }
        }

        viewModel.emitNext()
        advanceUntilIdle()
        assertEquals(emptyList(), events)

        collector.cancel()
    }

    @Test
    fun `update without a matching entry does not emit even when waiting`() = runTest {
        val viewModel = ScrollFitViewModel(this)
        val events = mutableListOf<ScrollFitViewModel.Event>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.eventFlow.collect { events.add(it) }
        }

        viewModel.emitNext()
        viewModel.update(
            showLeftSide = true,
            horizontalScrollState = androidx.compose.foundation.ScrollState(0),
            canvasLength = 1000f,
            entriesInPixel = emptyList(),
            currentIndex = 0,
        )
        advanceUntilIdle()
        assertEquals(emptyList(), events)

        collector.cancel()
    }
}
