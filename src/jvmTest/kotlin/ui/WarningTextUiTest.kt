package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.common.WarningText
import com.sdercolin.vlabeler.ui.common.WarningTextStyle
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class WarningTextUiTest {

    @Test
    fun testWarningStyleShowsWarningTitle() = runComposeUiTest {
        setContent {
            AppTheme {
                WarningText(text = "Something risky happened.", style = WarningTextStyle.Warning)
            }
        }
        onNodeWithText("Warning").assertExists()
        onNodeWithText("Something risky happened.").assertExists()
        onNodeWithText("Error").assertDoesNotExist()
    }

    @Test
    fun testErrorStyleShowsErrorTitle() = runComposeUiTest {
        setContent {
            AppTheme {
                WarningText(text = "Something failed.", style = WarningTextStyle.Error)
            }
        }
        onNodeWithText("Error").assertExists()
        onNodeWithText("Something failed.").assertExists()
        onNodeWithText("Warning").assertDoesNotExist()
    }
}
