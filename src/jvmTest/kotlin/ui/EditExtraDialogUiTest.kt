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
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.ui.dialog.EditExtraDialog
import com.sdercolin.vlabeler.ui.dialog.EditExtraDialogArgs
import com.sdercolin.vlabeler.ui.dialog.EditExtraDialogResult
import com.sdercolin.vlabeler.ui.dialog.EditExtraDialogTarget
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class EditExtraDialogUiTest {

    private val extraFields = listOf(
        LabelerConf.ExtraField(name = "required", default = null, isVisible = true, isEditable = true),
        LabelerConf.ExtraField(
            name = "optional",
            default = null,
            isVisible = true,
            isEditable = true,
            isOptional = true,
        ),
    )

    private fun args() = EditExtraDialogArgs(
        index = 3,
        initial = listOf("value0", null),
        extraFields = extraFields,
        target = EditExtraDialogTarget.EditEntry,
    )

    @Test
    fun testRendersDescription() = runComposeUiTest {
        setContent {
            AppTheme {
                EditExtraDialog(args(), finish = {})
            }
        }
        onNodeWithText("Edit extra information of current entry").assertExists()
    }

    @Test
    fun testEditAndSubmitReturnsResult() = runComposeUiTest {
        var result: EditExtraDialogResult? = null
        setContent {
            AppTheme {
                EditExtraDialog(args(), finish = { result = it })
            }
        }
        onAllNodes(hasSetTextAction())[1].performTextReplacement("value1")
        onNodeWithText("OK").performClick()
        assertEquals(3, result?.index)
        assertEquals(listOf("value0", "value1"), result?.extras)
        assertEquals(EditExtraDialogTarget.EditEntry, result?.target)
    }

    @Test
    fun testMissingRequiredFieldDisablesConfirm() = runComposeUiTest {
        setContent {
            AppTheme {
                EditExtraDialog(args(), finish = {})
            }
        }
        // clearing the non-optional field makes it null, which is invalid
        onAllNodes(hasSetTextAction())[0].performTextClearance()
        onNodeWithText("OK").assert(isNotEnabled())
    }

    @Test
    fun testCancelReturnsNull() = runComposeUiTest {
        var called = false
        var result: EditExtraDialogResult? = null
        setContent {
            AppTheme {
                EditExtraDialog(args(), finish = { called = true; result = it })
            }
        }
        onNodeWithText("Cancel").performClick()
        assertEquals(true, called)
        assertNull(result)
    }
}
