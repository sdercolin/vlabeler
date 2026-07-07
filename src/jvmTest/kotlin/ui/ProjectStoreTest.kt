package ui

import androidx.compose.runtime.mutableStateOf
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.InvalidEditedProjectException
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.model.filter.EntryFilter
import com.sdercolin.vlabeler.ui.AppErrorStateImpl
import com.sdercolin.vlabeler.ui.AppProgressStateImpl
import com.sdercolin.vlabeler.ui.AppScreenStateImpl
import com.sdercolin.vlabeler.ui.AppUnsavedChangesStateImpl
import com.sdercolin.vlabeler.ui.ProjectStoreImpl
import com.sdercolin.vlabeler.ui.editor.Edition
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
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
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ProjectStoreTest {

    private lateinit var storeScope: CoroutineScope
    private lateinit var screenState: AppScreenStateImpl
    private lateinit var errorState: AppErrorStateImpl
    private lateinit var progressState: AppProgressStateImpl

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
        storeScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @AfterTest
    fun teardown() {
        storeScope.cancel()
        Log.muted = false
    }

    private fun createStore(appConf: AppConf = AppConf()): ProjectStoreImpl {
        screenState = AppScreenStateImpl()
        errorState = AppErrorStateImpl()
        progressState = AppProgressStateImpl()
        return ProjectStoreImpl(
            scope = storeScope,
            appConf = mutableStateOf(appConf),
            screenState = screenState,
            scrollFitViewModel = ScrollFitViewModel(storeScope),
            errorState = errorState,
            progressState = progressState,
        )
    }

    /**
     * Creates a store with the shared base project loaded, normalized to module "C4" at entry 0.
     */
    private fun createStoreWithProject(appConf: AppConf = AppConf()): ProjectStoreImpl =
        createStore(appConf).apply {
            newProject(baseProject)
            jumpToModuleByNameAndEntry("C4", 0)
        }

    /**
     * Waits for all coroutines launched by the store on [storeScope] to finish.
     */
    private fun awaitStoreJobs() {
        runBlocking {
            storeScope.coroutineContext.job.children.toList().joinAll()
        }
    }

    /* region project lifecycle */

    @Test
    fun `initial state has no project`() {
        val store = createStore()
        assertNull(store.project)
        assertFalse(store.hasProject)
        assertFailsWith<IllegalArgumentException> { store.requireProject() }
    }

    @Test
    fun `newProject sets the project and initializes history`() {
        val store = createStore()
        store.newProject(baseProject)
        assertTrue(store.hasProject)
        assertEquals(baseProject, store.requireProject())
        assertEquals(store.requireProject(), store.history.current)
        assertFalse(store.canUndo)
        assertFalse(store.canRedo)
    }

    @Test
    fun `newProject replaces an existing project and resets history`() {
        val store = createStoreWithProject()
        store.editCurrentEntryTag("edited")
        assertTrue(store.canUndo)
        store.newProject(baseProject)
        assertEquals(baseProject, store.requireProject())
        assertFalse(store.canUndo)
        assertFalse(store.canRedo)
    }

    @Test
    fun `clearProject resets the store`() {
        val store = createStoreWithProject()
        store.clearProject()
        assertNull(store.project)
        assertFalse(store.hasProject)
        assertFalse(store.canUndo)
        assertFalse(store.canRedo)
    }

    /* endregion */

    /* region editProject and updateProject */

    @Test
    fun `editProject applies a valid edit and pushes history`() {
        val store = createStoreWithProject()
        store.editProject { updateCurrentModule { editEntryTag(0, "hello") } }
        assertEquals("hello", store.requireProject().currentModule.entries[0].notes.tag)
        assertTrue(store.canUndo)
        assertNull(errorState.error)
    }

    @Test
    fun `editProject reports invalid edits and keeps the project`() {
        val store = createStoreWithProject()
        val before = store.requireProject()
        store.editProject { copy(currentModuleIndex = 99) }
        assertIs<InvalidEditedProjectException>(errorState.error)
        assertSame(before, store.requireProject())
        assertFalse(store.canUndo)
    }

    @Test
    fun `updateProject pushes a changed project`() {
        val store = createStoreWithProject()
        val updated = store.requireProject().updateCurrentModule { editEntryTag(0, "updated") }
        store.updateProject(updated)
        assertEquals("updated", store.requireProject().currentModule.entries[0].notes.tag)
        assertTrue(store.canUndo)
    }

    @Test
    fun `updateProject ignores an equal project`() {
        val store = createStoreWithProject()
        val before = store.requireProject()
        store.updateProject(before.copy())
        assertFalse(store.canUndo)
        assertEquals(before, store.requireProject())
    }

    /* endregion */

    /* region undo and redo */

    @Test
    fun `undo and redo restore project states`() {
        val store = createStoreWithProject()
        store.editCurrentEntryTag("first")
        store.editCurrentEntryTag("second")

        assertTrue(store.canUndo)
        store.undo()
        assertEquals("first", store.requireProject().currentModule.entries[0].notes.tag)
        assertTrue(store.canRedo)

        store.undo()
        assertEquals("", store.requireProject().currentModule.entries[0].notes.tag)
        assertFalse(store.canUndo)

        store.redo()
        assertEquals("first", store.requireProject().currentModule.entries[0].notes.tag)
        store.redo()
        assertEquals("second", store.requireProject().currentModule.entries[0].notes.tag)
        assertFalse(store.canRedo)
    }

    @Test
    fun `undo keeps the current indexes when index changes are squashed`() {
        val store = createStoreWithProject()
        store.editEntryTag(0, "edited")
        // an index-only change is squashed by the history with the default configuration
        store.jumpToEntry(1)
        assertEquals(1, store.requireProject().currentModule.currentIndex)

        store.undo()
        val project = store.requireProject()
        assertEquals("", project.currentModule.entries[0].notes.tag)
        // the cursor does not move on undo
        assertEquals(1, project.currentModule.currentIndex)
        assertFalse(store.canUndo)
    }

    @Test
    fun `undo restores the indexes when squashing is disabled`() {
        val conf = AppConf(history = AppConf.History(squashIndex = false))
        val store = createStore(conf)
        store.newProject(baseProject)
        val initialIndex = store.requireProject().currentModule.currentIndex
        store.jumpToEntry(1)
        assertTrue(store.canUndo)

        store.undo()
        assertEquals(initialIndex, store.requireProject().currentModule.currentIndex)
        assertTrue(store.canRedo)
        store.redo()
        assertEquals(1, store.requireProject().currentModule.currentIndex)
    }

    /* endregion */

    /* region entry navigation */

    @Test
    fun `entry navigation moves within the module`() {
        val store = createStoreWithProject()
        assertFalse(store.canGoPreviousEntryOrSample)
        assertTrue(store.canGoNextEntryOrSample)

        store.nextEntry()
        assertEquals(1, store.requireProject().currentModule.currentIndex)
        assertTrue(store.canGoPreviousEntryOrSample)

        store.previousEntry()
        assertEquals(0, store.requireProject().currentModule.currentIndex)

        // no-op at the first entry
        store.previousEntry()
        assertEquals(0, store.requireProject().currentModule.currentIndex)

        store.jumpToEntry(3)
        assertFalse(store.canGoNextEntryOrSample)
        // no-op at the last entry
        store.nextEntry()
        assertEquals(3, store.requireProject().currentModule.currentIndex)
    }

    @Test
    fun `navigation flags are false without a project`() {
        val store = createStore()
        assertFalse(store.canGoNextEntryOrSample)
        assertFalse(store.canGoPreviousEntryOrSample)
        assertFalse(store.canGoNextModule)
        assertFalse(store.canGoPreviousModule)
    }

    @Test
    fun `nextSample and previousSample switch between sample groups`() {
        val store = createStoreWithProject()
        // module "C4": entries 0..1 belong to sample "_a_ka.wav", entries 2..3 to "_i_ki.wav"
        store.nextSample()
        assertEquals(2, store.requireProject().currentModule.currentIndex)

        // at the last sample: move to the last entry of the current group
        store.nextSample()
        assertEquals(3, store.requireProject().currentModule.currentIndex)

        store.previousSample()
        assertEquals(1, store.requireProject().currentModule.currentIndex)

        // at the first sample: move to the first entry of the current group
        store.previousSample()
        assertEquals(0, store.requireProject().currentModule.currentIndex)
    }

    @Test
    fun `jumpToEntry with module name targets another module without switching`() {
        val store = createStoreWithProject()
        store.jumpToEntry("A3", 1)
        val project = store.requireProject()
        assertEquals("C4", project.currentModule.name)
        assertEquals(1, project.modules.first { it.name == "A3" }.currentIndex)
    }

    @Test
    fun `jumpToModuleAndEntry switches module and entry`() {
        val store = createStoreWithProject()
        val a3Index = store.requireProject().modules.indexOfFirst { it.name == "A3" }
        store.jumpToModuleAndEntry(a3Index, 1)
        val project = store.requireProject()
        assertEquals("A3", project.currentModule.name)
        assertEquals(1, project.currentModule.currentIndex)
    }

    @Test
    fun `jumpToModuleByNameAndEntry with an unknown module is a no-op`() {
        val store = createStoreWithProject()
        val before = store.requireProject()
        store.jumpToModuleByNameAndEntry("unknown", 0)
        assertSame(before, store.requireProject())
    }

    @Test
    fun `jumpToModuleByNameAndEntryName finds the entry by name`() {
        val store = createStoreWithProject()
        store.jumpToModuleByNameAndEntryName("C4", "i ki")
        val project = store.requireProject()
        assertEquals("C4", project.currentModule.name)
        assertEquals(3, project.currentModule.currentIndex)
    }

    @Test
    fun `jumpToModuleByNameAndEntryName with an unknown entry is a no-op`() {
        val store = createStoreWithProject()
        val before = store.requireProject()
        store.jumpToModuleByNameAndEntryName("C4", "unknown entry")
        assertSame(before, store.requireProject())
    }

    /* endregion */

    /* region module navigation */

    @Test
    fun `module navigation moves between modules`() {
        val store = createStoreWithProject()
        assertTrue(store.shouldShowModuleNavigation())
        store.jumpToModule(0)
        assertFalse(store.canGoPreviousModule)
        assertTrue(store.canGoNextModule)

        store.nextModule()
        assertEquals(1, store.requireProject().currentModuleIndex)
        assertFalse(store.canGoNextModule)
        assertTrue(store.canGoPreviousModule)

        // no-op at the last module
        store.nextModule()
        assertEquals(1, store.requireProject().currentModuleIndex)

        store.previousModule()
        assertEquals(0, store.requireProject().currentModuleIndex)
    }

    @Test
    fun `jumpToModule with a target entry index sets the entry`() {
        val store = createStoreWithProject()
        val a3Index = store.requireProject().modules.indexOfFirst { it.name == "A3" }
        store.jumpToModule(a3Index, targetEntryIndex = 1)
        val project = store.requireProject()
        assertEquals("A3", project.currentModule.name)
        assertEquals(1, project.currentModule.currentIndex)
    }

    @Test
    fun `jumpToModule to the current position is a no-op`() {
        val store = createStoreWithProject()
        val before = store.requireProject()
        store.jumpToModule(before.currentModuleIndex)
        assertSame(before, store.requireProject())
        store.jumpToModule(before.currentModuleIndex, targetEntryIndex = before.currentModule.currentIndex)
        assertSame(before, store.requireProject())
    }

    /* endregion */

    /* region entry edits */

    @Test
    fun `renameEntry renames the entry`() {
        val store = createStoreWithProject()
        store.renameEntry(0, "renamed")
        assertEquals("renamed", store.requireProject().currentModule.entries[0].name)
        assertTrue(store.canUndo)
    }

    @Test
    fun `duplicateEntry inserts a copy after the entry and selects it`() {
        val store = createStoreWithProject()
        store.duplicateEntry(0, "dup")
        val module = store.requireProject().currentModule
        assertEquals(5, module.entries.size)
        assertEquals("dup", module.entries[1].name)
        assertEquals(module.entries[0].copy(name = "dup"), module.entries[1])
        assertEquals(1, module.currentIndex)
    }

    @Test
    fun `removeEntry removes the entry and moves the cursor backwards`() {
        val store = createStoreWithProject()
        store.jumpToEntry(1)
        store.removeEntry(1)
        val module = store.requireProject().currentModule
        assertEquals(listOf("- a", "- i", "i ki"), module.entries.map { it.name })
        assertEquals(0, module.currentIndex)
    }

    @Test
    fun `removeEntries removes multiple entries`() {
        val store = createStoreWithProject()
        store.removeEntries(listOf(1, 3))
        val module = store.requireProject().currentModule
        assertEquals(listOf("- a", "- i"), module.entries.map { it.name })
        assertEquals(0, module.currentIndex)
    }

    @Test
    fun `moveEntry moves the entry and follows the cursor`() {
        val store = createStoreWithProject()
        assertTrue(store.canMoveEntry)
        store.moveEntry(0, 1)
        val module = store.requireProject().currentModule
        assertEquals(listOf("a ka", "- a", "- i", "i ki"), module.entries.map { it.name })
        assertEquals(1, module.currentIndex)
    }

    @Test
    fun `isOnlyEntry is false when the sample has multiple entries`() {
        val store = createStoreWithProject()
        assertFalse(store.isOnlyEntry())
    }

    @Test
    fun `toggleMultipleEditMode on a non-continuous labeler reports an error`() {
        val store = createStoreWithProject()
        val before = store.requireProject()
        store.toggleMultipleEditMode(true)
        assertIs<InvalidEditedProjectException>(errorState.error)
        assertSame(before, store.requireProject())
    }

    @Test
    fun `toggle done and star of entries`() {
        val store = createStoreWithProject()
        store.toggleEntryDone(1)
        assertTrue(store.requireProject().currentModule.entries[1].notes.done)
        store.toggleEntryDone(1)
        assertFalse(store.requireProject().currentModule.entries[1].notes.done)

        store.toggleCurrentEntryDone()
        assertTrue(store.requireProject().currentModule.entries[0].notes.done)

        store.toggleEntryStar(1)
        assertTrue(store.requireProject().currentModule.entries[1].notes.star)
        store.toggleCurrentEntryStar()
        assertTrue(store.requireProject().currentModule.entries[0].notes.star)
    }

    @Test
    fun `edit entry tags`() {
        val store = createStoreWithProject()
        store.editEntryTag(1, "tag1")
        assertEquals("tag1", store.requireProject().currentModule.entries[1].notes.tag)

        store.editCurrentEntryTag("tag0")
        assertEquals("tag0", store.requireProject().currentModule.entries[0].notes.tag)

        store.editEntriesTag(listOf(2, 3), "bulk")
        val module = store.requireProject().currentModule
        assertEquals("bulk", module.entries[2].notes.tag)
        assertEquals("bulk", module.entries[3].notes.tag)
    }

    @Test
    fun `set done and star of multiple entries`() {
        val store = createStoreWithProject()
        store.setEntriesDone(listOf(0, 2), true)
        var module = store.requireProject().currentModule
        assertEquals(listOf(true, false, true, false), module.entries.map { it.notes.done })

        store.setEntriesDone(listOf(0), false)
        assertFalse(store.requireProject().currentModule.entries[0].notes.done)

        store.setEntriesStar(listOf(1, 3), true)
        module = store.requireProject().currentModule
        assertEquals(listOf(false, true, false, true), module.entries.map { it.notes.star })
    }

    @Test
    fun `createDefaultEntries appends entries with default values`() {
        val store = createStoreWithProject()
        store.createDefaultEntry("C4", "_a_ka.wav")
        val module = store.requireProject().currentModule
        assertEquals(5, module.entries.size)
        val created = module.entries.last()
        assertEquals("_a_ka.wav", created.sample)
        assertEquals(baseProject.labelerConf.defaultValues.first(), created.start)
        assertEquals(baseProject.labelerConf.defaultValues.last(), created.end)
    }

    @Test
    fun `updateEntryExtra updates the extra fields`() {
        val store = createStoreWithProject()
        store.updateEntryExtra(0, listOf("123"))
        assertEquals(listOf("123"), store.requireProject().currentModule.entries[0].extras)
    }

    @Test
    fun `importEntries appends entries to a module`() {
        val store = createStoreWithProject()
        val imported = baseProject.modules.first { it.name == "C4" }.entries[0].copy(name = "imported")
        store.importEntries(listOf("C4" to listOf(imported)), replace = false)
        val module = store.requireProject().currentModule
        assertEquals(5, module.entries.size)
        assertEquals("imported", module.entries.last().name)
    }

    @Test
    fun `importEntries replaces entries and coerces the current index`() {
        val store = createStoreWithProject()
        store.jumpToEntry(3)
        val newEntries = baseProject.modules.first { it.name == "C4" }.entries.take(2)
        store.importEntries(listOf("C4" to newEntries), replace = true)
        val module = store.requireProject().currentModule
        assertEquals(2, module.entries.size)
        assertEquals(1, module.currentIndex)
    }

    /* endregion */

    /* region editions */

    @Test
    fun `editEntries applies the edition and takes the post-edit done action`() {
        val store = createStoreWithProject()
        val entry = store.requireProject().currentModule.entries[0]
        val edited = entry.copy(end = entry.end + 5f)
        store.editEntries(listOf(Edition(0, edited, listOf("end"), Edition.Method.Dragging)))
        val result = store.requireProject().currentModule.entries[0]
        assertEquals(entry.end + 5f, result.end)
        // the default post-edit action marks the edited entry as done
        assertTrue(result.notes.done)
        assertTrue(store.canUndo)
    }

    @Test
    fun `editEntriesWithCascade applies editions to other modules`() {
        val store = createStoreWithProject()
        val currentEntry = store.requireProject().currentModule.entries[0]
        val a3Entry = store.requireProject().modules.first { it.name == "A3" }.entries[0]
        store.editEntriesWithCascade(
            currentModuleEditions = listOf(
                Edition(0, currentEntry.copy(end = currentEntry.end + 5f), listOf("end"), Edition.Method.Dragging),
            ),
            cascadeEditions = mapOf(
                "A3" to listOf(
                    Edition(0, a3Entry.copy(end = a3Entry.end + 7f), listOf("end"), Edition.Method.Dragging),
                ),
            ),
        )
        val project = store.requireProject()
        assertEquals(currentEntry.end + 5f, project.currentModule.entries[0].end)
        assertEquals(a3Entry.end + 7f, project.modules.first { it.name == "A3" }.entries[0].end)
    }

    @Test
    fun `cutEntry splits an entry at the position`() {
        val store = createStoreWithProject()
        val original = store.requireProject().currentModule.entries[0]
        store.cutEntry(0, position = 200f, rename = null, newName = "cut", targetEntryIndex = null)
        val module = store.requireProject().currentModule
        assertEquals(5, module.entries.size)
        assertEquals(original.name, module.entries[0].name)
        assertEquals(200f, module.entries[0].end)
        assertTrue(module.entries[0].notes.done)
        assertEquals("cut", module.entries[1].name)
        assertEquals(200f, module.entries[1].start)
        assertEquals(original.end, module.entries[1].end)
        assertEquals(0, module.currentIndex)
    }

    @Test
    fun `cutEntryOnScreen with former target renames the former part`() {
        val store = createStoreWithProject()
        val originalName = store.requireProject().currentModule.entries[0].name
        store.cutEntryOnScreen(
            index = 0,
            position = 200f,
            name = "former",
            target = AppConf.ScissorsActions.Target.Former,
            targetEntryIndex = 1,
        )
        val module = store.requireProject().currentModule
        assertEquals("former", module.entries[0].name)
        assertEquals(originalName, module.entries[1].name)
        assertEquals(1, module.currentIndex)
    }

    /* endregion */

    /* region entry filter */

    @Test
    fun `updateEntryFilter filters entries and adjusts the current index`() {
        val store = createStoreWithProject()
        store.updateEntryFilter { copy(searchText = "ki") }
        val project = store.requireProject()
        assertEquals("ki", project.entryFilter?.searchText)
        assertEquals(listOf(2, 3), project.currentModule.filteredEntryIndexes)
        assertEquals(2, project.currentModule.currentIndex)
    }

    @Test
    fun `updateEntryFilter with a null updater keeps the project`() {
        val store = createStoreWithProject()
        store.updateEntryFilter { copy(searchText = "ki") }
        val before = store.requireProject()
        store.updateEntryFilter { null }
        assertEquals(before, store.requireProject())
    }

    @Test
    fun `updateEntryFilter with an empty filter includes all entries`() {
        val store = createStoreWithProject()
        store.updateEntryFilter { copy(searchText = "ki") }
        store.updateEntryFilter { EntryFilter() }
        assertEquals(listOf(0, 1, 2, 3), store.requireProject().currentModule.filteredEntryIndexes)
    }

    /* endregion */

    /* region flags */

    @Test
    fun `raw label file related flags for a multi-module project`() {
        val store = createStoreWithProject()
        assertTrue(store.hasRawLabelFileForCurrentModule())
        assertTrue(store.shouldShowOverwriteExportAllModules())
        assertTrue(store.canOverwriteExportAllModules())
        assertTrue(store.canReloadAllLabelFiles())
    }

    @Test
    fun `flags are false without a project`() {
        val store = createStore()
        assertFalse(store.hasRawLabelFileForCurrentModule())
        assertFalse(store.shouldShowOverwriteExportAllModules())
        assertFalse(store.canOverwriteExportAllModules())
        assertFalse(store.canReloadAllLabelFiles())
        assertFalse(store.shouldShowModuleNavigation())
        assertFalse(store.canEditCurrentEntryExtra)
        assertFalse(store.canEditCurrentModuleExtra)
    }

    @Test
    fun `extra edit flags depend on the visible extra fields of the labeler`() {
        val store = createStoreWithProject()
        // the utau-singer labeler has one invisible entry extra field and no module extra fields
        assertFalse(store.canEditCurrentEntryExtra)
        assertFalse(store.canEditCurrentModuleExtra)
    }

    @Test
    fun `single-module project hides multi-module features`() {
        val store = createStore()
        store.newProject(singleModuleProject)
        assertFalse(store.shouldShowModuleNavigation())
        assertFalse(store.shouldShowOverwriteExportAllModules())
        assertFalse(store.canReloadAllLabelFiles())
        assertFalse(store.canGoNextModule)
        assertFalse(store.canGoPreviousModule)
    }

    /* endregion */

    /* region sample directory */

    @Test
    fun `changeSampleDirectory updates the module path in a multi-module project`() {
        val store = createStoreWithProject()
        val root = store.requireProject().rootSampleDirectory
        store.changeSampleDirectory("C4", root)
        val project = store.requireProject()
        val module = project.modules.first { it.name == "C4" }
        assertEquals("", module.sampleDirectoryPath)
        assertEquals(root.absolutePath, module.getSampleDirectory(project).absolutePath)
    }

    @Test
    fun `changeSampleDirectory changes the root for a single-module root project`() {
        val store = createStore()
        store.newProject(singleModuleProject)
        val oldWorkingDirectory = store.requireProject().workingDirectory.absolutePath
        val newRoot = tempDir.resolve("oto-new-root").also { it.mkdirs() }
        store.changeSampleDirectory(newRoot)
        val project = store.requireProject()
        assertEquals(newRoot.absolutePath, project.rootSampleDirectoryPath)
        assertEquals("", project.modules.first().sampleDirectoryPath)
        // the working directory keeps pointing to the previous location
        assertEquals(oldWorkingDirectory, project.workingDirectory.absolutePath)
    }

    /* endregion */

    /* region sample sync */

    @Test
    fun `updateProjectOnLoadedSample syncs negative entry ends and replaces the history top`() {
        val store = createStoreWithProject()
        val originalEnd = store.requireProject().currentModule.entries[0].end
        store.editProject {
            updateCurrentModule {
                copy(entries = entries.toMutableList().also { it[0] = it[0].copy(end = -100f) })
            }
        }
        val sampleName = store.requireProject().currentModule.entries[0].sample

        store.updateProjectOnLoadedSample(sampleInfo(sampleName, "C4", lengthMillis = 1000f), "C4")
        assertEquals(900f, store.requireProject().currentModule.entries[0].end)
        assertNull(errorState.error)

        // the sync replaced the top of the history instead of pushing a new state
        store.undo()
        assertEquals(originalEnd, store.requireProject().currentModule.entries[0].end)
        assertFalse(store.canUndo)
    }

    /* endregion */

    /* region property setter */

    @Test
    fun `setCurrentEntryProperty applies the labeler value setter`() {
        val store = createStoreWithProject()
        // property 0 of the utau-singer labeler is "left": sets points[3] and start if the value is smaller
        runBlocking { store.setCurrentEntryProperty(0, 5f) }
        assertNull(errorState.error)
        val entry = store.requireProject().currentModule.entries[0]
        assertEquals(5f, entry.points[3])
        assertEquals(5f, entry.start)
    }

    @Test
    fun `setCurrentEntryProperty with an invalid index reports an error`() {
        val store = createStoreWithProject()
        val before = store.requireProject()
        runBlocking { store.setCurrentEntryProperty(99, 1f) }
        assertNotNull(errorState.error)
        assertSame(before, store.requireProject())
    }

    /* endregion */

    /* region label file reload and export */

    @Test
    fun `reloadLabelFile with an explicit file replaces the current module entries`() {
        val store = createStoreWithProject()
        val labelFile = baseProject.rootSampleDirectory.resolve("A3/oto.ini")
        store.reloadLabelFile(labelFile, skipConfirmation = true)
        awaitStoreJobs()
        assertNull(errorState.error)
        assertFalse(progressState.isBusy)
        val module = store.requireProject().currentModule
        assertEquals("C4", module.name)
        assertEquals(listOf("- aA3", "a kaA3"), module.entries.map { it.name })
        assertEquals(0, module.currentIndex)
    }

    @Test
    fun `reloadAllLabelFiles restores the entries from the raw label files`() {
        val store = createStoreWithProject()
        store.renameEntry(0, "renamed")
        store.reloadAllLabelFiles(skipConfirmation = true)
        awaitStoreJobs()
        assertNull(errorState.error)
        assertFalse(progressState.isBusy)
        val project = store.requireProject()
        assertEquals(
            listOf("- a", "a ka", "- i", "i ki"),
            project.modules.first { it.name == "C4" }.entries.map { it.name },
        )
        assertEquals(
            listOf("- aA3", "a kaA3"),
            project.modules.first { it.name == "A3" }.entries.map { it.name },
        )
    }

    @Test
    fun `overwriteExportCurrentModule writes the raw label file`() {
        val sampleDir = TestFixtures.deploy(
            "utau-singer",
            tempDir.resolve("export-current"),
            wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
        )
        val project = createTestProject(labeler = TestLabelers.utauSinger, sampleDirectory = sampleDir)
        val store = createStore()
        store.newProject(project)
        store.jumpToModuleByNameAndEntry("C4", 0)
        store.renameEntry(0, "reexported")

        store.overwriteExportCurrentModule()
        awaitStoreJobs()
        assertNull(errorState.error)
        assertFalse(progressState.isBusy)
        val text = sampleDir.resolve("C4/oto.ini").readText()
        assertTrue(text.contains("reexported"))
    }

    @Test
    fun `withExporting runs the block and rethrows failures`() {
        val store = createStoreWithProject()
        val file = tempDir.resolve("export-marker.txt").also { it.writeText("content") }
        runBlocking { store.withExporting { file } }
        assertFailsWith<IllegalStateException> {
            runBlocking { store.withExporting { error("boom") } }
        }
    }

    /* endregion */

    /* region auto save */

    @Test
    fun `enableAutoSaveProject with none target does not launch a job`() {
        val store = createStoreWithProject()
        store.enableAutoSaveProject(
            AppConf.AutoSave(target = AppConf.AutoSave.Target.None),
            AppUnsavedChangesStateImpl(),
        )
        assertEquals(0, storeScope.coroutineContext.job.children.count())
        runBlocking { store.terminateAutoSaveProject() }
    }

    @Test
    fun `enableAutoSaveProject launches a job that terminate cancels`() {
        val store = createStoreWithProject()
        store.enableAutoSaveProject(
            AppConf.AutoSave(target = AppConf.AutoSave.Target.Record, intervalSec = 10000),
            AppUnsavedChangesStateImpl(),
        )
        assertEquals(1, storeScope.coroutineContext.job.children.count())
        runBlocking { store.terminateAutoSaveProject() }
        assertEquals(0, storeScope.coroutineContext.job.children.count())
    }

    /* endregion */

    private fun sampleInfo(name: String, moduleName: String, lengthMillis: Float) = SampleInfo(
        name = name,
        file = baseProject.rootSampleDirectory.resolve("$moduleName/$name").absolutePath,
        moduleName = moduleName,
        sampleRate = 44100f,
        maxSampleRate = 44100,
        normalize = false,
        normalizeRatio = null,
        channels = 1,
        length = 44100,
        lengthMillis = lengthMillis,
        chunkSize = 44100,
        chunkCount = 1,
        hasSpectrogram = false,
        hasPower = false,
        powerChannels = 0,
        hasFundamental = false,
        lastModified = 0L,
        algorithmVersion = 0,
    )

    companion object {

        private val tempDir: File by lazy {
            createTempDirectory("vlabeler-test").toFile().also { dir ->
                Runtime.getRuntime().addShutdownHook(Thread { dir.deleteRecursively() })
            }
        }

        /**
         * A real project created once and shared by all tests, since [Project] is immutable. It contains two modules
         * "A3" (entries "- aA3", "a kaA3") and "C4" (entries "- a", "a ka", "- i", "i ki").
         */
        private val baseProject: Project by lazy {
            val sampleDir = TestFixtures.deploy(
                "utau-singer",
                tempDir.resolve("utau-singer"),
                wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
            )
            createTestProject(labeler = TestLabelers.utauSinger, sampleDirectory = sampleDir)
        }

        /**
         * A single-module project (oto fixture, 2 entries) whose module directory is the root sample directory.
         */
        private val singleModuleProject: Project by lazy {
            val sampleDir = TestFixtures.deploy(
                "oto",
                tempDir.resolve("oto"),
                wavFiles = listOf("_a_ka.wav"),
            )
            createTestProject(
                labeler = TestLabelers.utauOto,
                sampleDirectory = sampleDir,
                inputFilePath = sampleDir.resolve("oto.ini").absolutePath,
            )
        }
    }
}
