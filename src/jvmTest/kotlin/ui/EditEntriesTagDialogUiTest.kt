package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.dialog.EditEntriesTagDialog
import com.sdercolin.vlabeler.ui.dialog.EditEntriesTagDialogArgs
import com.sdercolin.vlabeler.ui.dialog.EditEntriesTagDialogResult
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class EditEntriesTagDialogUiTest {

    private fun args() = EditEntriesTagDialogArgs(indexes = listOf(0, 1, 2), initial = "old")

    @Test
    fun testEditTagReturnsResult() = runComposeUiTest {
        var result: EditEntriesTagDialogResult? = null
        setContent {
            AppTheme {
                EditEntriesTagDialog(args(), finish = { result = it })
            }
        }
        onNode(hasSetTextAction()).performTextReplacement("new")
        onNodeWithText("OK").performClick()
        assertEquals(listOf(0, 1, 2), result?.indexes)
        assertEquals("new", result?.tag)
    }

    @Test
    fun testEmptyTagIsAllowed() = runComposeUiTest {
        var result: EditEntriesTagDialogResult? = null
        setContent {
            AppTheme {
                EditEntriesTagDialog(args(), finish = { result = it })
            }
        }
        onNode(hasSetTextAction()).performTextClearance()
        onNodeWithText("OK").performClick()
        assertEquals("", result?.tag)
    }

    @Test
    fun testCancelReturnsNull() = runComposeUiTest {
        var called = false
        var result: EditEntriesTagDialogResult? = null
        setContent {
            AppTheme {
                EditEntriesTagDialog(args(), finish = { called = true; result = it })
            }
        }
        onNodeWithText("Cancel").performClick()
        assertEquals(true, called)
        assertNull(result)
    }
}
