package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialog
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogAction
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogResult
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CommonConfirmationDialogUiTest {

    @Test
    fun testShowsLocalizedDescription() = runComposeUiTest {
        val action = CommonConfirmationDialogAction.RemoveEntries(listOf(0, 1, 2))
        setContent {
            AppTheme {
                CommonConfirmationDialog(action = action, finish = {})
            }
        }
        onNodeWithText("Removing 3 entries...").assertExists()
        onNodeWithText("OK").assertExists()
        onNodeWithText("Cancel").assertExists()
    }

    @Test
    fun testCancelFinishesWithNull() = runComposeUiTest {
        val action = CommonConfirmationDialogAction.RemoveEntries(listOf(0))
        var finished = false
        var result: CommonConfirmationDialogResult? = null
        setContent {
            AppTheme {
                CommonConfirmationDialog(
                    action = action,
                    finish = {
                        finished = true
                        result = it
                    },
                )
            }
        }
        onNodeWithText("Cancel").performClick()
        assertTrue(finished)
        assertNull(result)
    }

    @Test
    fun testConfirmFinishesWithAction() = runComposeUiTest {
        val action = CommonConfirmationDialogAction.RemoveEntries(listOf(0))
        var result: CommonConfirmationDialogResult? = null
        setContent {
            AppTheme {
                CommonConfirmationDialog(
                    action = action,
                    finish = { result = it },
                )
            }
        }
        onNodeWithText("OK").performClick()
        assertEquals(true, result != null)
        assertSame(action, result?.action)
    }
}
