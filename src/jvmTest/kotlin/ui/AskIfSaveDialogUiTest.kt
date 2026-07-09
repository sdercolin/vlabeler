package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialog
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogResult
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class AskIfSaveDialogUiTest {

    @Test
    fun testYesReturnsSaveWithAction() = runComposeUiTest {
        var result: AskIfSaveDialogResult? = null
        setContent {
            AppTheme {
                AskIfSaveDialog(AskIfSaveDialogPurpose.IsClosing, finish = { result = it })
            }
        }
        onNodeWithText("Yes").performClick()
        assertEquals(true, result?.save)
        assertEquals(AppState.PendingActionAfterSaved.Close, result?.actionAfterSaved)
    }

    @Test
    fun testNoReturnsNoSaveWithAction() = runComposeUiTest {
        var result: AskIfSaveDialogResult? = null
        setContent {
            AppTheme {
                AskIfSaveDialog(AskIfSaveDialogPurpose.IsClosing, finish = { result = it })
            }
        }
        onNodeWithText("No").performClick()
        assertEquals(false, result?.save)
        assertEquals(AppState.PendingActionAfterSaved.Close, result?.actionAfterSaved)
    }

    @Test
    fun testCancelReturnsNull() = runComposeUiTest {
        var called = false
        var result: AskIfSaveDialogResult? = null
        setContent {
            AppTheme {
                AskIfSaveDialog(AskIfSaveDialogPurpose.IsExiting, finish = { called = true; result = it })
            }
        }
        onNodeWithText("Cancel").performClick()
        assertEquals(true, called)
        assertNull(result)
    }
}
