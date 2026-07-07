package ui

import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import com.sdercolin.vlabeler.ui.AppSnackbarStateImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppSnackbarStateTest {

    @Test
    fun `showSnackbar exposes the message via the host state`() = runTest {
        val hostState = SnackbarHostState()
        val state = AppSnackbarStateImpl(hostState)

        var finished = false
        val job = launch {
            state.showSnackbar("hello", actionLabel = "OK", duration = SnackbarDuration.Short)
            finished = true
        }
        advanceUntilIdle()

        val data = assertNotNull(hostState.currentSnackbarData)
        assertEquals("hello", data.message)
        assertEquals("OK", data.actionLabel)
        assertEquals(SnackbarDuration.Short, data.duration)
        assertFalse(finished)

        data.dismiss()
        advanceUntilIdle()
        assertTrue(finished)
        assertNull(hostState.currentSnackbarData)
        job.join()
    }

    @Test
    fun `showSnackbar completes when the action is performed`() = runTest {
        val hostState = SnackbarHostState()
        val state = AppSnackbarStateImpl(hostState)

        var finished = false
        val job = launch {
            state.showSnackbar("message", actionLabel = "Retry")
            finished = true
        }
        advanceUntilIdle()

        assertNotNull(hostState.currentSnackbarData).performAction()
        advanceUntilIdle()
        assertTrue(finished)
        assertNull(hostState.currentSnackbarData)
        job.join()
    }

    @Test
    fun `showSnackbar queues messages until the current one is dismissed`() = runTest {
        val hostState = SnackbarHostState()
        val state = AppSnackbarStateImpl(hostState)

        val first = launch { state.showSnackbar("first") }
        val second = launch { state.showSnackbar("second") }
        advanceUntilIdle()

        assertEquals("first", assertNotNull(hostState.currentSnackbarData).message)
        assertNotNull(hostState.currentSnackbarData).dismiss()
        advanceUntilIdle()

        assertEquals("second", assertNotNull(hostState.currentSnackbarData).message)
        assertNotNull(hostState.currentSnackbarData).dismiss()
        advanceUntilIdle()
        assertNull(hostState.currentSnackbarData)
        first.join()
        second.join()
    }
}
