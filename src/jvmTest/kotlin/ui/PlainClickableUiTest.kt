package ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class PlainClickableUiTest {

    @Test
    fun testPlainClickableInvokesOnClick() = runComposeUiTest {
        var clicks = 0
        setContent {
            AppTheme {
                Box(
                    Modifier
                        .testTag("target")
                        .size(50.dp)
                        .plainClickable { clicks++ },
                )
            }
        }
        onNodeWithTag("target").performClick()
        assertEquals(1, clicks)
    }
}
