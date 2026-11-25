package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.ui.common.ColorHexInputBox
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ColorHexInputBoxUiTest {

    @Test
    fun testSanitizesInputAndCommitsRgbValue() = runComposeUiTest {
        val committed = mutableListOf<String>()
        setContent {
            AppTheme {
                ColorHexInputBox(
                    value = "#123456",
                    defaultValue = "#000000",
                    onValidValue = { committed += it },
                    useAlpha = false,
                )
            }
        }
        // non-hex chars are dropped and the value is truncated to #RRGGBB
        onNode(hasSetTextAction()).performTextReplacement("12345678zz")
        onNode(hasSetTextAction()).assertTextContains("#123456")
        assertEquals("#123456", committed.last())
    }

    @Test
    fun testPrependsHashAndNormalizesCase() = runComposeUiTest {
        val committed = mutableListOf<String>()
        setContent {
            AppTheme {
                ColorHexInputBox(
                    value = "#000000",
                    defaultValue = "#000000",
                    onValidValue = { committed += it },
                    useAlpha = false,
                )
            }
        }
        onNode(hasSetTextAction()).performTextReplacement("abcdef")
        onNode(hasSetTextAction()).assertTextContains("#abcdef")
        // the committed value is the normalized upper-case hex string
        assertEquals("#ABCDEF", committed.last())
    }

    @Test
    fun testAlphaModeKeepsNineChars() = runComposeUiTest {
        val committed = mutableListOf<String>()
        setContent {
            AppTheme {
                ColorHexInputBox(
                    value = "#FF000000",
                    defaultValue = "#FF000000",
                    onValidValue = { committed += it },
                    useAlpha = true,
                )
            }
        }
        onNode(hasSetTextAction()).performTextReplacement("80ABCDEF1234")
        onNode(hasSetTextAction()).assertTextContains("#80ABCDEF")
        assertEquals("#80ABCDEF", committed.last())
    }

    @Test
    fun testInvalidInitialValueFallsBackToDefault() = runComposeUiTest {
        setContent {
            AppTheme {
                ColorHexInputBox(
                    value = "not a color",
                    defaultValue = "#FF0000",
                    onValidValue = {},
                    useAlpha = false,
                )
            }
        }
        onNode(hasSetTextAction()).assertTextContains("#FF0000")
    }
}
