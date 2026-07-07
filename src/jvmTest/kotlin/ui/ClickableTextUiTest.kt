package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.common.PartialClickableText
import com.sdercolin.vlabeler.ui.common.SingleClickableText
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ClickableTextUiTest {

    @Test
    fun testSingleClickableTextInvokesOnClick() = runComposeUiTest {
        var clicks = 0
        setContent {
            AppTheme {
                SingleClickableText(
                    text = "Click me",
                    onClick = { clicks++ },
                )
            }
        }
        onNodeWithText("Click me").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun testDisabledSingleClickableTextIgnoresClick() = runComposeUiTest {
        var clicks = 0
        setContent {
            AppTheme {
                SingleClickableText(
                    text = "Click me",
                    onClick = { clicks++ },
                    enabled = false,
                )
            }
        }
        onNodeWithText("Click me").performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun testPartialClickableTextInvokesAnnotatedCallback() = runComposeUiTest {
        var clicks = 0
        setContent {
            AppTheme {
                PartialClickableText(
                    text = "open settings",
                    clickables = listOf("open settings" to { clicks++ }),
                )
            }
        }
        onNodeWithText("open settings").performClick()
        assertEquals(1, clicks)
    }
}
