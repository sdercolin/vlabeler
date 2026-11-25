package ui

import androidx.compose.foundation.ScrollState
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.sdercolin.vlabeler.audio.PlayerState
import com.sdercolin.vlabeler.env.KeyboardViewModel
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.filter.EntryFilter
import com.sdercolin.vlabeler.repository.ChartRepository
import com.sdercolin.vlabeler.repository.SampleInfoRepository
import com.sdercolin.vlabeler.ui.AppDialogState
import com.sdercolin.vlabeler.ui.AppDialogStateImpl
import com.sdercolin.vlabeler.ui.AppErrorState
import com.sdercolin.vlabeler.ui.AppErrorStateImpl
import com.sdercolin.vlabeler.ui.AppProgressState
import com.sdercolin.vlabeler.ui.AppProgressStateImpl
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppScreenState
import com.sdercolin.vlabeler.ui.AppScreenStateImpl
import com.sdercolin.vlabeler.ui.AppSnackbarState
import com.sdercolin.vlabeler.ui.AppSnackbarStateImpl
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.AppUnsavedChangesState
import com.sdercolin.vlabeler.ui.AppUnsavedChangesStateImpl
import com.sdercolin.vlabeler.ui.AppViewState
import com.sdercolin.vlabeler.ui.AppViewStateImpl
import com.sdercolin.vlabeler.ui.ProjectStore
import com.sdercolin.vlabeler.ui.ProjectStoreImpl
import com.sdercolin.vlabeler.ui.Screen
import com.sdercolin.vlabeler.ui.dialog.CommonConfirmationDialogAction
import com.sdercolin.vlabeler.ui.dialog.EditEntriesTagDialogArgs
import com.sdercolin.vlabeler.ui.dialog.EditExtraDialogArgs
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogArgs
import com.sdercolin.vlabeler.ui.dialog.InputEntryNameDialogPurpose
import com.sdercolin.vlabeler.ui.dialog.MoveEntryDialogArgs
import com.sdercolin.vlabeler.ui.dialog.SetEntryPropertyDialogArgs
import com.sdercolin.vlabeler.ui.editor.ChartStore
import com.sdercolin.vlabeler.ui.editor.Edition
import com.sdercolin.vlabeler.ui.editor.EditorEntryContextAction
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import com.sdercolin.vlabeler.ui.editor.Tool
import com.sdercolin.vlabeler.ui.editor.labeler.CanvasState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import testutil.TestEnv
import testutil.TestFixtures
import testutil.TestLabelers
import testutil.createTestProject
import java.io.File
import kotlin.io.path.createTempDirectory
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
 * Tests for [EditorState].
 *
 * [EditorState] requires an [AppState], which cannot be constructed in tests because its real construction starts
 * IPC/audio/tracking side effects. Instead, an [AppState] instance is allocated without running its constructor
 * (the same approach as in [ProjectCreatorStateTest]) and the members that [EditorState] and its exercised
 * collaborators dereference are injected reflectively with real implementations:
 *
 * - `appConf` (a [MutableState] delegate), `keyboardViewModel`, `scrollFitViewModel`, `mainScope`, `appRecordStore`
 *   and `playerState` (safe to construct: no audio line is opened until playback);
 * - the interface delegates ([AppErrorState], [AppViewState], [AppScreenState], [ProjectStore],
 *   [AppUnsavedChangesState], [AppSnackbarState], [AppDialogState], [AppProgressState]) with their real
 *   implementations, so project edits, dialogs and errors behave like in the application.
 *
 * The [AppRecordStore] uses an already-cancelled scope so nothing is written to the real application directory
 * (asserted in [ProjectCreatorStateTest]). The `mainScope` is an unconfined child scope of the test scope so that
 * coroutines launched by [AppState] functions run deterministically up to their first suspension point.
 */
class EditorStateTest {

