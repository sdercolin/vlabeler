package ui

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.Sample
import com.sdercolin.vlabeler.io.loadPlugins
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.starter.PathPicker
import com.sdercolin.vlabeler.ui.starter.ProjectCreatorState
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.util.AppRecordFile
import com.sdercolin.vlabeler.util.HomeDir
import com.sdercolin.vlabeler.util.toParamMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import testutil.TestEnv
import testutil.TestFixtures
import testutil.TestLabelers
import java.io.File
import java.nio.charset.Charset
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
 * Tests for [ProjectCreatorState].
 *
 * [ProjectCreatorState] only dereferences its [AppState] in the `appConf` getter and in `create()`
 * (`showError`/`onCreateProject`), none of which are exercised here, so the tests pass an uninitialized [AppState]
 * instance allocated without running its constructor (a real construction would start IPC/audio side effects).
 *
 * The [AppRecordStore] is constructed with an already-cancelled scope so that its `collectAndWrite()` loop never
 * runs and nothing is ever written to the real application directory. This safety assumption is itself asserted in
 * [app record store on cancelled scope never updates or writes the record file].
 */
class ProjectCreatorStateTest {

    private lateinit var scope: CoroutineScope
    private val tempDirs = mutableListOf<File>()

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @AfterTest
    fun teardown() {
        scope.cancel()
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

    private fun createState(
        appRecord: AppRecord = AppRecord(),
        labelers: List<LabelerConf> = defaultLabelers,
        plugins: List<Plugin> = emptyList(),
        initialFile: File? = null,
    ): ProjectCreatorState = ProjectCreatorState(
        uninitializedAppState(),
        scope,
        labelers,
        plugins,
        AppRecordStore(appRecord, cancelledScope()),
        initialFile,
    )

    /** Waits until all coroutines launched on [scope] by the state under test have finished. */
    private fun awaitIdle() = runBlocking {
        while (true) {
            val children = scope.coroutineContext.job.children.toList()
            if (children.isEmpty()) break
            children.joinAll()
        }
    }

    /* region AppRecordStore safety */

    @Test
    fun `app record store on cancelled scope never updates or writes the record file`() {
        val before = Triple(AppRecordFile.exists(), AppRecordFile.lastModified(), AppRecordFile.length())
        val store = AppRecordStore(AppRecord(), cancelledScope())
        store.update { copy(autoExport = true) }
        // longer than the 500 ms write throttle in AppRecordStore
        Thread.sleep(800)
        assertEquals(AppRecord(), store.value)
        val after = Triple(AppRecordFile.exists(), AppRecordFile.lastModified(), AppRecordFile.length())
        assertEquals(before, after)
    }

    /* endregion */

    /* region Page */

    @Test
    fun `page enum orders directory labeler and data source`() {
        assertEquals(ProjectCreatorState.Page.Labeler, ProjectCreatorState.Page.Directory.next())
        assertEquals(ProjectCreatorState.Page.DataSource, ProjectCreatorState.Page.Labeler.next())
        assertNull(ProjectCreatorState.Page.DataSource.next())
        assertNull(ProjectCreatorState.Page.Directory.previous())
        assertEquals(ProjectCreatorState.Page.Directory, ProjectCreatorState.Page.Labeler.previous())
        assertEquals(ProjectCreatorState.Page.Labeler, ProjectCreatorState.Page.DataSource.previous())
    }

    @Test
    fun `navigation moves between pages`() {
        val state = createState()
        assertEquals(ProjectCreatorState.Page.Directory, state.page)
        assertFalse(state.hasPrevious)
        assertTrue(state.hasNext)

        state.goNext()
        assertEquals(ProjectCreatorState.Page.Labeler, state.page)
        assertTrue(state.hasPrevious)
        assertTrue(state.hasNext)

        state.goNext()
        assertEquals(ProjectCreatorState.Page.DataSource, state.page)
        assertTrue(state.hasPrevious)
        assertFalse(state.hasNext)

        state.goPrevious()
        assertEquals(ProjectCreatorState.Page.Labeler, state.page)
        state.goPrevious()
        assertEquals(ProjectCreatorState.Page.Directory, state.page)
        // no previous page: stays
        state.goPrevious()
        assertEquals(ProjectCreatorState.Page.Directory, state.page)
    }

    @Test
    fun `detail expanded state is restored from app record per page`() {
        val record = AppRecord(projectCreatorDetailsExpanded = listOf(true, false, true))
        val state = createState(appRecord = record)
        assertTrue(state.isDetailExpanded)
        state.goNext()
        assertFalse(state.isDetailExpanded)
        state.goNext()
        assertTrue(state.isDetailExpanded)
    }

    @Test
    fun `toggle detail expanded only affects the current page`() {
        val state = createState()
        assertFalse(state.isDetailExpanded)
        state.toggleDetailExpanded()
        assertTrue(state.isDetailExpanded)
        state.goNext()
        assertFalse(state.isDetailExpanded)
        state.goPrevious()
        assertTrue(state.isDetailExpanded)
    }

    /* endregion */

    /* region Directory page */

    @Test
    fun `directories default to home directory when app record is empty`() {
        val state = createState()
        assertEquals(HomeDir.absolutePath, state.sampleDirectory)
        assertEquals(HomeDir.absolutePath, state.workingDirectory)
        assertEquals("", state.projectName)
        assertEquals("", state.cacheDirectory)
    }

    @Test
    fun `directories are restored from app record`() {
        val sample = newTempDir()
        val working = newTempDir()
        val state = createState(
            appRecord = AppRecord(sampleDirectory = sample.absolutePath, workingDirectory = working.absolutePath),
        )
        assertEquals(sample.absolutePath, state.sampleDirectory)
        assertEquals(working.absolutePath, state.workingDirectory)
    }

    @Test
    fun `update sample directory fills project name working directory and cache directory`() {
        val state = createState()
        val dir = newTempDir().resolve("MySinger").apply { mkdir() }

        state.updateSampleDirectory(dir.absolutePath)

        assertEquals(dir.absolutePath, state.sampleDirectory)
        assertEquals("MySinger", state.projectName)
        assertEquals(dir.absolutePath, state.workingDirectory)
        assertEquals(
            Project.getDefaultCacheDirectory(dir.absolutePath, "MySinger"),
            state.cacheDirectory,
        )
    }

    @Test
    fun `update sample directory to home clears the default project name`() {
        val state = createState()
        val dir = newTempDir().resolve("MySinger").apply { mkdir() }
        state.updateSampleDirectory(dir.absolutePath)

        state.updateSampleDirectory(HomeDir.absolutePath)

        assertEquals("", state.projectName)
        assertEquals(HomeDir.absolutePath, state.workingDirectory)
        // the previously filled cache directory is kept because the default filler skips empty project names
        assertEquals(Project.getDefaultCacheDirectory(dir.absolutePath, "MySinger"), state.cacheDirectory)
    }

    @Test
    fun `project name edited by user is kept when sample directory changes`() {
        val state = createState()
        state.updateProjectName("custom-name")
        val dir = newTempDir().resolve("MySinger").apply { mkdir() }

        state.updateSampleDirectory(dir.absolutePath)

        assertEquals("custom-name", state.projectName)
        assertEquals(Project.getDefaultCacheDirectory(dir.absolutePath, "custom-name"), state.cacheDirectory)
    }

    @Test
    fun `edited working directory is kept when sample directory changes`() {
        val state = createState()
        val working = newTempDir().resolve("workspace").apply { mkdir() }
        val sample = newTempDir().resolve("MySinger").apply { mkdir() }

        state.updateWorkingDirectory(working.absolutePath)
        state.updateSampleDirectory(sample.absolutePath)

        assertEquals(working.absolutePath, state.workingDirectory)
        assertEquals("MySinger", state.projectName)
        // the not-yet-edited cache directory follows the edited working directory and the refilled project name
        assertEquals(
            Project.getDefaultCacheDirectory(working.absolutePath, "MySinger"),
            state.cacheDirectory,
        )
    }

    @Test
    fun `update working directory refreshes the default cache directory`() {
        val state = createState()
        state.updateProjectName("proj")
        val working = newTempDir()

        state.updateWorkingDirectory(working.absolutePath)

        assertEquals(working.absolutePath, state.workingDirectory)
        assertEquals(Project.getDefaultCacheDirectory(working.absolutePath, "proj"), state.cacheDirectory)
    }

    @Test
    fun `edited cache directory is kept on further changes`() {
        val state = createState()
        val cache = newTempDir()
        state.updateCacheDirectory(cache.absolutePath)

        state.updateProjectName("proj")
        state.updateWorkingDirectory(newTempDir().absolutePath)
        state.updateSampleDirectory(newTempDir().resolve("MySinger").apply { mkdir() }.absolutePath)

        assertEquals(cache.absolutePath, state.cacheDirectory)
    }

    @Test
    fun `initial project file fills sample directory and project name`() {
        val dir = newTempDir()
        val projectFile = dir.resolve("my-project.${Project.PROJECT_FILE_EXTENSION}")
        val state = createState(initialFile = projectFile)

        assertEquals(dir.absolutePath, state.sampleDirectory)
        assertEquals("my-project", state.projectName)
    }

    @Test
    fun `initial project file name is not treated as a user edit`() {
        val dir = newTempDir()
        val projectFile = dir.resolve("my-project.${Project.PROJECT_FILE_EXTENSION}")
        val state = createState(initialFile = projectFile)

        val other = newTempDir().resolve("OtherSinger").apply { mkdir() }
        state.updateSampleDirectory(other.absolutePath)

        assertEquals("OtherSinger", state.projectName)
    }

    @Test
    fun `initial directory fills sample directory and derives project name`() {
        val dir = newTempDir().resolve("MySinger").apply { mkdir() }
        val state = createState(initialFile = dir)

        assertEquals(dir.absolutePath, state.sampleDirectory)
        assertEquals("MySinger", state.projectName)
    }

    @Test
    fun `sample directory validation`() {
        val state = createState()
        val dir = newTempDir()
        state.updateSampleDirectory(dir.absolutePath)
        assertTrue(state.isSampleDirectoryValid())

        state.updateSampleDirectory(dir.resolve("not-existing").absolutePath)
        assertFalse(state.isSampleDirectoryValid())

        val file = dir.resolve("file.txt").apply { writeText("") }
        state.updateSampleDirectory(file.absolutePath)
        assertFalse(state.isSampleDirectoryValid())
    }

    @Test
    fun `project name validation`() {
        val state = createState()
        state.updateProjectName("valid-name")
        assertTrue(state.isProjectNameValid())

        state.updateProjectName("in/valid")
        assertFalse(state.isProjectNameValid())

        state.updateProjectName("")
        assertFalse(state.isProjectNameValid())

        state.updateProjectName("  ")
        assertFalse(state.isProjectNameValid())
    }

    @Test
    fun `working directory validation requires an existing directory`() {
        val state = createState()
        val dir = newTempDir()
        state.updateWorkingDirectory(dir.absolutePath)
        assertTrue(state.isWorkingDirectoryValid())

        state.updateWorkingDirectory(dir.resolve("not-existing").absolutePath)
        assertFalse(state.isWorkingDirectoryValid())

        state.updateWorkingDirectory(dir.resolve("no-parent").resolve("child").absolutePath)
        assertFalse(state.isWorkingDirectoryValid())
    }

    @Test
    fun `project file existing check`() {
        val state = createState()
        val dir = newTempDir()
        state.updateWorkingDirectory(dir.absolutePath)
        state.updateProjectName("proj")
        assertFalse(state.isProjectFileExisting())

        dir.resolve("proj.${Project.PROJECT_FILE_EXTENSION}").writeText("{}")
        assertTrue(state.isProjectFileExisting())
    }

    @Test
    fun `cache directory validation`() {
        val state = createState()
        val working = newTempDir()
        state.updateWorkingDirectory(working.absolutePath)

        // not existing yet, but directly under the working directory
        state.updateCacheDirectory(working.resolve("proj.lbp.caches").absolutePath)
        assertTrue(state.isCacheDirectoryValid())

        // existing directory elsewhere
        state.updateCacheDirectory(newTempDir().resolve("caches").apply { mkdir() }.absolutePath)
        assertTrue(state.isCacheDirectoryValid())

        // parent does not exist and is not the working directory
        state.updateCacheDirectory(newTempDir().resolve("missing").resolve("caches").absolutePath)
        assertFalse(state.isCacheDirectoryValid())

        // existing plain file
        val file = working.resolve("file.txt").apply { writeText("") }
        state.updateCacheDirectory(file.absolutePath)
        assertFalse(state.isCacheDirectoryValid())

        state.updateCacheDirectory("")
        assertFalse(state.isCacheDirectoryValid())
    }

    @Test
    fun `has error reflects directory page validity`() {
        val state = createState()
        val dir = newTempDir().resolve("MySinger").apply { mkdir() }
        state.updateSampleDirectory(dir.absolutePath)
        assertFalse(state.hasError)

        state.updateProjectName("in/valid")
        assertTrue(state.hasError)
    }

    /* endregion */

    /* region Labeler page */

    @Test
    fun `labeler categories put built-in categories first and empty category last`() {
        val state = createState(
            labelers = listOf(
                TestLabelers.audacity,
                TestLabelers.utauOto.copy(categoryTag = "Zebra"),
                TestLabelers.nnsvsSinger,
                TestLabelers.utauOto.copy(categoryTag = "Apple"),
                TestLabelers.utauSinger,
            ),
        )
        assertEquals(listOf("UTAU", "NNSVS", "Apple", "Zebra", ""), state.labelerCategories.toList())
    }

    @Test
    fun `initial category and labeler fall back to the first available`() {
        val state = createState()
        assertEquals("UTAU", state.labelerCategory)
        // utau-singer.default has displayOrder 0, oto-plus.default has 1
        assertEquals(TestLabelers.utauSinger.name, state.labeler.name)
    }

    @Test
    fun `selectable labelers are sorted by display order then name`() {
        val state = createState()
        assertEquals(
            listOf(TestLabelers.utauSinger.name, TestLabelers.utauOto.name),
            state.selectableLabelers.map { it.name },
        )

        val tieState = createState(labelers = listOf(TestLabelers.sinsy, TestLabelers.audacity))
        assertEquals(
            listOf(TestLabelers.audacity.name, TestLabelers.sinsy.name),
            tieState.selectableLabelers.map { it.name },
        )
    }

    @Test
    fun `initial labeler is restored from app record`() {
        val state = createState(
            appRecord = AppRecord(labelerCategory = "UTAU", labelerName = TestLabelers.utauOto.name),
        )
        assertEquals("UTAU", state.labelerCategory)
        assertEquals(TestLabelers.utauOto.name, state.labeler.name)
    }

    @Test
    fun `recorded labeler name of another category falls back to first selectable`() {
        val state = createState(
            appRecord = AppRecord(labelerCategory = "NNSVS", labelerName = TestLabelers.utauOto.name),
        )
        assertEquals("NNSVS", state.labelerCategory)
        assertEquals(TestLabelers.nnsvsSinger.name, state.labeler.name)
    }

    @Test
    fun `update labeler category selects first labeler and remembers previous selections`() {
        val state = createState(appRecord = AppRecord(sampleDirectory = newTempDir().absolutePath))
        state.updateLabeler(TestLabelers.utauOto)
        awaitIdle()
        assertEquals(TestLabelers.utauOto.name, state.labeler.name)

        state.updateLabelerCategory("NNSVS")
        awaitIdle()
        assertEquals("NNSVS", state.labelerCategory)
        assertEquals(TestLabelers.nnsvsSinger.name, state.labeler.name)

        state.updateLabelerCategory("UTAU")
        awaitIdle()
        assertEquals(TestLabelers.utauOto.name, state.labeler.name)
    }

    @Test
    fun `update labeler category to the current category keeps the labeler`() {
        val state = createState()
        state.updateLabelerCategory("UTAU")
        assertEquals(TestLabelers.utauSinger.name, state.labeler.name)
    }

    @Test
    fun `update labeler loads params`() {
        val state = createState(appRecord = AppRecord(sampleDirectory = newTempDir().absolutePath))
        assertNull(state.labelerParams)

        state.updateLabeler(TestLabelers.utauOto)
        awaitIdle()

        assertEquals(TestLabelers.utauOto.name, state.labeler.name)
        assertNotNull(state.labelerParams)
        assertEquals(state.labelerParams, state.labelerSavedParams)
    }

    @Test
    fun `update labeler params flags invalid values`() {
        val state = createState()
        val labeler = state.labeler

        state.updateLabelerParams(labeler.getDefaultParams())
        assertFalse(state.labelerError)

        val invalid = (labeler.getDefaultParams() + mapOf("dragBase" to "NotAnOption")).toParamMap()
        state.updateLabelerParams(invalid)
        assertTrue(state.labelerError)

        state.goNext()
        assertEquals(ProjectCreatorState.Page.Labeler, state.page)
        assertTrue(state.hasError)
    }

    @Test
    fun `update labeler with self-constructed labeler resets content type and encoding`() {
        val state = createState(
            appRecord = AppRecord(projectContentType = ProjectCreatorState.ContentType.File),
            labelers = listOf(TestLabelers.audacity),
        )
        assertEquals(ProjectCreatorState.ContentType.File, state.contentType)
        assertEquals("UTF-8", state.encoding)

        state.updateLabeler(TestLabelers.utauSinger)
        awaitIdle()

        assertEquals(ProjectCreatorState.ContentType.Default, state.contentType)
        assertEquals("Shift-JIS", state.encoding)
    }

    @Test
    fun `update labeler autofills the default input file from the sample directory`() {
        val sampleDirectory = TestFixtures.deploy("oto", newTempDir())
        val state = createState(appRecord = AppRecord(sampleDirectory = sampleDirectory.absolutePath))

        state.updateLabeler(TestLabelers.utauOto)
        awaitIdle()

        assertEquals(sampleDirectory.resolve("oto.ini").absolutePath, state.inputFile)
    }

    @Test
    fun `input file edited by user is not overwritten by labeler autofill`() {
        val sampleDirectory = TestFixtures.deploy("oto", newTempDir())
        val state = createState(appRecord = AppRecord(sampleDirectory = sampleDirectory.absolutePath))
        val ownFile = newTempDir().resolve("own.ini").apply { writeText("") }

        state.updateInputFile(ownFile.absolutePath, editedByUser = true, detectEncoding = false)
        awaitIdle()
        state.updateLabeler(TestLabelers.utauOto)
        awaitIdle()

        assertEquals(ownFile.absolutePath, state.inputFile)
    }

    /* endregion */

    /* region Data source page */

    @Test
    fun `selectable content types depend on the labeler kind`() {
        val state = createState()
        // default labeler utau-singer is self-constructed
        assertEquals(
            listOf(ProjectCreatorState.ContentType.Default, ProjectCreatorState.ContentType.Plugin),
            state.selectableContentTypes,
        )

        val plainState = createState(labelers = listOf(TestLabelers.audacity))
        assertEquals(ProjectCreatorState.ContentType.entries.toList(), plainState.selectableContentTypes)
    }

    @Test
    fun `initial content type is taken from app record only when selectable`() {
        val record = AppRecord(projectContentType = ProjectCreatorState.ContentType.File)
        val selfConstructedState = createState(appRecord = record)
        assertEquals(ProjectCreatorState.ContentType.Default, selfConstructedState.contentType)

        val plainState = createState(appRecord = record, labelers = listOf(TestLabelers.audacity))
        assertEquals(ProjectCreatorState.ContentType.File, plainState.contentType)
    }

    @Test
    fun `select content type plugin picks the first supported plugin`() {
        val state = createState(
            appRecord = AppRecord(
                sampleDirectory = newTempDir().absolutePath,
                labelerCategory = "UTAU",
                labelerName = TestLabelers.utauOto.name,
            ),
            plugins = allTemplatePlugins,
        )
        state.selectContentType(ProjectCreatorState.ContentType.Plugin, Language.English)
        awaitIdle()

        assertEquals(ProjectCreatorState.ContentType.Plugin, state.contentType)
        val plugin = assertNotNull(state.templatePlugin)
        assertTrue(plugin.isLabelFileExtensionSupported("ini"))
    }

    @Test
    fun `select content type plugin without available plugins keeps none selected`() {
        val state = createState()
        state.selectContentType(ProjectCreatorState.ContentType.Plugin, Language.English)
        awaitIdle()

        assertEquals(ProjectCreatorState.ContentType.Plugin, state.contentType)
        assertNull(state.templatePlugin)

        // an empty plugin selection makes the data source page invalid
        state.goNext()
        state.goNext()
        assertEquals(ProjectCreatorState.Page.DataSource, state.page)
        assertTrue(state.hasError)
    }

    @Test
    fun `supported plugins are filtered by the labeler extension`() {
        val state = createState(
            appRecord = AppRecord(labelerCategory = "UTAU", labelerName = TestLabelers.utauOto.name),
            plugins = allTemplatePlugins,
        )
        val supported = state.getSupportedPlugins(Language.English).map { it.name }

        listOf("cv-oto-gen", "cvvc-oto-gen", "vcv-oto-gen", "regex-raw-gen").forEach {
            assertTrue(it in supported, "expected $it to support .ini")
        }
        listOf("ust2lab-ja-kana", "audacity2lab", "lab2audacity").forEach {
            assertFalse(it in supported, "expected $it not to support .ini")
        }
    }

    @Test
    fun `update plugin loads its params`() {
        val state = createState(
            appRecord = AppRecord(
                sampleDirectory = newTempDir().absolutePath,
                labelerCategory = "UTAU",
                labelerName = TestLabelers.utauOto.name,
            ),
            plugins = allTemplatePlugins,
        )
        val plugin = allTemplatePlugins.first { it.name == "cvvc-oto-gen" }

        state.updatePlugin(plugin)
        awaitIdle()

        assertEquals(plugin, state.templatePlugin)
        assertNotNull(state.templatePluginParams)
        assertEquals(state.templatePluginParams, state.templatePluginSavedParams)
        assertNull(state.warningText)
    }

    @Test
    fun `update plugin on a self-constructed labeler shows a warning`() {
        val state = createState(plugins = allTemplatePlugins)
        val plugin = allTemplatePlugins.first { it.name == "cvvc-oto-gen" }

        state.updatePlugin(plugin)
        awaitIdle()

        assertEquals(Strings.StarterNewWarningSelfConstructedLabelerWithTemplatePlugin, state.warningText)

        state.updatePlugin(null)
        assertNull(state.templatePlugin)
        assertNull(state.templatePluginParams)
        assertNull(state.templatePluginSavedParams)
        assertFalse(state.templatePluginError)
        assertNull(state.warningText)
    }

    @Test
    fun `update labeler clears a plugin that does not support the new extension`() {
        val state = createState(
            appRecord = AppRecord(
                sampleDirectory = newTempDir().absolutePath,
                labelerCategory = "UTAU",
                labelerName = TestLabelers.utauOto.name,
            ),
            plugins = allTemplatePlugins,
        )
        state.updatePlugin(allTemplatePlugins.first { it.name == "cvvc-oto-gen" })
        awaitIdle()
        assertNotNull(state.templatePlugin)

        // nnsvs-singer uses .lab, which cvvc-oto-gen does not support
        state.updateLabeler(TestLabelers.nnsvsSinger)
        awaitIdle()

        assertNull(state.templatePlugin)
    }

    @Test
    fun `update plugin params flags invalid values`() {
        val state = createState(
            appRecord = AppRecord(
                sampleDirectory = newTempDir().absolutePath,
                labelerCategory = "UTAU",
                labelerName = TestLabelers.utauOto.name,
            ),
            plugins = allTemplatePlugins,
        )
        val plugin = allTemplatePlugins.first { it.name == "cvvc-oto-gen" }
        state.updatePlugin(plugin)
        awaitIdle()

        state.updatePluginParams(plugin.getDefaultParams())
        assertFalse(state.templatePluginError)

        // bpm has min 0
        val invalid = (plugin.getDefaultParams() + mapOf("bpm" to -1f)).toParamMap()
        state.updatePluginParams(invalid)
        assertTrue(state.templatePluginError)
    }

    @Test
    fun `encoding defaults to the labeler parser encoding`() {
        val state = createState()
        assertEquals("Shift-JIS", state.encoding)

        val plainState = createState(labelers = listOf(TestLabelers.audacity))
        assertEquals("UTF-8", plainState.encoding)
    }

    @Test
    fun `update input file detects the file encoding`() {
        val state = createState(labelers = listOf(TestLabelers.audacity))
        assertEquals("UTF-8", state.encoding)
        val text = "これはボイスバンクの音源設定ファイルのテストです。あいうえおかきくけこ。".repeat(10)
        val file = newTempDir().resolve("input.txt").apply {
            writeBytes(text.toByteArray(Charset.forName("Shift_JIS")))
        }

        state.updateInputFile(file.absolutePath, editedByUser = true)
        awaitIdle()

        assertEquals(file.absolutePath, state.inputFile)
        assertEquals("Shift-JIS", state.encoding)
    }

    @Test
    fun `input file validation checks extension and existence`() {
        val state = createState(labelers = listOf(TestLabelers.audacity))
        assertFalse(state.isInputFileValid())

        val dir = newTempDir()
        val wrongExtension = dir.resolve("labels.ini").apply { writeText("") }
        state.updateInputFile(wrongExtension.absolutePath, editedByUser = true, detectEncoding = false)
        awaitIdle()
        assertFalse(state.isInputFileValid())

        state.updateInputFile(dir.resolve("missing.txt").absolutePath, editedByUser = true, detectEncoding = false)
        awaitIdle()
        assertFalse(state.isInputFileValid())

        val valid = dir.resolve("labels.txt").apply { writeText("") }
        state.updateInputFile(valid.absolutePath, editedByUser = true, detectEncoding = false)
        awaitIdle()
        assertTrue(state.isInputFileValid())
    }

    @Test
    fun `can auto export depends on labeler and content type`() {
        // self-constructed labeler: always
        assertTrue(createState().canAutoExport)

        // labeler with a default input file path: always
        val otoState = createState(
            appRecord = AppRecord(labelerCategory = "UTAU", labelerName = TestLabelers.utauOto.name),
        )
        assertTrue(otoState.canAutoExport)

        // plain labeler without default input: only with content type File
        val plainState = createState(labelers = listOf(TestLabelers.audacity))
        assertFalse(plainState.canAutoExport)
        plainState.selectContentType(ProjectCreatorState.ContentType.File, Language.English)
        assertTrue(plainState.canAutoExport)
    }

    @Test
    fun `toggle auto export updates the state`() {
        val state = createState()
        assertFalse(state.autoExport)
        state.toggleAutoExport(true)
        assertTrue(state.autoExport)
        state.toggleAutoExport(false)
        assertFalse(state.autoExport)
    }

    /* endregion */

    /* region File pickers */

    @Test
    fun `pick methods set the current path picker`() {
        val state = createState()
        assertNull(state.currentPathPicker)
        state.pickSampleDirectory()
        assertEquals(PathPicker.SampleDirectory, state.currentPathPicker)
        state.pickWorkingDirectory()
        assertEquals(PathPicker.WorkingDirectory, state.currentPathPicker)
        state.pickCacheDirectory()
        assertEquals(PathPicker.CacheDirectory, state.currentPathPicker)
        state.pickInputFile()
        assertEquals(PathPicker.InputFile, state.currentPathPicker)
    }

    @Test
    fun `file picker modes and extensions`() {
        val state = createState()
        assertTrue(state.getFilePickerDirectoryMode(PathPicker.SampleDirectory))
        assertTrue(state.getFilePickerDirectoryMode(PathPicker.WorkingDirectory))
        assertTrue(state.getFilePickerDirectoryMode(PathPicker.CacheDirectory))
        assertFalse(state.getFilePickerDirectoryMode(PathPicker.InputFile))

        assertEquals(Sample.acceptableSampleFileExtensions, state.getFilePickerExtensions(PathPicker.SampleDirectory))
        assertNull(state.getFilePickerExtensions(PathPicker.WorkingDirectory))
        assertNull(state.getFilePickerExtensions(PathPicker.CacheDirectory))
        assertEquals(listOf(state.labeler.extension), state.getFilePickerExtensions(PathPicker.InputFile))
    }

    @Test
    fun `file picker initial directories`() {
        val state = createState(labelers = listOf(TestLabelers.audacity))
        val sample = newTempDir()
        val working = newTempDir()
        val cache = newTempDir()
        state.updateSampleDirectory(sample.absolutePath)
        state.updateWorkingDirectory(working.absolutePath)
        state.updateCacheDirectory(cache.absolutePath)

        assertEquals(sample.absolutePath, state.getFilePickerInitialDirectory(PathPicker.SampleDirectory))
        assertEquals(working.absolutePath, state.getFilePickerInitialDirectory(PathPicker.WorkingDirectory))
        assertEquals(cache.absolutePath, state.getFilePickerInitialDirectory(PathPicker.CacheDirectory))
        // no valid input file yet: falls back to the sample directory
        assertEquals(sample.absolutePath, state.getFilePickerInitialDirectory(PathPicker.InputFile))

        val inputDir = newTempDir()
        val input = inputDir.resolve("labels.txt").apply { writeText("") }
        state.updateInputFile(input.absolutePath, editedByUser = true, detectEncoding = false)
        awaitIdle()
        assertEquals(inputDir.absolutePath, state.getFilePickerInitialDirectory(PathPicker.InputFile))
    }

    @Test
    fun `handle file picker result updates the picked field`() {
        val state = createState(labelers = listOf(TestLabelers.audacity))
        val dir = newTempDir().resolve("MySinger").apply { mkdir() }

        state.pickSampleDirectory()
        state.handleFilePickerResult(PathPicker.SampleDirectory, dir.parent, dir.name)
        assertNull(state.currentPathPicker)
        assertEquals(dir.absolutePath, state.sampleDirectory)

        val input = dir.resolve("labels.txt").apply { writeText("") }
        state.pickInputFile()
        state.handleFilePickerResult(PathPicker.InputFile, input.parent, input.name)
        awaitIdle()
        assertNull(state.currentPathPicker)
        assertEquals(input.absolutePath, state.inputFile)
    }

    @Test
    fun `handle file picker result with no selection only closes the picker`() {
        val state = createState()
        val originalSampleDirectory = state.sampleDirectory
        state.pickSampleDirectory()

        state.handleFilePickerResult(PathPicker.SampleDirectory, null, null)

        assertNull(state.currentPathPicker)
        assertEquals(originalSampleDirectory, state.sampleDirectory)
    }

    /* endregion */

    companion object {

        private val defaultLabelers by lazy {
            listOf(
                TestLabelers.utauSinger,
                TestLabelers.utauOto,
                TestLabelers.nnsvsSinger,
                TestLabelers.audacity,
                TestLabelers.sinsy,
            )
        }

        private val allTemplatePlugins: List<Plugin> by lazy {
            TestEnv.ensureLogDirectory()
            loadPlugins(Plugin.Type.Template, Language.English)
        }
    }
}
