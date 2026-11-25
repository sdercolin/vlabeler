package ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.EntrySelector
import com.sdercolin.vlabeler.ui.dialog.plugin.ParamEntrySelector
import com.sdercolin.vlabeler.ui.theme.AppTheme
import testutil.TestEnv
import testutil.TestLabelers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Compose UI tests for [ParamEntrySelector], the entry-selector parameter editor. The filter/selection model logic is
 * covered by [EntrySelectorStateTest]; this exercises the composable: that the filter rows, placeholder and expression
 * row render, and that edits (adding a filter, editing a matcher, editing the raw expression) propagate through
 * `onValueChange`.
 *
 * The preview summary requires a [com.sdercolin.vlabeler.util.JavaScript] runtime; it is passed as `null` here (so the
 * preview only shows the "Initializing..." placeholder), which keeps the tests free of a JS engine while still
 * exercising the whole composable tree.
 */
@OptIn(ExperimentalTestApi::class)
class EntrySelectorUiTest {

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    @Test
    fun emptySelectorShowsPlaceholderAndExpressionRow() = runComposeUiTest {
        val value = mutableStateOf(EntrySelector(emptyList()))
        setContent {
            AppTheme {
                ParamEntrySelector(
                    labelerConf = TestLabelers.utauOto,
                    value = value.value,
                    onValueChange = { value.value = it },
                    isError = false,
                    onParseErrorChange = {},
                    entries = null,
                    js = null,
                    enabled = true,
                    onError = {},
                )
            }
        }
        onNodeWithText("No filters, selecting all entries.").assertExists()
        onNodeWithText("Expression").assertExists()
    }

    @Test
    fun filterRowRendersForExistingFilter() = runComposeUiTest {
        val filter = EntrySelector.TextFilterItem("name", EntrySelector.TextMatchType.Contains, "a")
        val value = mutableStateOf(EntrySelector(listOf(filter), "#1"))
        setContent {
            AppTheme {
                ParamEntrySelector(
                    labelerConf = TestLabelers.utauOto,
                    value = value.value,
                    onValueChange = { value.value = it },
                    isError = false,
                    onParseErrorChange = {},
                    entries = null,
                    js = null,
                    enabled = true,
                    onError = {},
                )
            }
        }
        // the "#1" label appears twice: on the filter row and in the default expression box
        assertEquals(2, onAllNodesWithText("#1").fetchSemanticsNodes().size)
        // the placeholder is gone once there is a filter
        onNodeWithText("No filters, selecting all entries.").assertDoesNotExist()
    }

    @Test
    fun addFilterButtonAddsAFilter() = runComposeUiTest {
        val value = mutableStateOf(EntrySelector(emptyList()))
        setContent {
            AppTheme {
                ParamEntrySelector(
                    labelerConf = TestLabelers.utauOto,
                    value = value.value,
                    onValueChange = { value.value = it },
                    isError = false,
                    onParseErrorChange = {},
                    entries = null,
                    js = null,
                    enabled = true,
                    onError = {},
                )
            }
        }
        // the "+" add icon is the first clickable control (the "-" remove icon is disabled while empty)
        onAllNodes(hasClickAction())[0].performClick()
        assertEquals(1, value.value.filters.size)
        assertIs<EntrySelector.TextFilterItem>(value.value.filters.single())
    }

    @Test
    fun editingMatcherTextPropagates() = runComposeUiTest {
        val filter = EntrySelector.TextFilterItem("name", EntrySelector.TextMatchType.Contains, "")
        val value = mutableStateOf(EntrySelector(listOf(filter)))
        setContent {
            AppTheme {
                ParamEntrySelector(
                    labelerConf = TestLabelers.utauOto,
                    value = value.value,
                    onValueChange = { value.value = it },
                    isError = false,
                    onParseErrorChange = {},
                    entries = null,
                    js = null,
                    enabled = true,
                    onError = {},
                )
            }
        }
        // the first editable field is the matcher text input of the filter row
        onAllNodes(hasSetTextAction())[0].performTextInput("ka")
        val edited = assertIs<EntrySelector.TextFilterItem>(value.value.filters.single())
        assertEquals("ka", edited.matcherText)
    }

    @Test
    fun editingExpressionPropagates() = runComposeUiTest {
        // a boolean filter row has no text input, so the only editable field is the expression box
        val filter = EntrySelector.BooleanFilterItem("done", true)
        val value = mutableStateOf(EntrySelector(listOf(filter), "#1"))
        setContent {
            AppTheme {
                ParamEntrySelector(
                    labelerConf = TestLabelers.utauOto,
                    value = value.value,
                    onValueChange = { value.value = it },
                    isError = false,
                    onParseErrorChange = {},
                    entries = null,
                    js = null,
                    enabled = true,
                    onError = {},
                )
            }
        }
        onNode(hasSetTextAction()).performTextReplacement("not #1")
        assertEquals("not #1", value.value.rawExpression)
        assertTrue(value.value.filters.single() is EntrySelector.BooleanFilterItem)
    }
}
