package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.ui.dialog.MoveEntryDialog
import com.sdercolin.vlabeler.ui.dialog.MoveEntryDialogArgs
import com.sdercolin.vlabeler.ui.dialog.MoveEntryDialogResult
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class MoveEntryDialogUiTest {

    private val entries = List(5) { index ->
        Entry(sample = "a.wav", name = "e$index", start = 0f, end = 100f, points = emptyList(), extras = emptyList())
    }

    private fun args() = MoveEntryDialogArgs(currentIndex = 0, entries = entries, viewConf = AppConf().view)

    @Test
    fun testMoveToValidIndexReturnsResult() = runComposeUiTest {
        var result: MoveEntryDialogResult? = null
        setContent {
            AppTheme {
                MoveEntryDialog(args(), finish = { result = it as MoveEntryDialogResult? })
            }
        }
        // the displayed index is 1-based, so "3" moves to the zero-based index 2
        onNode(hasSetTextAction()).performTextReplacement("3")
        onNodeWithText("OK").performClick()
        assertEquals(0, result?.oldIndex)
        assertEquals(2, result?.newIndex)
    }

    @Test
    fun testInvalidIndexDisablesConfirm() = runComposeUiTest {
        setContent {
            AppTheme {
                MoveEntryDialog(args(), finish = {})
            }
        }
        onNode(hasSetTextAction()).performTextReplacement("999")
        onNodeWithText("OK").assert(isNotEnabled())
    }

    @Test
    fun testCancelReturnsNull() = runComposeUiTest {
        var called = false
        var result: MoveEntryDialogResult? = null
        setContent {
            AppTheme {
                MoveEntryDialog(args(), finish = { called = true; result = it as MoveEntryDialogResult? })
            }
        }
        onNodeWithText("Cancel").performClick()
        assertEquals(true, called)
        assertNull(result)
    }
}
