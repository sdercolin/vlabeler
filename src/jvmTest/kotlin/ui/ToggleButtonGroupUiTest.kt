package ui

import androidx.compose.material.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.common.ToggleButtonGroup
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ToggleButtonGroupUiTest {

    @Test
    fun testClickChangesSelection() = runComposeUiTest {
        val selected = mutableStateOf("A")
        setContent {
            AppTheme {
                ToggleButtonGroup(
                    selected = selected.value,
                    options = listOf("A", "B", "C"),
                    onSelectedChange = { selected.value = it },
                    buttonContent = { Text(it) },
                )
            }
        }
        onNodeWithText("B").performClick()
        assertEquals("B", selected.value)
        onNodeWithText("C").performClick()
        assertEquals("C", selected.value)
    }

    @Test
    fun testClickingSelectedOptionStillInvokesCallback() = runComposeUiTest {
        var calls = 0
        setContent {
            AppTheme {
                ToggleButtonGroup(
                    selected = "A",
                    options = listOf("A", "B"),
                    onSelectedChange = { calls++ },
                    buttonContent = { Text(it) },
                )
            }
        }
        onNodeWithText("A").performClick()
        assertEquals(1, calls)
    }
}
