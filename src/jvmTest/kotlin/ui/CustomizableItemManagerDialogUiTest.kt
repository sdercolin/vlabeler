package ui

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.customization.CustomizableItem
import com.sdercolin.vlabeler.ui.dialog.customization.CustomizableItemManagerDialog
import com.sdercolin.vlabeler.ui.dialog.customization.CustomizableItemManagerDialogState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.toLocalized
import com.sdercolin.vlabeler.ui.theme.AppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import testutil.TestEnv
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Compose UI tests for [CustomizableItemManagerDialog] (the labeler/plugin manager), mounted over a minimal concrete
 * [CustomizableItemManagerDialogState]. The state behavior is covered by [CustomizableItemManagerTest]; this exercises
 * the composable: the item list renders, clicking a row updates the selection, the enable/disable switch reflects and
 * flips the item, and the removability of the selected item follows its `canRemove`.
 *
 * The dialog only receives an [AppState] to build the default state; because the state is injected here, the
 * uninitialized instance (allocated without running the constructor, the pattern proven safe in
 * [CustomizableItemManagerTest]) is never dereferenced on the paths exercised. Actions that would touch it (`finish`,
 * `requestRemoveCurrentItem`, `openDirectory`) are not triggered.
 */
@OptIn(ExperimentalTestApi::class)
class CustomizableItemManagerDialogUiTest {

    private val tempDirs = mutableListOf<File>()

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
    }

    @AfterTest
    fun teardown() {
        tempDirs.forEach { it.deleteRecursively() }
        tempDirs.clear()
        Log.muted = false
    }

    private fun newTempDir(): File = createTempDirectory("vlabeler-test").toFile().also { tempDirs += it }

    private fun uninitializedAppState(): AppState {
        val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(AppState::class.java) as AppState
    }

    private fun cancelledScope(): CoroutineScope = CoroutineScope(Job().apply { cancel() })

    private inner class UiTestItem(
        name: String,
        canRemove: Boolean = true,
        disabled: Boolean = false,
    ) : CustomizableItem(
        name = name,
        author = "author",
        version = 1,
        displayedName = name.toLocalized(),
        description = "".toLocalized(),
        email = "",
        website = "",
        rootFile = newTempDir().resolve("$name.json").apply { writeText("{}") },
        canRemove = canRemove,
        disabled = disabled,
    ) {
        override fun canExecute(): Boolean = false
        override fun execute() = Unit
    }

    private inner class UiTestManagerState(
        appState: AppState,
    ) : CustomizableItemManagerDialogState<UiTestItem>(
        title = Strings.LabelerManagerTitle,
        importDialogTitle = Strings.LabelerManagerImportDialogTitle,
        definitionFileExtension = "json",
        directory = newTempDir(),
        allowExecution = false,
        appState = appState,
        appRecordStore = AppRecordStore(AppRecord(), cancelledScope()),
    ) {
        override fun reload() = Unit
        override fun saveDisabled(index: Int) = Unit
        override suspend fun importNewItem(configFile: File): String = configFile.nameWithoutExtension
    }

    private val appState: AppState by lazy { uninitializedAppState() }

    private fun createState(vararg items: UiTestItem): UiTestManagerState =
        UiTestManagerState(appState).apply { loadItems(items.toList()) }

    @Test
    fun rendersTitleAndItemList() = runComposeUiTest {
        val state = createState(UiTestItem("alpha"), UiTestItem("beta"))
        setContent {
            AppTheme {
                CustomizableItemManagerDialog(
                    type = CustomizableItem.Type.Labeler,
                    appState = appState,
                    state = state,
                )
            }
        }
        onNodeWithText("Labelers").assertExists()
        onNodeWithText("alpha").assertExists()
        onNodeWithText("beta").assertExists()
        // each of the two items shows its author caption
        assertEquals(2, onAllNodesWithText("author: author").fetchSemanticsNodes().size)
    }

    @Test
    fun selectingItemUpdatesSelection() = runComposeUiTest {
        val state = createState(UiTestItem("alpha"), UiTestItem("beta"))
        setContent {
            AppTheme {
                CustomizableItemManagerDialog(
                    type = CustomizableItem.Type.Labeler,
                    appState = appState,
                    state = state,
                )
            }
        }
        assertEquals(null, state.selectedIndex)
        onNodeWithText("beta").performClick()
        assertEquals(1, state.selectedIndex)
        assertEquals("beta", state.selectedItem?.name)
    }

    @Test
    fun disableSwitchReflectsAndTogglesItem() = runComposeUiTest {
        val state = createState(UiTestItem("alpha"), UiTestItem("beta"))
        setContent {
            AppTheme {
                CustomizableItemManagerDialog(
                    type = CustomizableItem.Type.Labeler,
                    appState = appState,
                    state = state,
                )
            }
        }
        // the switch is "on" when the item is enabled (not disabled)
        onAllNodes(isToggleable())[0].assertIsOn()
        onAllNodes(isToggleable())[0].performClick()
        assertTrue(state.items[0].disabled)
        onAllNodes(isToggleable())[0].assertIsOff()

        onAllNodes(isToggleable())[0].performClick()
        assertFalse(state.items[0].disabled)
        onAllNodes(isToggleable())[0].assertIsOn()
    }

    @Test
    fun removeAvailabilityFollowsCanRemove() = runComposeUiTest {
        val state = createState(UiTestItem("removable", canRemove = true), UiTestItem("fixed", canRemove = false))
        setContent {
            AppTheme {
                CustomizableItemManagerDialog(
                    type = CustomizableItem.Type.Labeler,
                    appState = appState,
                    state = state,
                )
            }
        }
        // no selection: nothing to remove
        assertFalse(state.canRemoveCurrentItem())

        onNodeWithText("removable").performClick()
        assertTrue(state.canRemoveCurrentItem())

        onNodeWithText("fixed").performClick()
        assertFalse(state.canRemoveCurrentItem())
    }
}
