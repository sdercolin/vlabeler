package ui

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.ImportedModule
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.importentries.ImportEntriesDialogState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import testutil.TestAppState
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [ImportEntriesDialogState], focusing on how imported modules are mapped to the existing modules of a real
 * project ("A3", "C4" from the UTAU singer fixture) and how compatibility and name matching drive the default
 * selection.
 */
class ImportEntriesDialogStateTest {

    private lateinit var scope: CoroutineScope
    private lateinit var tempDir: File
    private lateinit var appState: AppState

    @BeforeTest
    fun setup() {
        Log.muted = true
        scope = CoroutineScope(SupervisorJob())
        tempDir = createTempDirectory("vlabeler-test").toFile()
        val sampleDir = TestFixtures.deploy(
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
    }

    @AfterTest
    fun teardown() {
        scope.cancel()
        Log.muted = false
        tempDir.deleteRecursively()
    }

    private val labeler: LabelerConf get() = appState.requireProject().labelerConf

    private fun sampleEntry(name: String): Entry =
        appState.requireProject().modules.first { it.name == "C4" }.entries.first().copy(name = name)

    private fun compatibleModule(name: String, entries: List<Entry> = listOf(sampleEntry("imported"))) =
        ImportedModule(
            name = name,
            entries = entries,
            pointSize = labeler.fields.size,
            extraSize = labeler.extraFields.size,
            continuous = labeler.continuous,
            extension = labeler.extension,
        )

    private fun createState(
        modules: List<ImportedModule>,
        onFinish: () -> Unit = {},
    ) = ImportEntriesDialogState(onFinish, appState, modules)

    @Test
    fun testExistingModuleNamesAndContinuousDefaults() {
        val state = createState(listOf(compatibleModule("C4")))

        assertEquals(listOf("A3", "C4"), state.existingModuleNames.sorted())
        // the UTAU oto labeler is not continuous, so replacing content is optional and off by default
        assertFalse(state.forceReplaceContent)
        assertFalse(state.replaceContent)
    }

    @Test
    fun testMatchingModuleNameIsAutoSelected() {
        val state = createState(listOf(compatibleModule("C4")))
        val item = state.items.single()

        assertTrue(item.compatible)
        assertTrue(item.selected)
        assertEquals("C4", item.targetName)
        assertTrue(item.isValid)
        assertTrue(state.isValid)
    }

    @Test
    fun testMismatchingModuleNameIsNotSelected() {
        val state = createState(listOf(compatibleModule("Unknown")))
        val item = state.items.single()

        assertTrue(item.compatible)
        assertFalse(item.selected)
        assertNull(item.targetName)
        // an unselected item is still valid (it simply will not be imported)
        assertTrue(item.isValid)
        assertTrue(state.isValid)
    }

    @Test
    fun testIncompatibleModuleIsNotSelected() {
        val incompatible = ImportedModule(
            name = "C4",
            entries = listOf(sampleEntry("imported")),
            pointSize = labeler.fields.size + 1,
            extraSize = labeler.extraFields.size,
            continuous = labeler.continuous,
            extension = labeler.extension,
        )
        val state = createState(listOf(incompatible))
        val item = state.items.single()

        assertFalse(item.compatible)
        assertFalse(item.selected)
    }

    @Test
    fun testSelectTargetAndToggle() {
        val state = createState(listOf(compatibleModule("Unknown")))
        val item = state.items.single()

        item.selectTarget("C4")
        assertTrue(item.selected)
        assertEquals("C4", item.targetName)

        item.toggleSelected(false)
        assertFalse(item.selected)
        assertNull(item.targetName)

        // selected but with no target resolves to an invalid state
        item.toggleSelected(true)
        assertTrue(item.selected)
        assertNull(item.targetName)
        assertFalse(item.isValid)
        assertFalse(state.isValid)
    }

    @Test
    fun testSubmitImportsSelectedEntriesIntoTargetModule() {
        val before = appState.requireProject().modules.first { it.name == "C4" }.entries.size
        var finished = false
        val state = createState(
            listOf(compatibleModule("C4", entries = listOf(sampleEntry("imported-1")))),
        ) { finished = true }

        state.submit()

        assertTrue(finished)
        val c4 = appState.requireProject().modules.first { it.name == "C4" }
        assertEquals(before + 1, c4.entries.size)
        assertTrue(c4.entries.any { it.name == "imported-1" })
    }

    @Test
    fun testUnselectedModulesAreNotImported() {
        val before = appState.requireProject().modules.first { it.name == "C4" }.entries.size
        // name does not match any existing module, so it is not selected by default
        val state = createState(listOf(compatibleModule("Unknown", entries = listOf(sampleEntry("skipped")))))

        state.submit()

        val c4 = appState.requireProject().modules.first { it.name == "C4" }
        assertEquals(before, c4.entries.size)
    }

    @Test
    fun testForceReplaceContentForContinuousLabeler() {
        val nnsvsTempDir = createTempDirectory("vlabeler-test").toFile()
        try {
            val sampleDir = TestFixtures.deploy(
                "nnsvs-singer",
                nnsvsTempDir.resolve("nnsvs-singer"),
                wavFiles = listOf("wav/doremi.wav", "wav/legato.wav"),
                wavDurationMs = 1500,
            )
            val project: Project = createTestProject(
                labeler = TestLabelers.nnsvsSinger,
                sampleDirectory = sampleDir,
            )
            val nnsvsAppState = TestAppState.create(
                scope = scope,
                availableLabelerConfs = listOf(TestLabelers.nnsvsSinger),
            )
            nnsvsAppState.openEditor(project)
            val nnsvsLabeler = project.labelerConf
            val module = ImportedModule(
                name = "doremi",
                entries = listOf(project.modules.first { it.name == "doremi" }.entries.first()),
                pointSize = nnsvsLabeler.fields.size,
                extraSize = nnsvsLabeler.extraFields.size,
                continuous = nnsvsLabeler.continuous,
                extension = nnsvsLabeler.extension,
            )
            val state = ImportEntriesDialogState({}, nnsvsAppState, listOf(module))

            assertTrue(state.forceReplaceContent)
            assertTrue(state.replaceContent)
        } finally {
            nnsvsTempDir.deleteRecursively()
        }
    }
}
