package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialog
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogArgs
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogResult
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class EditEntryNameDialogUiTest {

    private fun args(
        showSnackbar: (String) -> Unit = {},
        invalidOptions: List<String> = emptyList(),
        presets: List<String> = emptyList(),
    ) = InputEntryNameDialogArgs(
        index = 1,
        initial = "name",
        invalidOptions = invalidOptions,
        showSnackbar = showSnackbar,
        purpose = InputEntryNameDialogPurpose.Rename,
        presets = presets,
    )

    @Test
    fun testValidNameReturnsResult() = runComposeUiTest {
        var result: InputEntryNameDialogResult? = null
        setContent {
            AppTheme {
                InputEntryNameDialog(args(), finish = { result = it })
            }
        }
        onNode(hasSetTextAction()).performTextReplacement("renamed")
        onNodeWithText("OK").performClick()
        assertEquals(1, result?.index)
        assertEquals("renamed", result?.name)
        assertEquals(InputEntryNameDialogPurpose.Rename, result?.purpose)
    }

    @Test
    fun testBlankNameDisablesConfirm() = runComposeUiTest {
        setContent {
            AppTheme {
                InputEntryNameDialog(args(), finish = {})
            }
        }
        onNode(hasSetTextAction()).performTextClearance()
        onNodeWithText("OK").assert(isNotEnabled())
    }

    @Test
    fun testInvalidNameShowsSnackbarAndDoesNotSubmit() = runComposeUiTest {
        var snackbar: String? = null
        var result: InputEntryNameDialogResult? = null
        var called = false
        setContent {
            AppTheme {
                InputEntryNameDialog(
                    args(showSnackbar = { snackbar = it }, invalidOptions = listOf("taken")),
                    finish = { called = true; result = it },
                )
            }
        }
        onNode(hasSetTextAction()).performTextReplacement("taken")
        onNodeWithText("OK").performClick()
        assertEquals("The name you input already exists.", snackbar)
        assertEquals(false, called)
        assertNull(result)
    }

    @Test
    fun testPresetFillsInputAndSubmits() = runComposeUiTest {
        var result: InputEntryNameDialogResult? = null
        setContent {
            AppTheme {
                InputEntryNameDialog(args(presets = listOf("preset-a", "preset-b")), finish = { result = it })
            }
        }
        onNodeWithText("preset-b").performClick()
        onNodeWithText("OK").performClick()
        assertEquals("preset-b", result?.name)
    }

    @Test
    fun testCancelReturnsNull() = runComposeUiTest {
        var called = false
        var result: InputEntryNameDialogResult? = null
        setContent {
            AppTheme {
                InputEntryNameDialog(args(), finish = { called = true; result = it })
            }
        }
        onNodeWithText("Cancel").performClick()
        assertEquals(true, called)
        assertNull(result)
    }
}
