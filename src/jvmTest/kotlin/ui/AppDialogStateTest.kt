package ui

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.Version
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.repository.update.model.Update
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.Screen
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.AskIfSaveDialogResult
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogRequest
import com.sdercolin.vlabeler.ui.dialog.EmbeddedDialogResult
import com.sdercolin.vlabeler.ui.dialog.ReloadLabelDialogArgs
import com.sdercolin.vlabeler.ui.dialog.customization.CustomizableItem
import com.sdercolin.vlabeler.ui.dialog.importentries.ImportEntriesDialogArgs
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesEditorState
import com.sdercolin.vlabeler.ui.string.toLocalized
import com.sdercolin.vlabeler.util.ParamMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import testutil.TestAppState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for the app-level dialog/glue logic implemented by `AppDialogStateImpl` and exercised through a real
 * [AppState] built with [TestAppState.create] (IPC disabled). Covers the open/close/queue of embedded and
 * standalone dialogs, dialog-stacking (opening a project/export/etc. dialog closes the others), the
 * [AppState.awaitEmbeddedDialog] request/result flow, and the [AppState.closeAllDialogs] reset.
 *
 * The app is built with an unconfined [mainScope] so that suspend `await...` flows run synchronously up to their
 * suspension point and can be resolved deterministically by invoking the captured result callback.
 */
class AppDialogStateTest {

    private lateinit var scope: CoroutineScope
    private lateinit var appState: AppState

    @BeforeTest
    fun setup() {
        Log.muted = true
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        appState = TestAppState.create(scope)
    }

    @AfterTest
    fun teardown() {
        scope.cancel()
        Log.muted = false
    }

    /** Invokes the star-projected result callback with an untyped result. */
    @Suppress("UNCHECKED_CAST")
    private fun EmbeddedDialogRequest<*>.resolve(result: EmbeddedDialogResult<*>?) {
        (onResult as (EmbeddedDialogResult<*>?) -> Unit)(result)
    }

    private fun macroPlugin(name: String) = Plugin(
        name = name,
        type = Plugin.Type.Macro,
        author = "tester",
        supportedLabelFileExtension = "*",
        scriptFiles = emptyList(),
    )

    /* region simple standalone dialog toggles */

    @Test
    fun `simple dialog toggles set and clear their flags`() {
        assertFalse(appState.isShowingProjectSettingDialog)
        appState.openProjectSettingDialog()
        assertTrue(appState.isShowingProjectSettingDialog)
        appState.closeProjectSettingDialog()
        assertFalse(appState.isShowingProjectSettingDialog)

        appState.openSampleListDialog()
        assertTrue(appState.isShowingSampleListDialog)
        appState.closeSampleListDialog()
        assertFalse(appState.isShowingSampleListDialog)

        appState.openSampleDirectoryRedirectDialog()
        assertTrue(appState.isShowingSampleDirectoryRedirectDialog)
        appState.closeSampleDirectoryRedirectDialog()
        assertFalse(appState.isShowingSampleDirectoryRedirectDialog)

        appState.openPrerenderDialog()
        assertTrue(appState.isShowingPrerenderDialog)
        appState.closePrerenderDialog()
        assertFalse(appState.isShowingPrerenderDialog)

        appState.openEntrySampleSyncDialog()
        assertTrue(appState.isShowingEntrySampleSyncDialog)
        appState.closeEntrySampleSyncDialog()
        assertFalse(appState.isShowingEntrySampleSyncDialog)

        appState.openAboutDialog()
        assertTrue(appState.isShowingAboutDialog)
        appState.closeAboutDialog()
        assertFalse(appState.isShowingAboutDialog)

        appState.openLicenseDialog()
        assertTrue(appState.isShowingLicenseDialog)
        appState.closeLicenseDialog()
        assertFalse(appState.isShowingLicenseDialog)

        appState.openQuickLaunchManagerDialog()
        assertTrue(appState.isShowingQuickLaunchManagerDialog)
        appState.closeQuickLaunchManagerDialog()
        assertFalse(appState.isShowingQuickLaunchManagerDialog)

        appState.openTrackingSettingsDialog()
        assertTrue(appState.isShowingTrackingSettingsDialog)
        appState.closeTrackingSettingsDialog()
        assertFalse(appState.isShowingTrackingSettingsDialog)

        appState.openFileNameNormalizerDialog()
        assertTrue(appState.isShowingFileNameNormalizerDialog)
        appState.closeFileNameNormalizerDialog()
        assertFalse(appState.isShowingFileNameNormalizerDialog)
    }

    /* endregion */

    /* region dialog stacking: opening one closes the previously opened ones */

