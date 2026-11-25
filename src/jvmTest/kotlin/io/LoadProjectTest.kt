package io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.ProjectParseException
import com.sdercolin.vlabeler.exception.RequiredLabelerNotFoundException
import com.sdercolin.vlabeler.io.awaitLoadProject
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.stringStatic
import com.sdercolin.vlabeler.util.CustomLabelerDir
import com.sdercolin.vlabeler.util.stringifyJson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import testutil.TestEnv
import testutil.TestFixtures
import testutil.TestLabelers
import testutil.createTestProject
import java.io.File
import java.util.Collections
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [awaitLoadProject]: the happy path, the labeler-version reconciliation branches (newer/equal/older than the
 * project's), the [RequiredLabelerNotFoundException] and labeler auto-install branches, the parse-error and
 * missing-file branches, and the auto-saved path.
 */
class LoadProjectTest {

    private lateinit var tempDir: File
    private lateinit var sampleDir: File
    private lateinit var scope: CoroutineScope

    @BeforeTest
    fun setup() {
        Log.muted = true
        TestEnv.ensureLogDirectory()
        scope = CoroutineScope(SupervisorJob())
        tempDir = createTempDirectory("vlabeler-test").toFile()
        sampleDir = TestFixtures.deploy(
            "oto",
            tempDir.resolve("oto"),
            wavFiles = listOf("_a_ka.wav"),
        )
    }

    @AfterTest
    fun teardown() {
        scope.cancel()
        Log.muted = false
        tempDir.deleteRecursively()
    }

    private fun createProject(): Project = createTestProject(
        labeler = TestLabelers.utauOto,
        sampleDirectory = sampleDir,
        inputFilePath = sampleDir.resolve("oto.ini").absolutePath,
    )

    private fun write(project: Project, file: File = project.projectFile): File {
        file.parentFile.mkdirs()
        file.writeText(project.stringifyJson())
        return file
    }

    /**
     * Runs [awaitLoadProject] while consuming any snackbars it raises (they use [SnackbarDuration.Indefinite] and would
     * otherwise suspend forever without a UI host consuming them). Returns the messages that were shown, in order.
     */
    private fun load(file: File, appState: AppState, autoSaved: Boolean = false): List<String> = runBlocking {
        val messages = Collections.synchronizedList(mutableListOf<String>())
        val dismisser = launch(Dispatchers.Default) {
            var last: Any? = null
            while (isActive) {
                val data = appState.snackbarHostState.currentSnackbarData
                if (data != null && data !== last) {
                    messages.add(data.message)
                    last = data
                    data.dismiss()
                } else {
                    delay(2)
                }
            }
        }
        try {
            withContext(Dispatchers.IO) { awaitLoadProject(file, appState, autoSaved) }
        } finally {
            dismisser.cancel()
        }
        messages.toList()
    }

    @Test
    fun testLoadWithSameLabelerVersion() {
        val project = createProject()
        val file = write(project)
        val appState = TestAppState()

        val messages = load(file, appState)

        assertEquals(emptyList(), messages)
        assertNull(appState.error)
        assertTrue(appState.hasProject)
        assertEquals(project.projectName, appState.project?.projectName)
        assertFalse(appState.isBusy)
        // the existing labeler (equal version) is used as-is
        assertEquals(TestLabelers.utauOto.name, appState.project?.labelerConf?.name)
        assertEquals(TestLabelers.utauOto.version, appState.project?.labelerConf?.version)
    }

    @Test
    fun testLoadWithNewerExistingLabeler() {
        // the project stores an older labeler version; the newer available labeler wins with no snackbar
        val project = createProject()
        val onDisk = project.copy(originalLabelerConf = project.originalLabelerConf.copy(version = 1))
        val file = write(onDisk)
        val appState = TestAppState(availableLabelerConfs = listOf(TestLabelers.utauOto))

        val messages = load(file, appState)

        assertEquals(emptyList(), messages)
        assertNull(appState.error)
        assertTrue(appState.hasProject)
        // the newer available labeler (version 30) replaces the older one stored in the project (version 1)
        assertEquals(TestLabelers.utauOto.version, appState.project?.labelerConf?.version)
    }

    @Test
    fun testLoadWithOlderExistingLabelerInstallsUpdate() {
        // the available labeler is older than the project's -> the project's labeler is installed and a warning shown
        val olderAvailable = TestLabelers.utauOto.copy(version = 1)
        val project = createProject()
        val file = write(project)
        val appState = TestAppState(availableLabelerConfs = listOf(olderAvailable))

        val messages = load(file, appState)

        assertEquals(1, messages.size)
        assertTrue(messages.single().isNotBlank())
        assertNull(appState.error)
        assertTrue(appState.hasProject)
        // the project keeps its own (newer) labeler
        assertEquals(TestLabelers.utauOto.version, appState.project?.labelerConf?.version)
    }

    @Test
    fun testLoadInstallsMissingLabelerWithoutResources() {
        // the labeler is unknown but ships no resource files -> it is auto-installed and a warning is shown
        val project = createProject()
        val renamed = project.copy(
            originalLabelerConf = project.originalLabelerConf.copy(name = "custom-oto-test"),
        )
        val file = write(renamed)
        val appState = TestAppState(availableLabelerConfs = listOf(TestLabelers.nnsvsSinger))

        val messages = load(file, appState)

        assertEquals(1, messages.size)
        assertTrue(messages.single().isNotBlank())
        assertNull(appState.error)
        assertTrue(appState.hasProject)
        assertEquals("custom-oto-test", appState.project?.labelerConf?.name)
        // the labeler was written into the custom labeler directory
        assertTrue(CustomLabelerDir.resolve("custom-oto-test-labeler").isDirectory)
    }

    @Test
    fun testLoadThrowsWhenMissingLabelerHasResources() {
        // the labeler is unknown and requires resource files -> it cannot be auto-installed
        val project = createProject()
        val onDisk = project.copy(
            originalLabelerConf = project.originalLabelerConf.copy(
                name = "missing-oto-test",
                resourceFiles = listOf("resource.bin"),
            ),
        )
        val file = write(onDisk)
        val appState = TestAppState(availableLabelerConfs = listOf(TestLabelers.nnsvsSinger))

        assertFailsWith<RequiredLabelerNotFoundException> { load(file, appState) }
        assertFalse(appState.hasProject)
    }

    @Test
    fun testLoadThrowsWhenOlderLabelerHasResources() {
        // the available labeler is older AND the project's labeler requires resource files -> cannot install
        val olderAvailable = TestLabelers.utauOto.copy(version = 1)
        val project = createProject()
        val onDisk = project.copy(
            originalLabelerConf = project.originalLabelerConf.copy(resourceFiles = listOf("resource.bin")),
        )
        val file = write(onDisk)
        val appState = TestAppState(availableLabelerConfs = listOf(olderAvailable))

        assertFailsWith<RequiredLabelerNotFoundException> { load(file, appState) }
        assertFalse(appState.hasProject)
    }

    @Test
    fun testLoadShowsErrorOnInvalidProjectFile() {
        val file = tempDir.resolve("broken.lbp")
        file.writeText("{ not a valid project }")
        val appState = TestAppState()

        val messages = load(file, appState)

        assertEquals(emptyList(), messages)
        assertFalse(appState.hasProject)
        assertIs<ProjectParseException>(appState.error)
        assertFalse(appState.isBusy)
    }

    @Test
    fun testLoadShowsSnackbarWhenFileMissing() {
        val file = tempDir.resolve("does-not-exist.lbp")
        val appState = TestAppState()

        val messages = load(file, appState)

        assertEquals(listOf(stringStatic(Strings.StarterRecentDeleted)), messages)
        assertFalse(appState.hasProject)
    }

    @Test
    fun testLoadResetsCacheDirectoryWhenParentMissing() {
        // the stored cache directory's parent no longer exists -> it is reset to the default and a warning is shown
        val project = createProject()
        val onDisk = project.copy(
            cacheDirectoryPath = tempDir.resolve("gone").resolve("caches").absolutePath,
        )
        val file = write(onDisk)
        val appState = TestAppState()

        val messages = load(file, appState)

        assertEquals(1, messages.size)
        assertTrue(messages.single().isNotBlank())
        assertTrue(appState.hasProject)
        val expectedCache = Project.getDefaultCacheDirectory(project.workingDirectoryPath, project.projectName)
        assertEquals(expectedCache, appState.project?.cacheDirectoryPath)
    }

    @Test
    fun testLoadRedirectsProjectPathWhenFileMoved() {
        // the project file is loaded from a path different from the one recorded inside it -> paths are redirected
        val project = createProject()
        val movedFile = tempDir.resolve("moved").resolve("renamed-project.lbp")
        write(project, movedFile)
        val appState = TestAppState()

        val messages = load(movedFile, appState)

        assertEquals(emptyList(), messages)
        assertTrue(appState.hasProject)
        assertEquals("renamed-project", appState.project?.projectName)
        assertEquals(movedFile.parentFile.absolutePath, appState.project?.workingDirectory?.absolutePath)
    }

    @Test
    fun testLoadAutoSavedProject() {
        val project = createProject()
        val file = write(project)
        val appState = TestAppState()

        val messages = load(file, appState, autoSaved = true)

        assertEquals(emptyList(), messages)
        assertNull(appState.error)
        assertTrue(appState.hasProject)
        assertEquals(project.projectName, appState.project?.projectName)
    }

    private fun TestAppState(
        availableLabelerConfs: List<LabelerConf> = listOf(TestLabelers.utauOto, TestLabelers.nnsvsSinger),
    ): AppState = testutil.TestAppState.create(scope = scope, availableLabelerConfs = availableLabelerConfs)
}