    private lateinit var scope: CoroutineScope
    private lateinit var tempDir: File
    private lateinit var env: AppEnv
    private val loadedProjects = mutableSetOf<Project>()

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        tempDir = createTempDirectory("vlabeler-test").toFile()
    }

    @AfterTest
    fun teardown() {
        scope.cancel()
        loadedProjects.forEach {
            ChartRepository.clear(it)
            SampleInfoRepository.clear(it)
        }
        loadedProjects.clear()
        tempDir.deleteRecursively()
        Log.muted = false
    }

    private fun uninitializedAppState(): AppState {
        val unsafeField = sun.misc.Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as sun.misc.Unsafe
        return unsafe.allocateInstance(AppState::class.java) as AppState
    }

    private fun AppState.setField(name: String, value: Any) {
        AppState::class.java.getDeclaredField(name).apply {
            isAccessible = true
            set(this@setField, value)
        }
    }

    private fun AppState.setDelegate(interfaceClass: Class<*>, value: Any) {
        val field = AppState::class.java.declaredFields.single {
            it.name.startsWith("\$\$delegate") && it.type == interfaceClass
        }
        field.isAccessible = true
        field.set(this, value)
    }

    private fun cancelledScope(): CoroutineScope = CoroutineScope(Job().apply { cancel() })

    private inner class AppEnv(appConf: AppConf) {
        val appConfState: MutableState<AppConf> = mutableStateOf(appConf)
        val errorState = AppErrorStateImpl()
        val progressState = AppProgressStateImpl()
        val screenState = AppScreenStateImpl()
        val scrollFitViewModel = ScrollFitViewModel(scope)
        val projectStore = ProjectStoreImpl(
            scope = scope,
            appConf = appConfState,
            screenState = screenState,
            scrollFitViewModel = scrollFitViewModel,
            errorState = errorState,
            progressState = progressState,
        )
        val unsavedChangesState = AppUnsavedChangesStateImpl()
        val snackbarState = AppSnackbarStateImpl(SnackbarHostState())
        val dialogState = AppDialogStateImpl(unsavedChangesState, projectStore, snackbarState)
        val appRecordStore = AppRecordStore(AppRecord(), cancelledScope())
        val mainScope = CoroutineScope(SupervisorJob(scope.coroutineContext[Job]) + Dispatchers.Unconfined)

        val appState: AppState = uninitializedAppState().also { state ->
            state.setField("mainScope", mainScope)
            state.setField("keyboardViewModel", KeyboardViewModel(scope, appConf.keymaps))
            state.setField("scrollFitViewModel", scrollFitViewModel)
            state.setField("appRecordStore", appRecordStore)
            state.setField("appConf\$delegate", appConfState)
            state.setField("playerState", PlayerState(appConf, scope))
            state.setDelegate(AppErrorState::class.java, errorState)
            state.setDelegate(AppViewState::class.java, AppViewStateImpl(appRecordStore))
            state.setDelegate(AppScreenState::class.java, screenState)
            state.setDelegate(ProjectStore::class.java, projectStore)
            state.setDelegate(AppUnsavedChangesState::class.java, unsavedChangesState)
            state.setDelegate(AppSnackbarState::class.java, snackbarState)
            state.setDelegate(AppDialogState::class.java, dialogState)
            state.setDelegate(AppProgressState::class.java, progressState)
            dialogState.initDialogState(state)
            projectStore.initProjectStore(state)
        }

        val dialogArgs get() = dialogState.embeddedDialog?.args
    }

    /**
     * Creates an [EditorState] wired to a real [ProjectStoreImpl] behind the reflective [AppState], mirroring the
     * production wiring: the editor is registered as the current screen, so project edits are propagated back into
     * the editor via [EditorState.updateProject].
     */
    private fun createEditor(
        project: Project,
        appConf: AppConf = AppConf(),
        setup: (ProjectStoreImpl.() -> Unit)? = null,
    ): EditorState {
        env = AppEnv(appConf)
        env.projectStore.newProject(project)
        setup?.invoke(env.projectStore)
        val editor = EditorState(env.projectStore.requireProject(), env.appState)
        env.screenState.screen = Screen.Editor(editor)
        return editor
    }

    /**
     * Loads the current sample of the editor's project through the real loading path.
     */
    private fun loadSample(editor: EditorState, appConf: AppConf) {
        val project = editor.project
        SampleInfoRepository.init(project)
        loadedProjects += project
        runBlocking { editor.loadSample(appConf) }
        assertIs<CanvasState.Loaded>(editor.canvasState)
    }

    /**
     * An [AppConf] whose scissors actions never play audio and whose post-edit actions are disabled, so that
     * committed entries are exactly the submitted ones.
     */
    private fun plainConf(
        useOnScreenScissors: Boolean = true,
        askForName: AppConf.ScissorsActions.Target = AppConf.ScissorsActions.Target.Former,
    ) = AppConf(
        editor = AppConf.Editor(
            scissorsActions = AppConf.ScissorsActions(
                askForName = askForName,
                play = AppConf.ScissorsActions.Target.None,
            ),
            useOnScreenScissors = useOnScreenScissors,
            postEditDone = AppConf.PostEditAction.DEFAULT_DONE.copy(enabled = false),
        ),
    )

    /* region initial state and project updates */

    @Test
    fun `initial state is derived from the project and the app conf`() {
        val editor = createEditor(otoProject)
        assertEquals(CanvasState.Loading, editor.canvasState)
        assertTrue(editor.isLoading)
        assertFalse(editor.isError)
        assertEquals(Tool.Cursor, editor.tool)
        assertEquals(AppConf().painter.canvasResolution.default, editor.canvasResolution)
        assertEquals(0 to 0, editor.renderProgress)
        assertEquals(0, editor.entryLoadedCount)
        assertNull(editor.getSampleInfo())
        // single edit mode: only the current entry is loaded for editing
        assertEquals(editor.project.getEntriesForEditing().second, editor.editedEntries)
        assertEquals(listOf(0), editor.editedEntries.map { it.index })
        assertEquals("- a", editor.entryTitle)
        assertFalse(editor.entryStar)
        assertFalse(editor.entryDone)
        assertEquals("", editor.entryTag)
        assertEquals(emptyList<String>(), editor.tagOptions)
        assertFalse(editor.canUseOnScreenScissors)
        // the screen range is unknown until the scroll state has been measured
        assertNull(editor.getScreenRange(1000f, ScrollState(0)))
    }

    @Test
    fun `entry note getters and tag options follow project edits`() {
        val editor = createEditor(otoProject)
        editor.editEntryTag(0, "zebra")
        editor.editEntryTag(1, "apple")
        editor.toggleEntryStar(0)
        editor.toggleEntryDone(0)

        assertEquals("zebra", editor.entryTag)
        assertTrue(editor.entryStar)
        assertTrue(editor.entryDone)
        assertEquals(listOf("apple", "zebra"), editor.tagOptions)

        editor.toggleEntryDone(0)
        assertFalse(editor.entryDone)
    }

    @Test
    fun `updateProject reloads the edited entries only when the editing target changes`() {
        val editor = createEditor(otoProject)
        assertEquals(0, editor.entryLoadedCount)
        val initialEntries = editor.editedEntries

        // an edit that does not touch the current editing target does not reload
        editor.updateProject(editor.project.updateCurrentModule { editEntryTag(1, "other") })
        assertEquals(0, editor.entryLoadedCount)
        assertEquals(initialEntries, editor.editedEntries)

        // switching the current entry reloads the edited entries
        editor.updateProject(editor.project.updateCurrentModule { copy(currentIndex = 1) })
        assertEquals(1, editor.entryLoadedCount)
        assertEquals(listOf(1), editor.editedEntries.map { it.index })
        assertEquals("a ka", editor.entryTitle)
    }

    /* endregion */

    /* region editions */

    @Test
    fun `updateEntries stages editions and submitEntries commits them`() {
        val editor = createEditor(otoProject, appConf = plainConf())
        val original = editor.editedEntries.single()
        val edited = original.entry.copy(end = original.entry.end + 5f)

        editor.updateEntries(listOf(Edition(0, edited, listOf("end"), Edition.Method.Dragging)))
        assertEquals(edited, editor.editedEntries.single().entry)
        // the edition is not committed to the project yet
        assertEquals(original.entry, env.projectStore.requireProject().currentModule.entries[0])

        editor.submitEntries()
        assertEquals(edited, env.projectStore.requireProject().currentModule.entries[0])
        assertEquals(edited, editor.project.currentModule.entries[0])
        // the already staged entries match the committed project, so no reload happens
        assertEquals(0, editor.entryLoadedCount)

        // a second submit without any change is a no-op
        val committedProject = env.projectStore.requireProject()
        editor.submitEntries()
        assertSame(committedProject, env.projectStore.requireProject())
    }

    @Test
    fun `submitEntries applies the default post-edit done action`() {
        val editor = createEditor(otoProject)
        val original = editor.editedEntries.single()
        val edited = original.entry.copy(end = original.entry.end + 5f)

        editor.submitEntries(listOf(Edition(0, edited, listOf("end"), Edition.Method.Dragging)))

        val committed = editor.project.currentModule.entries[0]
        assertEquals(edited.end, committed.end)
        assertTrue(committed.notes.done)
        // the edited entries are reloaded with the committed done flag
        assertEquals(committed, editor.editedEntries.single().entry)
    }

    @Test
    fun `editions of entries not loaded for editing are not staged and are discarded on submit`() {
        val editor = createEditor(otoProject, appConf = plainConf())
        val otherEntry = editor.project.currentModule.entries[1]

        editor.updateEntries(
            listOf(Edition(1, otherEntry.copy(end = otherEntry.end + 5f), listOf("end"), Edition.Method.Dragging)),
        )
        // single edit mode only loads the current entry, so the edited list does not grow
        assertEquals(listOf(0), editor.editedEntries.map { it.index })

        val before = env.projectStore.requireProject()
        editor.submitEntries()
        assertSame(before, env.projectStore.requireProject())
    }

    @Test
    fun `updateEntries syncs the neighbor boundaries for a continuous labeler`() {
        val editor = createEditor(nnsvsProject, appConf = plainConf()) {
            jumpToModuleByNameAndEntry("doremi", 2)
        }
        assertTrue(editor.project.multipleEditMode)
        assertEquals(listOf(0, 1, 2, 3, 4, 5), editor.editedEntries.map { it.index })

        val target = editor.editedEntries[2].entry
        val edited = target.copy(end = target.end + 50f)
        editor.updateEntries(listOf(Edition(2, edited, listOf("end"), Edition.Method.Dragging)))

        assertEquals(edited, editor.editedEntries[2].entry)
        // the next entry's start is aligned to the edited end
        assertEquals(edited.end, editor.editedEntries[3].entry.start)
        // the previous entry's end is aligned to the (unchanged) edited start
        assertEquals(target.start, editor.editedEntries[1].entry.end)

        editor.submitEntries()
        val module = editor.project.currentModule
        assertEquals(edited.end, module.entries[2].end)
        assertEquals(edited.end, module.entries[3].start)
    }

    @Test
    fun `submitEntriesWithCascade commits the editions of other modules`() {
        val editor = createEditor(utauSingerProject, appConf = plainConf()) {
            jumpToModuleByNameAndEntry("C4", 0)
        }
        val current = editor.editedEntries.single().entry
        val a3Entry = editor.project.modules.first { it.name == "A3" }.entries[0]

        editor.updateEntries(
            listOf(Edition(0, current.copy(end = current.end + 5f), listOf("end"), Edition.Method.Dragging)),
        )
        editor.updateCascadeEditions(
            mapOf(
                "A3" to listOf(
                    Edition(0, a3Entry.copy(end = a3Entry.end + 7f), listOf("end"), Edition.Method.Dragging),
                ),
            ),
        )
        assertEquals(setOf("A3"), editor.cascadeEditions.keys)

        editor.submitEntriesWithCascade()

        val project = editor.project
        assertEquals(current.end + 5f, project.modules.first { it.name == "C4" }.entries[0].end)
        assertEquals(a3Entry.end + 7f, project.modules.first { it.name == "A3" }.entries[0].end)
        assertTrue(editor.cascadeEditions.isEmpty())
    }

    /* endregion */

    /* region scissors */

    @Test
    fun `commitEntryCut cuts the entry with the on-screen scissors input`() {
        val editor = createEditor(otoProject, appConf = plainConf())
        editor.onScreenScissorsState.start(0, 200f, 120f, "- a")
        assertTrue(editor.onScreenScissorsState.isOn)
        assertEquals(0, editor.onScreenScissorsState.entryIndex)
        assertEquals(200f, editor.onScreenScissorsState.timePosition)
        assertEquals(120f, editor.onScreenScissorsState.pixelPosition)
        assertEquals("- a", editor.onScreenScissorsState.text)

        editor.onScreenScissorsState.text = "cut"
        editor.commitEntryCut()

        assertFalse(editor.onScreenScissorsState.isOn)
        val module = editor.project.currentModule
        assertEquals(3, module.entries.size)
        // askForName = Former: the former part takes the input name and ends at the cut position
        assertEquals("cut", module.entries[0].name)
        assertEquals(200f, module.entries[0].end)
        assertEquals("- a", module.entries[1].name)
        assertEquals(200f, module.entries[1].start)
        // goTo = Latter moves the cursor to the new latter entry
        assertEquals(1, module.currentIndex)
    }

    @Test
    fun `commitEntryCut without a name only closes the scissors state`() {
        val editor = createEditor(otoProject, appConf = plainConf())
        val before = editor.project
        editor.onScreenScissorsState.start(0, 200f, 120f, "")

        editor.commitEntryCut()

        assertFalse(editor.onScreenScissorsState.isOn)
        assertSame(before, editor.project)
    }

    @Test
    fun `cutEntry without a loaded sample is a no-op`() {
        val editor = createEditor(otoProject, appConf = plainConf())
        val before = editor.project

        editor.cutEntry(0, 200f, 120f)

        assertFalse(editor.onScreenScissorsState.isOn)
        assertSame(before, editor.project)
        assertNull(env.dialogArgs)
    }

    @Test
    fun `cutEntry with on-screen scissors starts the input state`() {
        val appConf = plainConf()
        val editor = createEditor(nnsvsProject, appConf = appConf) {
            jumpToModuleByNameAndEntry("doremi", 2)
        }
        loadSample(editor, appConf)
        assertTrue(editor.canUseOnScreenScissors)

        editor.cutEntry(2, 500f, 42f)

        assertTrue(editor.onScreenScissorsState.isOn)
        assertEquals(2, editor.onScreenScissorsState.entryIndex)
        assertEquals(500f, editor.onScreenScissorsState.timePosition)
        assertEquals(42f, editor.onScreenScissorsState.pixelPosition)
        assertEquals("o", editor.onScreenScissorsState.text)
    }

    @Test
    fun `cutEntry with askForName none cuts immediately with a default name`() {
        val appConf = plainConf(askForName = AppConf.ScissorsActions.Target.None)
        val editor = createEditor(nnsvsProject, appConf = appConf) {
            jumpToModuleByNameAndEntry("doremi", 2)
        }
        loadSample(editor, appConf)
        val beforeCount = editor.project.currentModule.entries.size

        editor.cutEntry(2, 500f, 42f)

        assertFalse(editor.onScreenScissorsState.isOn)
        val module = editor.project.currentModule
        assertEquals(beforeCount + 1, module.entries.size)
        assertEquals(500f, module.entries[2].end)
        assertEquals(500f, module.entries[3].start)
        // goTo = Latter selects the new latter entry
        assertEquals(3, module.currentIndex)
    }

    @Test
    fun `cutEntry without on-screen scissors opens the cut name dialog`() {
        val appConf = plainConf(useOnScreenScissors = false)
        val editor = createEditor(otoProject, appConf = appConf)
        loadSample(editor, appConf)

        editor.cutEntry(0, 200f, 0f)

        // the unconfined main scope has run the request up to the dialog suspension point
        val args = assertIs<InputEntryNameDialogArgs>(env.dialogArgs)
        assertEquals(0, args.index)
        assertEquals("- a", args.initial)
        assertEquals(InputEntryNameDialogPurpose.CutFormer, args.purpose)
        assertFalse(editor.onScreenScissorsState.isOn)
    }

    @Test
    fun `canUseOnScreenScissors requires the configuration and multiple edit mode`() {
        assertFalse(createEditor(otoProject).canUseOnScreenScissors)
        assertTrue(createEditor(nnsvsProject).canUseOnScreenScissors)
        assertFalse(
            createEditor(
                nnsvsProject,
                appConf = AppConf(editor = AppConf.Editor(useOnScreenScissors = false)),
            ).canUseOnScreenScissors,
        )
    }

    /* endregion */

    /* region sample loading and canvas state */

    @Test
    fun `loadSample transitions to loaded and initializes the render progress`() {
        val editor = createEditor(otoProject)
        loadSample(editor, AppConf())

        val loaded = assertIs<CanvasState.Loaded>(editor.canvasState)
        assertEquals("_a_ka.wav", loaded.sampleInfo.name)
        assertEquals(loaded.sampleInfo, editor.getSampleInfo())
        assertEquals(loaded.sampleInfo.length, loaded.params.dataLength)
        assertEquals(loaded.sampleInfo.chunkCount, loaded.params.chunkCount)
        assertEquals(editor.canvasResolution, loaded.params.resolution)
        assertFalse(editor.isLoading)
        assertEquals(0 to loaded.sampleInfo.totalChartCount, editor.renderProgress)

        editor.cancelLoading()
        assertEquals(CanvasState.Loading, editor.canvasState)
        assertEquals(0 to 0, editor.renderProgress)
    }

    @Test
    fun `loadSample redirects when the sample directory is missing`() {
        val sampleDir = TestFixtures.deploy(
            "oto",
            tempDir.resolve("missing-oto"),
            wavFiles = listOf("_a_ka.wav"),
        )
        val project = createTestProject(
            labeler = TestLabelers.utauOto,
            sampleDirectory = sampleDir,
            inputFilePath = sampleDir.resolve("oto.ini").absolutePath,
        )
        val editor = createEditor(project)
        sampleDir.deleteRecursively()

        runBlocking { editor.loadSample(AppConf()) }

        assertEquals(CanvasState.Error, editor.canvasState)
        assertTrue(editor.isError)
        assertIs<CommonConfirmationDialogAction.RedirectSampleDirectory>(env.dialogArgs)
    }

    @Test
    fun `changeResolution updates the resolution and the canvas params when loaded`() {
        val editor = createEditor(otoProject)
        editor.changeResolution(200)
        assertEquals(200, editor.canvasResolution)
        // without a loaded sample only the resolution changes
        assertEquals(CanvasState.Loading, editor.canvasState)

        loadSample(editor, AppConf())
        editor.changeResolution(300)

        assertEquals(300, editor.canvasResolution)
        val loaded = assertIs<CanvasState.Loaded>(editor.canvasState)
        assertEquals(300, loaded.params.resolution)
        assertEquals(loaded.sampleInfo.length.toFloat() / 300, loaded.params.lengthInPixel)
    }

    @Test
    fun `renderCharts renders through the chart store and clear resets it`() {
        val editor = createEditor(otoProject)
        loadSample(editor, AppConf())
        val sampleInfo = assertNotNull(editor.getSampleInfo())
        assertEquals(0 to sampleInfo.totalChartCount, editor.renderProgress)

        editor.renderCharts(scope, sampleInfo, AppConf(), Density(1f), LayoutDirection.Ltr)
        runBlocking { editor.chartStore.awaitLoad() }

        assertEquals(sampleInfo.totalChartCount to sampleInfo.totalChartCount, editor.renderProgress)
        assertEquals(ChartStore.ChartLoadingStatus.Loaded, editor.chartStore.getWaveformStatus(0, 0))
        assertEquals(ChartStore.ChartLoadingStatus.Loaded, editor.chartStore.getSpectrogramStatus(0))
        assertTrue(editor.chartStore.hasCachedSample(sampleInfo))

        editor.clear()
        assertNull(editor.chartStore.getWaveformStatus(0, 0))
        assertNull(editor.chartStore.getSpectrogramStatus(0))
    }

    /* endregion */

    /* region navigation and dialogs */

    @Test
    fun `navigation and module functions delegate to the project store`() {
        val editor = createEditor(utauSingerProject) {
            jumpToModuleByNameAndEntry("C4", 0)
        }

        editor.jumpToEntry("C4", 1)
        assertEquals(1, editor.project.currentModule.currentIndex)
        assertEquals("a ka", editor.entryTitle)

        editor.jumpToModule("A3")
        assertEquals("A3", editor.project.currentModule.name)

        val c4Index = editor.project.modules.indexOfFirst { it.name == "C4" }
        editor.jumpToModule(c4Index)
        assertEquals("C4", editor.project.currentModule.name)

        editor.jumpToModule("A3", targetEntryIndex = 1)
        assertEquals("A3", editor.project.currentModule.name)
        assertEquals(1, editor.project.currentModule.currentIndex)

        editor.createDefaultEntry("C4", "_a_ka.wav")
        assertEquals(5, editor.project.modules.first { it.name == "C4" }.entries.size)

        editor.createDefaultEntries("C4", listOf("_a_ka.wav", "_i_ki.wav"))
        assertEquals(7, editor.project.modules.first { it.name == "C4" }.entries.size)

        val root = editor.project.rootSampleDirectory
        editor.changeSampleDirectory("C4", root)
        val c4 = editor.project.modules.first { it.name == "C4" }
        assertEquals(root.absolutePath, c4.getSampleDirectory(editor.project).absolutePath)
    }

    @Test
    fun `dialog functions open the corresponding embedded dialogs`() {
        val editor = createEditor(otoProject)

        editor.openEditEntryNameDialog(0, InputEntryNameDialogPurpose.Rename)
        var nameArgs = assertIs<InputEntryNameDialogArgs>(env.dialogArgs)
        assertEquals(0, nameArgs.index)
        assertEquals("- a", nameArgs.initial)
        assertEquals(InputEntryNameDialogPurpose.Rename, nameArgs.purpose)
        // the oto labeler does not allow duplicate names; renaming excludes the entry's own name
        assertEquals(listOf("a ka"), nameArgs.invalidOptions)

        editor.consumeEditorEntryContextAction(EditorEntryContextAction.OpenDuplicateEntryDialog(1))
        nameArgs = assertIs(env.dialogArgs)
        assertEquals(1, nameArgs.index)
        assertEquals(InputEntryNameDialogPurpose.Duplicate, nameArgs.purpose)

        editor.consumeEditorEntryContextAction(EditorEntryContextAction.OpenRenameEntryDialog(1))
        nameArgs = assertIs(env.dialogArgs)
        assertEquals(InputEntryNameDialogPurpose.Rename, nameArgs.purpose)

        editor.consumeEditorEntryContextAction(EditorEntryContextAction.OpenMoveEntryDialog(1))
        val moveArgs = assertIs<MoveEntryDialogArgs>(env.dialogArgs)
        assertEquals(1, moveArgs.currentIndex)

        editor.consumeEditorEntryContextAction(EditorEntryContextAction.OpenRemoveEntryDialog(0))
        val removeArgs = assertIs<CommonConfirmationDialogAction.RemoveEntry>(env.dialogArgs)
        assertEquals(0, removeArgs.entryIndex)

        editor.consumeEditorEntryContextAction(EditorEntryContextAction.RemoveEntries(listOf(0, 1)))
        val removeEntriesArgs = assertIs<CommonConfirmationDialogAction.RemoveEntries>(env.dialogArgs)
        assertEquals(listOf(0, 1), removeEntriesArgs.entryIndexes)

        editor.consumeEditorEntryContextAction(EditorEntryContextAction.EditEntriesTag(listOf(0, 1), "common"))
        val tagArgs = assertIs<EditEntriesTagDialogArgs>(env.dialogArgs)
        assertEquals(listOf(0, 1), tagArgs.indexes)
        assertEquals("common", tagArgs.initial)

        editor.editEntryExtra(0)
        val extraArgs = assertIs<EditExtraDialogArgs>(env.dialogArgs)
        assertEquals(0, extraArgs.index)
    }

    @Test
    fun `context filter actions update the project entry filter`() {
        val editor = createEditor(otoProject)

        editor.consumeEditorEntryContextAction(EditorEntryContextAction.FilterByEntryName("- a"))
        assertEquals(listOf(0), editor.project.currentModule.filteredEntryIndexes)

        // the sample name filter matches the sample name without its extension
        editor.consumeEditorEntryContextAction(EditorEntryContextAction.FilterBySampleName("_a_ka"))
        assertEquals(listOf(0, 1), editor.project.currentModule.filteredEntryIndexes)

        editor.editEntryTag(0, "x")
        editor.consumeEditorEntryContextAction(EditorEntryContextAction.FilterByTag("x"))
        assertEquals(listOf(0), editor.project.currentModule.filteredEntryIndexes)

        editor.consumeEditorEntryContextAction(EditorEntryContextAction.SetEntriesStar(listOf(0), true))
        assertTrue(editor.project.currentModule.entries[0].notes.star)
        editor.consumeEditorEntryContextAction(EditorEntryContextAction.FilterStarred())
        assertEquals(EntryFilter(star = true), editor.project.entryFilter)
        assertEquals(listOf(0), editor.project.currentModule.filteredEntryIndexes)

        editor.consumeEditorEntryContextAction(EditorEntryContextAction.FilterUnstarred())
        assertEquals(listOf(1), editor.project.currentModule.filteredEntryIndexes)

        editor.consumeEditorEntryContextAction(EditorEntryContextAction.SetEntriesDone(listOf(1), true))
        assertTrue(editor.project.currentModule.entries[1].notes.done)
        editor.consumeEditorEntryContextAction(EditorEntryContextAction.FilterDone())
        assertEquals(listOf(1), editor.project.currentModule.filteredEntryIndexes)

        editor.consumeEditorEntryContextAction(EditorEntryContextAction.FilterUndone())
        assertEquals(listOf(0), editor.project.currentModule.filteredEntryIndexes)
    }

    @Test
    fun `pinned entry list filter submits to the project only when linked`() {
        val editor = createEditor(otoProject)
        assertFalse(editor.pinnedEntryListFilterState.linked)

        editor.pinnedEntryListFilterState.editFilter { copy(searchText = "name:ka") }
        // not linked: the project filter is untouched
        assertNull(editor.project.entryFilter)

        editor.pinnedEntryListFilterState.toggleLinked()
        assertTrue(editor.pinnedEntryListFilterState.linked)
        assertEquals(EntryFilter(searchText = "name:ka"), editor.project.entryFilter)
        assertEquals(listOf(1), editor.project.currentModule.filteredEntryIndexes)

        editor.pinnedEntryListFilterState.clear()
        assertFalse(editor.pinnedEntryListFilterState.linked)
        assertNull(editor.project.entryFilter)
        assertEquals(listOf(0, 1), editor.project.currentModule.filteredEntryIndexes)
    }

    @Test
    fun `handleSetPropertyKeyAction opens the set property dialog for bound properties`() {
        val editor = createEditor(otoProject)

        assertTrue(editor.handleSetPropertyKeyAction(KeyAction.SetProperty1))
        val args = assertIs<SetEntryPropertyDialogArgs>(env.dialogArgs)
        // property 0 ("left") of the oto labeler is bound to the first shortcut; its getter reads points[3]
        assertEquals(0, args.propertyIndex)
        assertEquals(editor.project.currentEntry.points[3], args.currentValue)

        // no property of the oto labeler is bound to the 10th shortcut
        assertFalse(editor.handleSetPropertyKeyAction(KeyAction.SetProperty10))
        // non-property actions are not handled
        assertFalse(editor.handleSetPropertyKeyAction(KeyAction.NavigateNextEntry))
    }

    /* endregion */

    companion object {

        private val sharedTempDir: File by lazy {
            createTempDirectory("vlabeler-test").toFile().also { dir ->
                Runtime.getRuntime().addShutdownHook(Thread { dir.deleteRecursively() })
            }
        }

        /**
         * A single-module oto project with entries "- a" and "a ka" on the real sample "_a_ka.wav".
         */
        private val otoProject: Project by lazy {
            val sampleDir = TestFixtures.deploy(
                "oto",
                sharedTempDir.resolve("oto"),
                wavFiles = listOf("_a_ka.wav"),
            )
            createTestProject(
                labeler = TestLabelers.utauOto,
                sampleDirectory = sampleDir,
                inputFilePath = sampleDir.resolve("oto.ini").absolutePath,
            )
        }

        /**
         * A two-module utau-singer project: "A3" ("- aA3", "a kaA3") and "C4" ("- a", "a ka", "- i", "i ki").
         */
        private val utauSingerProject: Project by lazy {
            val sampleDir = TestFixtures.deploy(
                "utau-singer",
                sharedTempDir.resolve("utau-singer"),
                wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
            )
            createTestProject(labeler = TestLabelers.utauSinger, sampleDirectory = sampleDir)
        }

        /**
         * A continuous nnsvs-singer project in multiple edit mode with modules "doremi"
         * (entries "pau", "d", "o", "r", "e", "pau") and "legato".
         */
        private val nnsvsProject: Project by lazy {
            val sampleDir = TestFixtures.deploy(
                "nnsvs-singer",
                sharedTempDir.resolve("nnsvs-singer"),
                wavFiles = listOf("wav/doremi.wav", "wav/legato.wav"),
                wavDurationMs = 1500,
            )
            createTestProject(labeler = TestLabelers.nnsvsSinger, sampleDirectory = sampleDir)
        }
    }
}
