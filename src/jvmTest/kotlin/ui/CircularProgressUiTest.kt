package ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.common.CircularProgress
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class CircularProgressUiTest {

    @Test
    fun testBlockingOverlayConsumesClicks() = runComposeUiTest {
        var clicks = 0
        setContent {
            AppTheme {
                Box(Modifier.fillMaxSize()) {
                    Button(
                        onClick = { clicks++ },
                        modifier = Modifier.testTag("under").fillMaxSize(),
                    ) {
                        Text("Under")
                    }
                    CircularProgress(blocking = true)
                }
            }
        }
        onNodeWithTag("under").performClick()
        assertEquals(0, clicks)
    }

    @Test
    fun testNonBlockingOverlayLetsClicksThrough() = runComposeUiTest {
        var clicks = 0
        setContent {
            AppTheme {
                Box(Modifier.fillMaxSize()) {
                    Button(
                        onClick = { clicks++ },
                        modifier = Modifier.testTag("under").fillMaxSize(),
                    ) {
                        Text("Under")
                    }
                    CircularProgress(blocking = false)
                }
            }
        }
        onNodeWithTag("under").performClick()
        assertEquals(1, clicks)
    }
}
