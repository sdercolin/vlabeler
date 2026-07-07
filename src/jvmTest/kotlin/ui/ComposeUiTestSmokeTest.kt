package ui

import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.common.InputBox
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test

/**
 * Smoke tests proving that the Compose UI test harness ([runComposeUiTest]) works in this environment, including on
 * headless CI machines. Component-specific UI tests live in their own classes.
 */
@OptIn(ExperimentalTestApi::class)
class ComposeUiTestSmokeTest {

    @Test
    fun testStateDrivenRecomposition() = runComposeUiTest {
        setContent {
            var count by remember { mutableStateOf(0) }
            Button(
                onClick = { count++ },
                modifier = Modifier.testTag("button"),
            ) {
                Text("count: $count")
            }
        }
        onNodeWithText("count: 0").assertExists()
        onNodeWithTag("button").performClick()
        onNodeWithText("count: 1").assertExists()
    }

    @Test
    fun testAppThemedComponentWithTextInput() = runComposeUiTest {
        setContent {
            AppTheme {
                var value by remember { mutableStateOf("") }
                InputBox(
                    value = value,
                    onValueChange = { value = it },
                    modifier = Modifier.testTag("input"),
                )
            }
        }
        // the tag is on InputBox's wrapper; the editable text field is the descendant with a SetText action
        onNode(hasSetTextAction()).performTextInput("hello")
        onNode(hasSetTextAction()).assertTextContains("hello", substring = true)
    }
}
