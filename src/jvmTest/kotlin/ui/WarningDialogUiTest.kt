package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.common.WarningTextStyle
import com.sdercolin.vlabeler.ui.dialog.WarningDialog
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class WarningDialogUiTest {

    @Test
    fun testRendersMessageAndErrorTitle() = runComposeUiTest {
        setContent {
            AppTheme {
                WarningDialog(message = "Something failed", finish = {}, style = WarningTextStyle.Error)
            }
        }
        onNodeWithText("Something failed").assertExists()
        onNodeWithText("Error").assertExists()
    }

    @Test
    fun testRendersWarningTitle() = runComposeUiTest {
        setContent {
            AppTheme {
                WarningDialog(message = "Careful now", finish = {}, style = WarningTextStyle.Warning)
            }
        }
        onNodeWithText("Careful now").assertExists()
        onNodeWithText("Warning").assertExists()
    }

    @Test
    fun testConfirmInvokesFinish() = runComposeUiTest {
        var finished = false
        setContent {
            AppTheme {
                WarningDialog(message = "msg", finish = { finished = true }, style = WarningTextStyle.Error)
            }
        }
        onNodeWithText("OK").performClick()
        assertEquals(true, finished)
    }
}
