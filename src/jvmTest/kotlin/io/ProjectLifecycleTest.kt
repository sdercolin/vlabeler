package io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.reloadEntriesFromLabelFile
import com.sdercolin.vlabeler.io.singleModuleToRawLabels
import com.sdercolin.vlabeler.model.EntryListDiffItem.Unchanged
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
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
import kotlin.test.assertTrue

/**
 * Tests the full project lifecycle through the real IO primitives: create → save the project file → load it back →
 * edit entries → save again → export raw labels → reload them.
 */
class ProjectLifecycleTest {

    private lateinit var tempDir: File
    private lateinit var sampleDir: File

    @BeforeTest
    fun setup() {
        Log.muted = true
        TestEnv.ensureLogDirectory()
        tempDir = createTempDirectory("vlabeler-test").toFile()
        sampleDir = TestFixtures.deploy(
            "utau-singer",
            tempDir.resolve("utau-singer"),
            wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
        )
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
        tempDir.deleteRecursively()
    }

    private fun save(project: Project) {
        project.projectFile.parentFile.mkdirs()
        project.projectFile.writeText(project.stringifyJson())
    }

    private fun load(file: File): Project = file.readText()
        .parseJson<Project>()
        .run { copy(labelerConf = this.labelerConf.migrate()) }

    @Test
    fun testFullLifecycle() {
        // create
        val created = createTestProject(
            labeler = TestLabelers.utauSinger,
            sampleDirectory = sampleDir,
            workingDirectory = tempDir.resolve("working"),
        )

        // save and load back
        save(created)
        val loaded = load(created.projectFile)
        assertEquals(created.projectName, loaded.projectName)
        assertEquals(created.modules, loaded.modules)
        assertEquals(created.labelerParams, loaded.labelerParams)
        // the transient injected labelerConf falls back to the original one on load
        assertEquals(loaded.originalLabelerConf, loaded.labelerConf)

        // edit an entry and save again
        val editedName = "a ka edited"
        val edited = loaded.updateModule("C4") {
            renameEntry(entries.indexOfFirst { it.name == "a ka" }, editedName, loaded.labelerConf)
        }
        save(edited)
        val reloaded = load(edited.projectFile)
        assertEquals(
            edited.modules.first { it.name == "C4" }.entries,
            reloaded.modules.first { it.name == "C4" }.entries,
        )

        // export raw labels to the module's label file and reload them from disk
        val moduleIndex = reloaded.modules.indexOfFirst { it.name == "C4" }
        val module = reloaded.modules[moduleIndex]
        val rawFile = requireNotNull(module.getRawFile(reloaded))
        rawFile.writeText(reloaded.singleModuleToRawLabels(moduleIndex))

        val (entries, diff) = reloadEntriesFromLabelFile(reloaded, module, rawFile).getOrThrow()
        assertEquals(module.entries.map { it.name }, entries.map { it.name })
        assertTrue(entries.any { it.name == editedName })
        // exporting and reloading without further edits must be lossless
        assertEquals(module.entries.size, diff.items.filterIsInstance<Unchanged>().size)
    }
}
