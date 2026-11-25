package ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.common.InputBox
import com.sdercolin.vlabeler.ui.common.IntegerInputBox
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class InputBoxUiTest {

    @Test
    fun testErrorPromptAppearsAndDisappears() = runComposeUiTest {
        val value = mutableStateOf("")
        setContent {
            AppTheme {
                InputBox(
                    value = value.value,
                    onValueChange = { value.value = it },
                    errorPrompt = { if (it.isBlank()) "Required" else null },
                )
            }
        }
        onNodeWithText("Required").assertExists()
        onNode(hasSetTextAction()).performTextInput("abc")
        assertEquals("abc", value.value)
        onNodeWithText("Required").assertDoesNotExist()
    }

    @Test
    fun testDisabledInputBoxRejectsInput() = runComposeUiTest {
        setContent {
            AppTheme {
                InputBox(
                    value = "locked",
                    onValueChange = {},
                    enabled = false,
                )
            }
        }
        // the text field is marked disabled, so it cannot receive input
        onNode(hasSetTextAction()).assert(isNotEnabled())
        onNodeWithText("locked").assertExists()
    }

    @Test
    fun testIntegerInputBoxValidation() = runComposeUiTest {
        val intValue = mutableStateOf(5)
        setContent {
            AppTheme {
                IntegerInputBox(
                    enabled = true,
                    intValue = intValue.value,
                    onValueChange = { intValue.value = it },
                    min = 1,
                    max = 10,
                    getInvalidPrompt = { null },
                )
            }
        }
        // non-integer input shows the integer prompt and does not commit
        onNode(hasSetTextAction()).performTextClearance()
        onNodeWithText("Please enter an integer number.").assertExists()
        assertEquals(5, intValue.value)

        // out-of-range input shows the range prompt and does not commit
        onNode(hasSetTextAction()).performTextInput("50")
        onNodeWithText("Please enter a number between 1 and 10.").assertExists()
        assertEquals(5, intValue.value)

        // valid input commits and clears the error
        onNode(hasSetTextAction()).performTextClearance()
        onNode(hasSetTextAction()).performTextInput("7")
        assertEquals(7, intValue.value)
        onNodeWithText("Please enter a number between 1 and 10.").assertDoesNotExist()
        onNodeWithText("Please enter an integer number.").assertDoesNotExist()
    }
}
