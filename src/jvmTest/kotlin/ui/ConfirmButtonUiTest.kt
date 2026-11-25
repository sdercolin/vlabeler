package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ConfirmButtonUiTest {

    @Test
    fun testDefaultLabelAndClick() = runComposeUiTest {
        var clicks = 0
        setContent {
            AppTheme {
                ConfirmButton(onClick = { clicks++ })
            }
        }
        onNodeWithText("OK").performClick()
        assertEquals(1, clicks)
    }

    @Test
    fun testCustomText() = runComposeUiTest {
        setContent {
            AppTheme {
                ConfirmButton(onClick = {}, text = "Apply")
            }
        }
        onNodeWithText("Apply").assertExists()
        onNodeWithText("OK").assertDoesNotExist()
    }

    @Test
    fun testDisabledButtonDoesNotInvoke() = runComposeUiTest {
        var clicks = 0
        setContent {
            AppTheme {
                ConfirmButton(onClick = { clicks++ }, enabled = false)
            }
        }
        onNodeWithText("OK").performClick()
        assertEquals(0, clicks)
    }
}
