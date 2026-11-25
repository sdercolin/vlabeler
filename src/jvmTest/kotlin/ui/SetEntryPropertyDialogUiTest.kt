package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.dialog.SetEntryPropertyDialog
import com.sdercolin.vlabeler.ui.dialog.SetEntryPropertyDialogArgs
import com.sdercolin.vlabeler.ui.dialog.SetEntryPropertyDialogResult
import com.sdercolin.vlabeler.ui.string.toLocalized
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class SetEntryPropertyDialogUiTest {

    private fun args() = SetEntryPropertyDialogArgs(
        currentValue = 1.5f,
        propertyIndex = 2,
        propertyDisplayedName = "Overlap".toLocalized(),
    )

    @Test
    fun testValidValueReturnsResult() = runComposeUiTest {
        var result: SetEntryPropertyDialogResult? = null
        setContent {
            AppTheme {
                SetEntryPropertyDialog(args(), finish = { result = it as SetEntryPropertyDialogResult? })
            }
        }
        onNode(hasSetTextAction()).performTextReplacement("2.5")
        onNodeWithText("OK").performClick()
        assertEquals(2.5f, result?.newValue)
        assertEquals(2, result?.propertyIndex)
    }

    @Test
    fun testNonNumericValueDisablesConfirm() = runComposeUiTest {
        setContent {
            AppTheme {
                SetEntryPropertyDialog(args(), finish = {})
            }
        }
        onNode(hasSetTextAction()).performTextReplacement("abc")
        onNodeWithText("OK").assert(isNotEnabled())
    }

    @Test
    fun testCancelReturnsNull() = runComposeUiTest {
        var called = false
        var result: SetEntryPropertyDialogResult? = null
        setContent {
            AppTheme {
                SetEntryPropertyDialog(args(), finish = { called = true; result = it as SetEntryPropertyDialogResult? })
            }
        }
        onNodeWithText("Cancel").performClick()
        assertEquals(true, called)
        assertNull(result)
    }
}
