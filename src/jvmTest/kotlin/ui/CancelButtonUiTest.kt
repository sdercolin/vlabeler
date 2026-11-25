package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.common.CancelButton
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CancelButtonUiTest {

    @Test
    fun testLabelAndClick() = runComposeUiTest {
        var clicks = 0
        setContent {
            AppTheme {
                CancelButton(onClick = { clicks++ })
            }
        }
        onNodeWithText("Cancel").performClick()
        assertEquals(1, clicks)
    }
}
