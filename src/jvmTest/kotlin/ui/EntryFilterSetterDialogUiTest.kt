package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.filter.EntryFilter
import com.sdercolin.vlabeler.ui.dialog.EntryFilterSetterDialog
import com.sdercolin.vlabeler.ui.dialog.EntryFilterSetterDialogArgs
import com.sdercolin.vlabeler.ui.dialog.EntryFilterSetterDialogResult
import com.sdercolin.vlabeler.ui.theme.AppTheme
import testutil.TestEnv
import testutil.TestLabelers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class EntryFilterSetterDialogUiTest {

    @BeforeTest
    fun setup() {
        Log.muted = true
        TestEnv.ensureLogDirectory()
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    private fun args(value: EntryFilter = EntryFilter()) = EntryFilterSetterDialogArgs(
        labelerConf = TestLabelers.utauOto,
        entries = listOf(
            Entry(sample = "a.wav", name = "e0", start = 0f, end = 100f, points = emptyList(), extras = emptyList()),
        ),
        value = value,
    )

    @Test
    fun testRendersTitleAndModes() = runComposeUiTest {
        setContent {
            AppTheme {
                EntryFilterSetterDialog(args(), finish = {})
            }
        }
        onNodeWithText("Filter Settings").assertExists()
        onNodeWithText("Basic").assertExists()
        onNodeWithText("Advanced").assertExists()
    }

    @Test
    fun testBasicModeSubmitReturnsFilter() = runComposeUiTest {
        var result: EntryFilterSetterDialogResult? = null
        setContent {
            AppTheme {
                EntryFilterSetterDialog(args(), finish = { result = it })
            }
        }
        // the first text field corresponds to the "any" search field
        onAllNodes(hasSetTextAction())[0].performTextInput("query")
        onNodeWithText("OK").performClick()
        assertEquals("query", result?.value?.searchText)
    }

    @Test
    fun testToggleToAdvancedShowsSelectorPlaceholder() = runComposeUiTest {
        var result: EntryFilterSetterDialogResult? = null
        setContent {
            AppTheme {
                EntryFilterSetterDialog(args(), finish = { result = it })
            }
        }
        onNodeWithText("Advanced").performClick()
        onNodeWithText("No filters, selecting all entries.").assertExists()
        // submitting in advanced mode with an empty selector yields a filter without a basic search text
        onNodeWithText("OK").performClick()
        assertEquals("", result?.value?.searchText)
        assertNull(result?.value?.advanced)
    }

    @Test
    fun testCancelReturnsNull() = runComposeUiTest {
        var called = false
        var result: EntryFilterSetterDialogResult? = null
        setContent {
            AppTheme {
                EntryFilterSetterDialog(args(), finish = { called = true; result = it })
            }
        }
        onNodeWithText("Cancel").performClick()
        assertEquals(true, called)
        assertNull(result)
    }
}
