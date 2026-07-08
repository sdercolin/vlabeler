package ui

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.action.KeyActionKeyBind
import com.sdercolin.vlabeler.model.key.Key
import com.sdercolin.vlabeler.model.key.KeySet
import com.sdercolin.vlabeler.repository.ColorPaletteRepository
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesEditorState
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesItem
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesPage
import com.sdercolin.vlabeler.ui.dialog.preferences.PreferencesPages
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.stringStatic
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import testutil.TestEnv
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [PreferencesEditorState].
 *
 * [PreferencesEditorState] only stores its [AppState] (it is dereferenced solely by [PreferencesItem.Button]
 * `onClick` handlers and by composables, none of which are exercised here), so the tests pass an uninitialized
 * [AppState] instance allocated without running its constructor, following the pattern proven safe in
 * [ProjectCreatorStateTest]. All file system access goes through the app directory redirected by `VLABELER_APP_DIR`
 * (repositories) or through temp directories (import/export), so no user data is ever touched.
 */
class PreferencesEditorStateTest {

    private val tempDirs = mutableListOf<File>()
    private val submitted = mutableListOf<AppConf?>()
    private val applied = mutableListOf<AppConf>()
    private val viewedPages = mutableListOf<PreferencesPage>()
    private val snackbars = mutableListOf<String>()

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
        // the state's init block loads the repositories; ensure the palette directory (inside the redirected app
        // directory) exists so that the preset palettes are available
        ColorPaletteRepository.directory.mkdirs()
    }

    @AfterTest
    fun teardown() {
        tempDirs.forEach { it.deleteRecursively() }
        tempDirs.clear()
        submitted.clear()
        applied.clear()
        viewedPages.clear()
        snackbars.clear()
        Log.muted = false
    }

    private fun newTempDir(): File = createTempDirectory("vlabeler-test").toFile().also { tempDirs += it }

    private fun uninitializedAppState(): AppState {
        val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(AppState::class.java) as AppState
    }

    private fun createState(
        initConf: AppConf = AppConf(),
        initialPage: PreferencesPage? = null,
        launchArgs: PreferencesEditorState.LaunchArgs? = null,
    ) = PreferencesEditorState(
        appState = uninitializedAppState(),
        initConf = initConf,
        submit = { submitted += it },
        apply = { applied += it },
        initialPage = initialPage,
        onViewPage = { viewedPages += it },
        showSnackbar = { snackbars += it },
        launchArgs = launchArgs,
    )

    private fun PreferencesEditorState.pageItem(model: PreferencesPage) = pages.first { it.model == model }

    private val canvasResolutionItem = PreferencesPages.ChartsCanvas.content
        .flatMap { it.items }
        .filterIsInstance<PreferencesItem.IntegerInput>()
        .first { it.title == Strings.PreferencesChartsCanvasResolutionDefault }

    private val autoSaveIntervalItem = PreferencesPages.AutoSave.content
        .flatMap { it.items }
        .filterIsInstance<PreferencesItem.IntegerInput>()
        .first { it.title == Strings.PreferencesAutoSaveIntervalSec }

    private val historyMaxSizeItem = PreferencesPages.History.content
        .flatMap { it.items }
        .filterIsInstance<PreferencesItem.IntegerInput>()
        .first { it.title == Strings.PreferencesHistoryMaxSize }

    @Suppress("UNCHECKED_CAST")
    private val keyActionKeymapItem = PreferencesPages.KeymapKeyAction.content
        .flatMap { it.items }
        .filterIsInstance<PreferencesItem.Keymap<*>>()
        .single() as PreferencesItem.Keymap<KeyAction>

    private val defaultKeyBinds = KeyAction.entries.map { KeyActionKeyBind(it, it.defaultKeySet) }

    /* region initialization */

    @Test
    fun `initial pages are the collapsed root pages and the first one is selected`() {
        val state = createState()
        assertEquals(PreferencesPages.rootPages.toList(), state.pages.map { it.model })
        assertTrue(state.pages.all { it.level == 0 })
        assertTrue(state.pages.none { it.isExpanded })
        assertEquals(state.pages.first(), state.selectedPage)
        assertFalse(state.needSave)
        assertFalse(state.isLaunchArgsHandled)
        assertNull(state.launchArgs)
    }

    @Test
    fun `initial page expands its ancestors and gets selected`() {
        val state = createState(initialPage = PreferencesPages.ChartsSpectrogram)
        val charts = state.pageItem(PreferencesPages.Charts)
        assertTrue(charts.isExpanded)
        val chartsIndex = state.pages.indexOf(charts)
        assertEquals(
            PreferencesPages.Charts.children,
            state.pages.subList(chartsIndex + 1, chartsIndex + 1 + PreferencesPages.Charts.children.size)
                .map { it.model },
        )
        assertTrue(state.pages.first { it.model == PreferencesPages.ChartsSpectrogram }.level == 1)
        assertEquals(PreferencesPages.ChartsSpectrogram, state.selectedPage.model)
        // other root pages stay collapsed
        assertFalse(state.pageItem(PreferencesPages.Editor).isExpanded)
    }

    @Test
    fun `initial root page is selected without expansion`() {
        val state = createState(initialPage = PreferencesPages.Playback)
        assertEquals(PreferencesPages.Playback, state.selectedPage.model)
        assertTrue(state.pages.none { it.isExpanded })
    }

    @Test
    fun `launch args take precedence over the initial page`() {
        val state = createState(
            initialPage = PreferencesPages.View,
            launchArgs = PreferencesEditorState.LaunchArgs.Keymap("search text"),
        )
        assertEquals(PreferencesPages.KeymapKeyAction, state.selectedPage.model)
        assertTrue(state.pageItem(PreferencesPages.Keymap).isExpanded)
        assertEquals("search text", (state.launchArgs as PreferencesEditorState.LaunchArgs.Keymap).searchText)
        assertFalse(state.isLaunchArgsHandled)
    }

    @Test
    fun `init fixes a missing color palette to the default`() {
        val badConf = AppConf().let {
            it.copy(
                painter = it.painter.copy(
                    spectrogram = it.painter.spectrogram.copy(colorPalette = "__not-existing-palette__"),
                ),
            )
        }
        val state = createState(initConf = badConf)
        assertEquals(AppConf.Spectrogram.DEFAULT_COLOR_PALETTE, state.conf.painter.spectrogram.colorPalette)
        // the saved conf keeps the original value, so the fix is pending as an unsaved change
        assertEquals("__not-existing-palette__", state.savedConf.painter.spectrogram.colorPalette)
        assertTrue(state.needSave)
    }

    @Test
    fun `init fixes a missing font family to the default`() {
        val badConf = AppConf().let {
            it.copy(view = it.view.copy(fontFamilyName = "__not-existing-font__"))
        }
        val state = createState(initConf = badConf)
        assertEquals(AppConf.View.DEFAULT_FONT_FAMILY_NAME, state.conf.view.fontFamilyName)
        assertTrue(state.needSave)
    }

    @Test
    fun `init keeps a valid conf untouched`() {
        val state = createState()
        assertEquals(AppConf(), state.conf)
        assertEquals(AppConf(), state.savedConf)
        assertFalse(state.needSave)
    }

    /* endregion */

    /* region page navigation */

    @Test
    fun `toggle page expands and collapses its children`() {
        val state = createState()
        val charts = state.pageItem(PreferencesPages.Charts)

        state.togglePage(charts)
        assertTrue(charts.isExpanded)
        val chartsIndex = state.pages.indexOf(charts)
        val childItems = state.pages.subList(chartsIndex + 1, chartsIndex + 1 + PreferencesPages.Charts.children.size)
        assertEquals(PreferencesPages.Charts.children, childItems.map { it.model })
        assertTrue(childItems.all { it.level == 1 })

        state.togglePage(charts)
        assertFalse(charts.isExpanded)
        assertEquals(PreferencesPages.rootPages.toList(), state.pages.map { it.model })
    }

    @Test
    fun `collapsing the page containing the selection moves the selection to the parent`() {
        val state = createState()
        val charts = state.pageItem(PreferencesPages.Charts)
        state.togglePage(charts)
        state.selectPage(state.pageItem(PreferencesPages.ChartsWaveform))
        assertEquals(PreferencesPages.ChartsWaveform, state.selectedPage.model)

        state.togglePage(charts)

        assertEquals(PreferencesPages.Charts, state.selectedPage.model)
        assertEquals(listOf(PreferencesPages.ChartsWaveform, PreferencesPages.Charts), viewedPages)
    }

    @Test
    fun `collapsing a page keeps an unrelated selection`() {
        val state = createState()
        val charts = state.pageItem(PreferencesPages.Charts)
        state.togglePage(charts)
        state.selectPage(state.pageItem(PreferencesPages.View))

        state.togglePage(charts)

        assertEquals(PreferencesPages.View, state.selectedPage.model)
    }

    @Test
    fun `select page reports the viewed page`() {
        val state = createState()
        state.selectPage(state.pageItem(PreferencesPages.Playback))
        assertEquals(PreferencesPages.Playback, state.selectedPage.model)
        assertEquals(listOf<PreferencesPage>(PreferencesPages.Playback), viewedPages)
    }

    @Test
    fun `select page by link expands the selected parent when the target is not listed`() {
        val state = createState()
        state.selectPage(state.pageItem(PreferencesPages.Editor))

        state.selectPageByLink(PreferencesPages.EditorScissors)

        assertTrue(state.pageItem(PreferencesPages.Editor).isExpanded)
        assertEquals(PreferencesPages.EditorScissors, state.selectedPage.model)
    }

    @Test
    fun `select page by link uses the existing page item when listed`() {
        val state = createState()
        val pageCount = state.pages.size
        state.selectPageByLink(PreferencesPages.View)
        assertEquals(PreferencesPages.View, state.selectedPage.model)
        assertEquals(pageCount, state.pages.size)
    }

    /* endregion */

    /* region editing, saving and resetting */

    @Test
    fun `update rewrites the working conf but not the saved conf`() {
        val state = createState()
        val newValue = AppConf.CanvasResolution.DEFAULT_DEFAULT + 25

        state.update(canvasResolutionItem, newValue)

        assertEquals(newValue, state.conf.painter.canvasResolution.default)
        assertEquals(AppConf(), state.savedConf)
        assertTrue(state.needSave)
        assertTrue(applied.isEmpty())
    }

    @Test
    fun `save applies the working conf`() {
        val state = createState()
        val newValue = AppConf.CanvasResolution.DEFAULT_DEFAULT + 25
        state.update(canvasResolutionItem, newValue)

        state.save()

        assertEquals(listOf(state.conf), applied)
        assertEquals(state.conf, state.savedConf)
        assertFalse(state.needSave)
    }

    @Test
    fun `finish positively with pending changes submits the conf`() {
        val state = createState()
        state.update(canvasResolutionItem, AppConf.CanvasResolution.DEFAULT_DEFAULT + 25)
        state.finish(positive = true)
        assertEquals(listOf<AppConf?>(state.conf), submitted)
    }

    @Test
    fun `finish positively without pending changes submits null`() {
        val state = createState()
        state.finish(positive = true)
        assertEquals(listOf<AppConf?>(null), submitted)
    }

    @Test
    fun `finish negatively discards pending changes`() {
        val state = createState()
        state.update(canvasResolutionItem, AppConf.CanvasResolution.DEFAULT_DEFAULT + 25)
        state.finish(positive = false)
        assertEquals(listOf<AppConf?>(null), submitted)
    }

    @Test
    fun `reset page only resets the items of the selected page`() {
        val state = createState()
        state.update(autoSaveIntervalItem, AppConf.AutoSave.DEFAULT_INTERVAL_SEC + 100)
        state.update(historyMaxSizeItem, AppConf.History.DEFAULT_MAX_SIZE + 100)
        state.selectPage(state.pageItem(PreferencesPages.AutoSave))

        state.resetPage()

        assertEquals(AppConf.AutoSave.DEFAULT_INTERVAL_SEC, state.conf.autoSave.intervalSec)
        assertEquals(AppConf.History.DEFAULT_MAX_SIZE + 100, state.conf.history.maxSize)
    }

    @Test
    fun `reset all restores the default conf`() {
        val state = createState()
        state.update(autoSaveIntervalItem, AppConf.AutoSave.DEFAULT_INTERVAL_SEC + 100)
        state.update(historyMaxSizeItem, AppConf.History.DEFAULT_MAX_SIZE + 100)

        state.resetAll()

        assertEquals(AppConf(), state.conf)
        assertFalse(state.needSave)
    }

    /* endregion */

    /* region keymap editing */

    @Test
    fun `open keymap item edit dialog exposes the dialog args`() {
        val state = createState()
        val keyBind = defaultKeyBinds.first { it.action == KeyAction.NewProject }

        state.openKeymapItemEditDialog(keyBind, keyActionKeymapItem, defaultKeyBinds)

        val args = assertNotNull(state.keymapItemEditDialogArgs)
        assertEquals(keyBind, args.actionKeyBind)
        assertEquals(keyActionKeymapItem, args.keymapItem)
        assertEquals(defaultKeyBinds, args.allKeyBinds)
        assertNull(state.keymapItemEditConflictDialogArgs)
    }

    @Suppress("UNCHECKED_CAST")
    private fun PreferencesEditorState.submitKeymapEdit(edited: KeyActionKeyBind?) {
        val args = assertNotNull(keymapItemEditDialogArgs) as PreferencesEditorState.KeymapItemEditDialogArgs<KeyAction>
        args.submit(edited)
    }

    @Test
    fun `submitting null closes the dialog without changes`() {
        val state = createState()
        val keyBind = defaultKeyBinds.first { it.action == KeyAction.NewProject }
        state.openKeymapItemEditDialog(keyBind, keyActionKeymapItem, defaultKeyBinds)

        state.submitKeymapEdit(null)

        assertNull(state.keymapItemEditDialogArgs)
        assertEquals(AppConf(), state.conf)
    }

    @Test
    fun `submitting a non-conflicting key bind stores it in the keymap`() {
        val state = createState()
        val keyBind = defaultKeyBinds.first { it.action == KeyAction.NewProject }
        state.openKeymapItemEditDialog(keyBind, keyActionKeymapItem, defaultKeyBinds)
        val newKeySet = KeySet(Key.J, setOf(Key.Ctrl, Key.Alt, Key.Shift))

        state.submitKeymapEdit(KeyActionKeyBind(KeyAction.NewProject, newKeySet))

        assertNull(state.keymapItemEditDialogArgs)
        assertNull(state.keymapItemEditConflictDialogArgs)
        assertEquals(mapOf(KeyAction.NewProject to newKeySet), state.conf.keymaps.keyActionMap)
        assertTrue(state.needSave)
    }

    @Test
    fun `submitting the default key bind removes the custom entry`() {
        val state = createState()
        val customKeySet = KeySet(Key.J, setOf(Key.Ctrl, Key.Alt, Key.Shift))
        state.update(keyActionKeymapItem, listOf(KeyActionKeyBind(KeyAction.NewProject, customKeySet)))
        val keyBind = KeyActionKeyBind(KeyAction.NewProject, customKeySet)
        state.openKeymapItemEditDialog(keyBind, keyActionKeymapItem, defaultKeyBinds)

        state.submitKeymapEdit(KeyActionKeyBind(KeyAction.NewProject, KeyAction.NewProject.defaultKeySet))

        assertEquals(emptyMap(), state.conf.keymaps.keyActionMap)
    }

    @Test
    fun `submitting a conflicting key bind opens the conflict dialog without applying`() {
        val state = createState()
        val keyBind = defaultKeyBinds.first { it.action == KeyAction.NewProject }
        state.openKeymapItemEditDialog(keyBind, keyActionKeymapItem, defaultKeyBinds)
        val conflictingKeySet = assertNotNull(KeyAction.OpenProject.defaultKeySet)

        state.submitKeymapEdit(KeyActionKeyBind(KeyAction.NewProject, conflictingKeySet))

        assertNull(state.keymapItemEditDialogArgs)
        val conflictArgs = assertNotNull(state.keymapItemEditConflictDialogArgs)
        assertEquals(KeyActionKeyBind(KeyAction.NewProject, conflictingKeySet), conflictArgs.editedKeyBind)
        assertEquals(
            listOf<Any>(KeyActionKeyBind(KeyAction.OpenProject, conflictingKeySet)),
            conflictArgs.conflictingKeyBinds,
        )
        // nothing is applied until the conflict is resolved
        assertEquals(emptyMap(), state.conf.keymaps.keyActionMap)
    }

    @Test
    fun `cancelling the conflict dialog keeps the conf unchanged`() {
        val state = createState()
        val keyBind = defaultKeyBinds.first { it.action == KeyAction.NewProject }
        state.openKeymapItemEditDialog(keyBind, keyActionKeymapItem, defaultKeyBinds)
        val conflictingKeySet = assertNotNull(KeyAction.OpenProject.defaultKeySet)
        state.submitKeymapEdit(KeyActionKeyBind(KeyAction.NewProject, conflictingKeySet))

        assertNotNull(state.keymapItemEditConflictDialogArgs).cancel()

        assertNull(state.keymapItemEditConflictDialogArgs)
        assertEquals(AppConf(), state.conf)
    }

    @Test
    fun `keeping the conflict applies the edited key bind and keeps the conflicting one`() {
        val state = createState()
        val keyBind = defaultKeyBinds.first { it.action == KeyAction.NewProject }
        state.openKeymapItemEditDialog(keyBind, keyActionKeymapItem, defaultKeyBinds)
        val conflictingKeySet = assertNotNull(KeyAction.OpenProject.defaultKeySet)
        state.submitKeymapEdit(KeyActionKeyBind(KeyAction.NewProject, conflictingKeySet))

        assertNotNull(state.keymapItemEditConflictDialogArgs).keep()

        assertNull(state.keymapItemEditConflictDialogArgs)
        assertEquals(mapOf(KeyAction.NewProject to conflictingKeySet), state.conf.keymaps.keyActionMap)
    }

    @Test
    fun `removing the conflict unbinds the conflicting key binds`() {
        val state = createState()
        val keyBind = defaultKeyBinds.first { it.action == KeyAction.NewProject }
        state.openKeymapItemEditDialog(keyBind, keyActionKeymapItem, defaultKeyBinds)
        val conflictingKeySet = assertNotNull(KeyAction.OpenProject.defaultKeySet)
        state.submitKeymapEdit(KeyActionKeyBind(KeyAction.NewProject, conflictingKeySet))

        assertNotNull(state.keymapItemEditConflictDialogArgs).remove()

        assertNull(state.keymapItemEditConflictDialogArgs)
        assertEquals(
            mapOf(KeyAction.NewProject to conflictingKeySet, KeyAction.OpenProject to null),
            state.conf.keymaps.keyActionMap,
        )
    }

    /* endregion */

    /* region import and export */

    @Test
    fun `export writes the current conf as json`() {
        val state = createState()
        state.update(canvasResolutionItem, AppConf.CanvasResolution.DEFAULT_DEFAULT + 25)
        val dir = newTempDir()
        state.currentFilePicker = PreferencesEditorState.FilePicker.Export

        state.handleFilePickerResult(PreferencesEditorState.FilePicker.Export, dir.absolutePath, "exported.json")

        assertNull(state.currentFilePicker)
        val written = dir.resolve("exported.json").readText().parseJson<AppConf>()
        assertEquals(state.conf, written)
        assertEquals(listOf(stringStatic(Strings.PreferencesEditorExportSuccess)), snackbars)
    }

    @Test
    fun `export failure shows the failure snackbar`() {
        val state = createState()
        val missingDir = newTempDir().resolve("not-existing")

        state.handleFilePickerResult(PreferencesEditorState.FilePicker.Export, missingDir.absolutePath, "out.json")

        assertEquals(listOf(stringStatic(Strings.PreferencesEditorExportFailure)), snackbars)
    }

    @Test
    fun `import replaces the working conf`() {
        val state = createState()
        val imported = AppConf().let {
            it.copy(history = it.history.copy(maxSize = AppConf.History.DEFAULT_MAX_SIZE + 42))
        }
        val file = newTempDir().resolve("in.json").apply { writeText(imported.stringifyJson()) }

        state.handleFilePickerResult(PreferencesEditorState.FilePicker.Import, file.parent, file.name)

        assertNull(state.currentFilePicker)
        assertEquals(imported, state.conf)
        assertTrue(state.needSave)
        assertEquals(listOf(stringStatic(Strings.PreferencesEditorImportSuccess)), snackbars)
    }

    @Test
    fun `import of an invalid file keeps the conf and shows the failure snackbar`() {
        val state = createState()
        val file = newTempDir().resolve("in.json").apply { writeText("this is not json") }

        state.handleFilePickerResult(PreferencesEditorState.FilePicker.Import, file.parent, file.name)

        assertEquals(AppConf(), state.conf)
        assertEquals(listOf(stringStatic(Strings.PreferencesEditorImportFailure)), snackbars)
    }

    @Test
    fun `a cancelled file picker only closes itself`() {
        val state = createState()
        state.currentFilePicker = PreferencesEditorState.FilePicker.Import

        state.handleFilePickerResult(PreferencesEditorState.FilePicker.Import, null, null)

        assertNull(state.currentFilePicker)
        assertEquals(AppConf(), state.conf)
        assertTrue(snackbars.isEmpty())
    }

    @Test
    fun `file picker definitions`() {
        assertFalse(PreferencesEditorState.FilePicker.Import.writeMode)
        assertNull(PreferencesEditorState.FilePicker.Import.initialFileName)
        assertEquals(listOf("json"), PreferencesEditorState.FilePicker.Import.extensions)
        assertTrue(PreferencesEditorState.FilePicker.Export.writeMode)
        assertEquals("vLabeler.conf.json", PreferencesEditorState.FilePicker.Export.initialFileName)
        assertEquals(listOf("json"), PreferencesEditorState.FilePicker.Export.extensions)
    }

    /* endregion */
}
