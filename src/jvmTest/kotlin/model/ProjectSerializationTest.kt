package model

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Parameter
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import com.sdercolin.vlabeler.util.toParamMap
import testutil.TestFixtures
import testutil.TestLabelers
import testutil.createTestProject
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ProjectSerializationTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        Log.muted = true
        tempDir = createTempDirectory("vlabeler-test").toFile()
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
        tempDir.deleteRecursively()
    }

    @Test
    fun `project survives a json round trip`() {
        val sampleDir = TestFixtures.deploy(
            "utau-singer",
            tempDir.resolve("utau-singer"),
            wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
        )
        val labeler = TestLabelers.utauSinger
        // use a non-default labeler parameter so that `labelerParams` is stored in the project
        val labelerParams = (labeler.getDefaultParams() + ("useNegativeOvl" to false)).toParamMap()
        val project = createTestProject(
            labeler = labeler,
            sampleDirectory = sampleDir,
            labelerParams = labelerParams,
        )

        val parsed = project.stringifyJson().parseJson<Project>()

        // basic fields
        assertEquals(project.version, parsed.version)
        assertEquals(project.rootSampleDirectoryPath, parsed.rootSampleDirectoryPath)
        assertEquals(project.workingDirectoryPath, parsed.workingDirectoryPath)
        assertEquals(project.projectName, parsed.projectName)
        assertEquals(project.cacheDirectoryPath, parsed.cacheDirectoryPath)
        assertEquals(project.encoding, parsed.encoding)
        assertEquals(project.multipleEditMode, parsed.multipleEditMode)
        assertEquals(project.currentModuleIndex, parsed.currentModuleIndex)
        assertEquals(project.autoExport, parsed.autoExport)

        // modules with all entries survive
        assertEquals(project.modules, parsed.modules)
        assertEquals(listOf("A3", "C4"), parsed.modules.map { it.name }.sorted())
        val entries = parsed.modules.first { it.name == "C4" }.entries
        assertEquals(project.modules.first { it.name == "C4" }.entries, entries)

        // the non-default labeler parameter survives
        val storedParam = requireNotNull(requireNotNull(parsed.labelerParams).get("useNegativeOvl"))
        assertEquals(Parameter.BooleanParam.Type, storedParam.type)
        assertEquals(false, storedParam.value)

        // the original labeler conf is stored; the transient injected conf falls back to it
        assertEquals(labeler.name, parsed.originalLabelerConf.name)
        assertEquals(labeler.version, parsed.originalLabelerConf.version)
        assertEquals(labeler.fields.map { it.name }, parsed.originalLabelerConf.fields.map { it.name })
        assertEquals(parsed.originalLabelerConf, parsed.labelerConf)
    }
}
