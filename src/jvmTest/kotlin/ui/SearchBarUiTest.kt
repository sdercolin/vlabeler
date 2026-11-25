package ui

import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.common.SearchBar
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SearchBarUiTest {

    @Test
    fun testTypingUpdatesText() = runComposeUiTest {
        val text = mutableStateOf("")
        setContent {
            AppTheme {
                SearchBar(
                    text = text.value,
                    onTextChange = { text.value = it },
                )
            }
        }
        onNode(hasSetTextAction()).performTextInput("query")
        assertEquals("query", text.value)
        onNode(hasSetTextAction()).assertTextContains("query")
    }

    @Test
    fun testSubmitViaImeAction() = runComposeUiTest {
        val text = mutableStateOf("")
        var submitted = 0
        setContent {
            AppTheme {
                SearchBar(
                    text = text.value,
                    onTextChange = { text.value = it },
                    onSubmit = { submitted++ },
                )
            }
        }
        onNode(hasSetTextAction()).performTextInput("abc")
        onNode(hasSetTextAction()).performImeAction()
        assertEquals(1, submitted)
    }

    @Test
    fun testDisabledContentBlocksInputAndIsShown() = runComposeUiTest {
        setContent {
            AppTheme {
                SearchBar(
                    text = "",
                    onTextChange = {},
                    disabledContent = { Text("Disabled hint") },
                )
            }
        }
        // providing disabledContent disables the text field
        onNode(hasSetTextAction()).assert(isNotEnabled())
        onNodeWithText("Disabled hint").assertExists()
    }

    @Test
    fun testTrailingContentRendered() = runComposeUiTest {
        setContent {
            AppTheme {
                SearchBar(
                    text = "",
                    onTextChange = {},
                    trailingContent = { Text("Trailing") },
                )
            }
        }
        onNodeWithText("Trailing").assertExists()
        // the text field itself stays editable
        onNode(hasSetTextAction()).assertExists()
    }
}
