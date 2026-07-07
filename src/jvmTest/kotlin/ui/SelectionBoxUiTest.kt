package ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.common.SelectionBox
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SelectionBoxUiTest {

    @Test
    fun testOpenDropdownAndSelectOption() = runComposeUiTest {
        val value = mutableStateOf("One")
        setContent {
            AppTheme {
                SelectionBox(
                    value = value.value,
                    onSelect = { value.value = it },
                    options = listOf("One", "Two", "Three"),
                )
            }
        }
        // dropdown is closed initially
        onNodeWithText("Two").assertDoesNotExist()

        // clicking the box opens the dropdown
        onNodeWithText("One").performClick()
        onNodeWithText("Two").assertExists()
        onNodeWithText("Three").assertExists()

        // selecting an option commits it and closes the dropdown
        onNodeWithText("Two").performClick()
        assertEquals("Two", value.value)
        onNodeWithText("Three").assertDoesNotExist()
        onNodeWithText("Two").assertExists()
    }

    @Test
    fun testDisabledSelectionBoxDoesNotOpen() = runComposeUiTest {
        setContent {
            AppTheme {
                SelectionBox(
                    value = "One",
                    onSelect = {},
                    options = listOf("One", "Two"),
                    enabled = false,
                )
            }
        }
        onNodeWithText("One").performClick()
        onNodeWithText("Two").assertDoesNotExist()
    }
}