    @Test
    fun `opening a project or export dialog closes the previously opened dialogs`() {
        appState.openProjectSettingDialog()
        assertTrue(appState.isShowingProjectSettingDialog)

        appState.openOpenProjectDialog()
        assertTrue(appState.isShowingOpenProjectDialog)
        // opening the open-project dialog closes the other closable dialogs first
        assertFalse(appState.isShowingProjectSettingDialog)

        appState.openSaveAsProjectDialog()
        assertTrue(appState.isShowingSaveAsProjectDialog)
        assertFalse(appState.isShowingOpenProjectDialog)

        appState.openExportDialog()
        assertTrue(appState.isShowingExportDialog)
        assertFalse(appState.isShowingSaveAsProjectDialog)

        appState.openImportDialog()
        assertTrue(appState.isShowingImportDialog)
        assertFalse(appState.isShowingExportDialog)

        appState.closeImportDialog()
        assertFalse(appState.isShowingImportDialog)
    }

    @Test
    fun `preferences dialog carries and clears its launch args`() {
        val args = PreferencesEditorState.LaunchArgs.Keymap("search")
        appState.openProjectSettingDialog()

        appState.openPreferencesDialog(args)
        assertTrue(appState.isShowingPreferencesDialog)
        assertSame(args, appState.preferencesDialogArgs)
        // opening preferences also closed the project setting dialog
        assertFalse(appState.isShowingProjectSettingDialog)

        appState.closePreferencesDialog()
        assertFalse(appState.isShowingPreferencesDialog)
        assertNull(appState.preferencesDialogArgs)
    }

    /* endregion */

    /* region dialogs holding args */

    @Test
    fun `updater dialog holds and clears the update content`() {
        val update = Update(Version(2, 0, 0), "2024-01-01", "https://example.com/asset", emptyList())
        appState.openUpdaterDialog(update)
        assertSame(update, appState.updaterDialogContent)
        appState.closeUpdaterDialog()
        assertNull(appState.updaterDialogContent)
    }

    @Test
    fun `import entries dialog holds and clears its args`() {
        val args = ImportEntriesDialogArgs(emptyList())
        appState.openImportEntriesDialog(args)
        assertSame(args, appState.importEntriesDialogArgs)
        appState.closeImportEntriesDialog()
        assertNull(appState.importEntriesDialogArgs)
    }

    @Test
    fun `reload label dialog holds and clears its args`() {
        val args = ReloadLabelDialogArgs(emptyList())
        appState.openReloadLabelDialog(args)
        assertSame(args, appState.reloadLabelDialogArgs)
        appState.closeReloadLabelDialog()
        assertNull(appState.reloadLabelDialogArgs)
    }

    @Test
    fun `customizable item manager dialog toggles the shown type and closes others`() {
        appState.openProjectSettingDialog()

        appState.openCustomizableItemManagerDialog(CustomizableItem.Type.Labeler)
        assertEquals(CustomizableItem.Type.Labeler, appState.customizableItemManagerTypeShownInDialog)
        // opening the manager closed the project setting dialog
        assertFalse(appState.isShowingProjectSettingDialog)

        appState.closeCustomizableItemManagerDialog()
        assertNull(appState.customizableItemManagerTypeShownInDialog)
    }

    @Test
    fun `macro plugin dialog from slot stores args, updates params, and closes`() {
        val plugin = macroPlugin("macro-1")
        val params = ParamMap(mapOf("a" to 1))
        appState.openMacroPluginDialogFromSlot(plugin, params, slot = 3)

        var args = assertNotNull(appState.macroPluginShownInDialog)
        assertSame(plugin, args.plugin)
        assertSame(params, args.paramMap)
        assertEquals(3, args.slot)

        val newParams = ParamMap(mapOf("a" to 2))
        appState.updateMacroPluginDialogInputParams(newParams)
        args = assertNotNull(appState.macroPluginShownInDialog)
        assertSame(newParams, args.paramMap)
        // the plugin and slot are preserved when only the params change
        assertSame(plugin, args.plugin)
        assertEquals(3, args.slot)

        appState.closeMacroPluginDialog()
        assertNull(appState.macroPluginShownInDialog)
    }

    @Test
    fun `macro plugin report is shown and dismissed`() {
        val report = "done".toLocalized()
        appState.showMacroPluginReport(report)
        assertSame(report, appState.macroPluginReport)
        appState.closeMacroPluginReport()
        assertNull(appState.macroPluginReport)
    }

