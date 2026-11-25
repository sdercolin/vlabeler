package ui

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.sample.SampleListDialogState
import com.sdercolin.vlabeler.ui.editor.EditorState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import testutil.TestAppState
import testutil.TestFixtures
import testutil.TestLabelers
import testutil.TestWav
import testutil.createTestProject
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [SampleListDialogState] using a real multi-module UTAU singer project (modules "A3" and "C4"). Missing and
 * excluded samples are simulated by editing the deployed sample directory on disk.
 */
class SampleListDialogStateTest {

    private lateinit var scope: CoroutineScope
    private lateinit var tempDir: File
    private lateinit var sampleDir: File
    private lateinit var appState: AppState
    private lateinit var editorState: EditorState

    @BeforeTest
    fun setup() {
        Log.muted = true
        scope = CoroutineScope(SupervisorJob())
        tempDir = createTempDirectory("vlabeler-test").toFile()
        sampleDir = TestFixtures.deploy(
            "utau-singer",
            tempDir.resolve("utau-singer"),
            wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
        )
        val project = createTestProject(
            labeler = TestLabelers.utauSinger,
            sampleDirectory = sampleDir,
        )
        appState = TestAppState.create(scope = scope, availableLabelerConfs = listOf(TestLabelers.utauSinger))
        appState.openEditor(project)
        editorState = requireNotNull(appState.editor)
    }

    @AfterTest
    fun teardown() {
        scope.cancel()
        Log.muted = false
        tempDir.deleteRecursively()
    }

    private val c4Dir: File get() = sampleDir.resolve("C4")

    private fun createDialog() = SampleListDialogState(editorState)

    @Test
    fun testModuleListing() {
        val dialog = createDialog()

        assertEquals(listOf("A3", "C4"), dialog.allModuleNames.sorted())
    }

    @Test
    fun testIncludedSamplesAllValidWhenFilesPresent() {
        val dialog = createDialog()
        dialog.selectModule("C4")

        assertEquals(listOf("_a_ka.wav", "_i_ki.wav"), dialog.includedSampleItems.map { it.name }.sorted())
        assertTrue(dialog.includedSampleItems.all { it.valid })
        assertTrue(dialog.includedSampleItems.all { it.entryCount == 2 })
        assertEquals(emptyList(), dialog.excludedSampleItems)
    }

    @Test
    fun testMissingSampleDetected() {
        // the entries still reference _i_ki.wav, but the file is no longer on disk
        assertTrue(c4Dir.resolve("_i_ki.wav").delete())

        val dialog = createDialog()
        dialog.selectModule("C4")

        val included = dialog.includedSampleItems.associateBy { it.name }
        assertEquals(true, included.getValue("_a_ka.wav").valid)
        assertEquals(false, included.getValue("_i_ki.wav").valid)
    }

    @Test
    fun testExcludedSampleDetected() {
        // a sample file that no entry references
        TestWav.write(c4Dir.resolve("_extra.wav"))

        val dialog = createDialog()
        dialog.selectModule("C4")

        assertEquals(listOf("_extra.wav"), dialog.excludedSampleItems.map { it.name })
        assertTrue(dialog.includedSampleItems.none { it.name == "_extra.wav" })
    }

    @Test
    fun testCreateDefaultEntriesForAllExcludedSamples() {
        TestWav.write(c4Dir.resolve("_extra.wav"))

        val dialog = createDialog()
        dialog.selectModule("C4")
        assertEquals(listOf("_extra.wav"), dialog.excludedSampleItems.map { it.name })

        dialog.createDefaultEntriesForAllExcludedSamples()

        assertEquals(emptyList(), dialog.excludedSampleItems)
        val extra = dialog.includedSampleItems.first { it.name == "_extra.wav" }
        assertEquals(1, extra.entryCount)
        assertTrue(extra.valid)
    }

    @Test
    fun testCreateDefaultEntryForSelectedSample() {
        TestWav.write(c4Dir.resolve("_extra.wav"))

        val dialog = createDialog()
        dialog.selectModule("C4")
        dialog.selectSample("_extra.wav")

        dialog.createDefaultEntry()

        val extra = dialog.includedSampleItems.first { it.name == "_extra.wav" }
        assertEquals(1, extra.entryCount)
    }

    @Test
    fun testSelectSampleAndEntry() {
        val dialog = createDialog()
        dialog.selectModule("C4")

        dialog.selectSample("_a_ka.wav")
        assertEquals("_a_ka.wav", dialog.selectedSampleName)
        assertEquals(2, dialog.entryItems.size)
        assertNull(dialog.selectedEntryIndex)

        val targetIndex = dialog.entryItems.first().entry.index
        dialog.selectEntry(targetIndex)
        assertEquals(targetIndex, dialog.selectedEntryIndex)

        // jumping moves the project's current entry in the C4 module
        dialog.jumpToSelectedEntry()
        val c4 = appState.requireProject().modules.first { it.name == "C4" }
        assertEquals(targetIndex, c4.currentIndex)
    }

    @Test
    fun testSelectModuleClearsSelections() {
        val dialog = createDialog()
        dialog.selectModule("C4")
        dialog.selectSample("_a_ka.wav")
        dialog.selectEntry(dialog.entryItems.first().entry.index)

        dialog.selectModule("A3")

        assertNull(dialog.selectedSampleName)
        assertNull(dialog.selectedEntryIndex)
        assertEquals(emptyList(), dialog.entryItems)
    }

    @Test
    fun testSampleDirectoryHelpers() {
        val dialog = createDialog()
        dialog.selectModule("C4")

        assertEquals(c4Dir.absolutePath, dialog.sampleDirectory.absolutePath)
        assertTrue(dialog.isSampleDirectoryExisting())
        assertEquals(c4Dir.absolutePath, dialog.getInitialSampleDirectoryForRedirection())
    }

    @Test
    fun testRedirectionDialogFlagAndNoOpResults() {
        val dialog = createDialog()
        dialog.selectModule("C4")
        val before = dialog.sampleDirectory.absolutePath

        dialog.requestRedirectSampleDirectory()
        assertTrue(dialog.isShowingSampleDirectoryRedirectDialog)

        // cancelling (null result) closes the dialog and changes nothing
        dialog.handleRedirectionDialogResult(null, null)
        assertFalse(dialog.isShowingSampleDirectoryRedirectDialog)
        assertEquals(before, dialog.sampleDirectory.absolutePath)
    }

    @Test
    fun testRedirectionToExistingDirectory() {
        val newDir = tempDir.resolve("redirected").apply { mkdirs() }
        TestWav.write(newDir.resolve("_a_ka.wav"))

        val dialog = createDialog()
        dialog.selectModule("C4")

        dialog.handleRedirectionDialogResult(tempDir.absolutePath, "redirected")

        assertFalse(dialog.isShowingSampleDirectoryRedirectDialog)
        assertEquals(newDir.canonicalPath, dialog.sampleDirectory.canonicalPath)
    }
}
