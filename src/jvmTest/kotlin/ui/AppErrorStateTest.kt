package ui

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.ui.AppErrorState
import com.sdercolin.vlabeler.ui.AppErrorStateImpl
import testutil.TestEnv
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class AppErrorStateTest {

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    @Test
    fun `initial state has no error`() {
        val state = AppErrorStateImpl()
        assertNull(state.error)
        assertNull(state.errorPendingAction)
    }

    @Test
    fun `showError sets the error without pending action`() {
        val state = AppErrorStateImpl()
        val error = IllegalStateException("test")
        state.showError(error)
        assertSame(error, state.error)
        assertNull(state.errorPendingAction)
    }

    @Test
    fun `showError sets the error with pending action`() {
        val state = AppErrorStateImpl()
        val error = IllegalStateException("test")
        state.showError(error, AppErrorState.ErrorPendingAction.Exit)
        assertSame(error, state.error)
        assertEquals(AppErrorState.ErrorPendingAction.Exit, state.errorPendingAction)
    }

    @Test
    fun `showError overwrites the previous error`() {
        val state = AppErrorStateImpl()
        state.showError(IllegalStateException("first"), AppErrorState.ErrorPendingAction.Exit)
        val second = IllegalArgumentException("second")
        state.showError(second)
        assertSame(second, state.error)
        assertNull(state.errorPendingAction)
    }

    @Test
    fun `clearError resets the state`() {
        val state = AppErrorStateImpl()
        state.showError(IllegalStateException("test"), AppErrorState.ErrorPendingAction.ExitProject)
        state.clearError()
        assertNull(state.error)
        assertNull(state.errorPendingAction)
    }
}
