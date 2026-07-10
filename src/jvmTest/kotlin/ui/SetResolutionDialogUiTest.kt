package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.dialog.SetResolutionDialog
import com.sdercolin.vlabeler.ui.dialog.SetResolutionDialogArgs
import com.sdercolin.vlabeler.ui.dialog.SetResolutionDialogResult
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class SetResolutionDialogUiTest {

    private fun args() = SetResolutionDialogArgs(current = 100, min = 10, max = 1000)

    @Test
    fun testValidValueReturnsResult() = runComposeUiTest {
        var result: SetResolutionDialogResult? = null
        setContent {
            AppTheme {
                SetResolutionDialog(args(), finish = { result = it as SetResolutionDialogResult? })
            }
        }
        onNode(hasSetTextAction()).performTextReplacement("500")
        onNodeWithText("OK").performClick()
        assertEquals(500, result?.newValue)
    }

    @Test
    fun testOutOfRangeValueDisablesConfirm() = runComposeUiTest {
        setContent {
            AppTheme {
                SetResolutionDialog(args(), finish = {})
            }
        }
        onNode(hasSetTextAction()).performTextReplacement("5000")
        onNodeWithText("OK").assert(isNotEnabled())
    }

    @Test
    fun testNonNumericValueDisablesConfirm() = runComposeUiTest {
        setContent {
            AppTheme {
                SetResolutionDialog(args(), finish = {})
            }
        }
        onNode(hasSetTextAction()).performTextReplacement("abc")
        onNodeWithText("OK").assert(isNotEnabled())
    }

    @Test
    fun testValidValueSubmittedByKeyboardDone() = runComposeUiTest {
        var result: SetResolutionDialogResult? = null
        setContent {
            AppTheme {
                SetResolutionDialog(args(), finish = { result = it as SetResolutionDialogResult? })
            }
        }
        onNode(hasSetTextAction()).performTextReplacement("500")
        onNode(hasSetTextAction()).performImeAction()
        assertEquals(500, result?.newValue)
    }

    @Test
    fun testOutOfRangeValueNotSubmittedByKeyboardDone() = runComposeUiTest {
        var called = false
        setContent {
            AppTheme {
                SetResolutionDialog(args(), finish = { called = true })
            }
        }
        onNode(hasSetTextAction()).performTextReplacement("5000")
        // pressing Enter on an out-of-range value must not submit it, matching the disabled confirm button
        onNode(hasSetTextAction()).performImeAction()
        assertEquals(false, called)
    }

    @Test
    fun testCancelReturnsNull() = runComposeUiTest {
        var called = false
        var result: SetResolutionDialogResult? = null
        setContent {
            AppTheme {
                SetResolutionDialog(args(), finish = { called = true; result = it as SetResolutionDialogResult? })
            }
        }
        onNodeWithText("Cancel").performClick()
        assertEquals(true, called)
        assertNull(result)
    }
}