    @Test
    fun `pending action after saved is stored and cleared`() {
        val action = AppState.PendingActionAfterSaved.Exit
        appState.putPendingActionAfterSaved(action)
        assertSame(action, appState.pendingActionAfterSaved)
        appState.clearPendingActionAfterSaved()
        assertNull(appState.pendingActionAfterSaved)
    }

    /* endregion */

    /* region embedded dialog request / result */

    @Test
    fun `openEmbeddedDialog exposes a request whose null result closes the dialog without dispatching`() {
        appState.openEmbeddedDialog(AskIfSaveDialogPurpose.IsCreatingNew)
        val request = assertNotNull(appState.embeddedDialog)
        assertIs<AskIfSaveDialogPurpose.IsCreatingNew>(request.args)

        request.resolve(null)
        assertNull(appState.embeddedDialog)
        // a cancelled dialog leaves the app on the starter screen (no action dispatched)
        assertIs<Screen.Starter>(appState.screen)
    }

    @Test
    fun `openEmbeddedDialog result is dispatched to handleDialogResult`() {
        appState.openEmbeddedDialog(AskIfSaveDialogPurpose.IsCreatingNew)
        val request = assertNotNull(appState.embeddedDialog)

        // declining to save with a "creating new" pending action opens the project creator
        request.resolve(AskIfSaveDialogResult(save = false, AppState.PendingActionAfterSaved.CreatingNew))

        assertNull(appState.embeddedDialog)
        assertIs<Screen.ProjectCreator>(appState.screen)
    }

    @Test
    fun `awaitEmbeddedDialog resolves with the callback result`() {
        var completed = false
        var result: EmbeddedDialogResult<*>? = null
        scope.launch {
            result = appState.awaitEmbeddedDialog(AskIfSaveDialogPurpose.IsExiting)
            completed = true
        }
        // the unconfined coroutine has run up to the dialog suspension point
        val request = assertNotNull(appState.embeddedDialog)
        assertFalse(completed)

        val expected = AskIfSaveDialogResult(save = true, AppState.PendingActionAfterSaved.Exit)
        request.resolve(expected)

        assertTrue(completed)
        assertSame(expected, result)
        assertNull(appState.embeddedDialog)
    }

    @Test
    fun `closeEmbeddedDialog cancels a pending awaitEmbeddedDialog`() {
        var thrown: Throwable? = null
        var result: EmbeddedDialogResult<*>? = null
        scope.launch {
            try {
                result = appState.awaitEmbeddedDialog(AskIfSaveDialogPurpose.IsExiting)
            } catch (e: CancellationException) {
                thrown = e
            }
        }
        assertNotNull(appState.embeddedDialog)

        appState.closeEmbeddedDialog()

        assertIs<CancellationException>(thrown)
        assertNull(result)
        assertNull(appState.embeddedDialog)
    }

    /* endregion */

    /* region close-all and any-dialog-opening */

    @Test
    fun `closeAllDialogs clears the dialogs it owns`() {
        appState.openProjectSettingDialog()
        appState.openSampleListDialog()
        appState.openSampleDirectoryRedirectDialog()
        appState.openQuickLaunchManagerDialog()
        appState.showMacroPluginReport("report".toLocalized())
        appState.openEmbeddedDialog(AskIfSaveDialogPurpose.IsExiting)
        assertTrue(appState.anyDialogOpening())

        appState.closeAllDialogs()

        assertFalse(appState.isShowingProjectSettingDialog)
        assertFalse(appState.isShowingSampleListDialog)
        assertFalse(appState.isShowingSampleDirectoryRedirectDialog)
        assertFalse(appState.isShowingQuickLaunchManagerDialog)
        assertNull(appState.macroPluginReport)
        assertNull(appState.embeddedDialog)
        assertFalse(appState.anyDialogOpening())
    }

    @Test
    fun `anyDialogOpening reflects open dialogs`() {
        assertFalse(appState.anyDialogOpening())
        assertFalse(appState.anyDialogOpeningExceptMacroPluginManager())

        appState.openAboutDialog()
        assertTrue(appState.anyDialogOpening())
        assertTrue(appState.anyDialogOpeningExceptMacroPluginManager())

        appState.closeAboutDialog()
        assertFalse(appState.anyDialogOpening())

        // NOTE: despite its name, anyDialogOpeningExceptMacroPluginManager() does NOT exclude the macro-plugin
        // customizable item manager: it returns true whenever customizableItemManagerTypeShownInDialog != null.
        appState.openCustomizableItemManagerDialog(CustomizableItem.Type.MacroPlugin)
        assertTrue(appState.anyDialogOpening())
        assertTrue(appState.anyDialogOpeningExceptMacroPluginManager())
    }

    /* endregion */
}
