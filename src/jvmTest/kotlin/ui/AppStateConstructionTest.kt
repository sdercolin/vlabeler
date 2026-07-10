package ui

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.ui.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import testutil.TestAppState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Verifies that a real [com.sdercolin.vlabeler.ui.AppState] can be constructed in tests (IPC disabled), which is the
 * basis for the state-glue and dialog-state tests that use [TestAppState].
 */
class AppStateConstructionTest {

    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setup() {
        Log.muted = true
        scope = CoroutineScope(SupervisorJob())
    }

    @AfterTest
    fun teardown() {
        scope.cancel()
        Log.muted = false
    }

    @Test
    fun testConstructsWithoutBindingIpcPort() {
        val appState = TestAppState.create(scope = CoroutineScope(Job()))
        // fresh app starts on the starter screen with no project
        assertTrue(appState.screen == Screen.Starter)
        assertFalse(appState.hasProject)
    }
}
