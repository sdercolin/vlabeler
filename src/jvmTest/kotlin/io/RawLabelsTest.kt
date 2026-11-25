package io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.moduleFromRawLabels
import com.sdercolin.vlabeler.io.singleModuleToRawLabels
import com.sdercolin.vlabeler.model.Entry
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
import kotlin.test.assertTrue

/**
 * Tests for [moduleFromRawLabels] and [singleModuleToRawLabels] using the bundled labelers.
 */
class RawLabelsTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        Log.muted = true
        TestEnv.ensureLogDirectory()
        tempDir = createTempDirectory("vlabeler-test").toFile()
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
        tempDir.deleteRecursively()
    }

    private fun parseUtau(vararg lines: String): List<Entry> = moduleFromRawLabels(
        sources = lines.toList(),
        inputFile = null,
        labelerConf = TestLabelers.utauSinger,
        labelerParams = TestLabelers.utauSinger.getDefaultParams(),
        sampleFiles = listOf(File("_a_ka.wav"), File("_i_ki.wav")),
        encoding = "UTF-8",
    )

    @Test
    fun testParseUtauOtoLine() {
        val entries = parseUtau("_a_ka.wav=- a,10.0,100.0,-400.0,80.0,30.0")
        val entry = entries.single()
        assertEquals("_a_ka.wav", entry.sample)
        assertEquals("- a", entry.name)
        assertEquals(10f, entry.start)
        assertEquals(410f, entry.end)
        // points: [fixed, preutterance, overlap, offset] as absolute positions
        assertEquals(listOf(110f, 90f, 40f, 10f), entry.points)
        assertFalse(entry.needSync)
        // the raw cutoff value is kept as an extra to allow restoring the original value
        assertEquals(-400f, entry.extras.single()?.toFloat())
    }

    @Test
    fun testParseSkipsBlankLines() {
        val entries = parseUtau(
            "",
            "_a_ka.wav=- a,10.0,100.0,-400.0,80.0,30.0",
            "   ",
            "_i_ki.wav=- i,10.0,100.0,-400.0,80.0,30.0",
            "",
        )
        assertEquals(listOf("- a", "- i"), entries.map { it.name })
    }

    @Test
    fun testParseSkipsInvalidLines() {
        val entries = parseUtau(
            "this is not an oto line",
            "_a_ka.wav=- a,10.0,100.0,-400.0,80.0,30.0",
        )
        assertEquals(listOf("- a"), entries.map { it.name })
    }

    @Test
    fun testParseEmptyNameFallsBackToSampleName() {
        val entries = parseUtau("_a_ka.wav=,10.0,100.0,-400.0,80.0,30.0")
        assertEquals("_a_ka", entries.single().name)
    }

    @Test
    fun testParseEmptyFieldsDefaultToZero() {
        val entries = parseUtau("_a_ka.wav=empty,,,,,")
        val entry = entries.single()
        assertEquals(0f, entry.start)
        assertEquals(0f, entry.end, absoluteTolerance = 0f)
        assertEquals(listOf(0f, 0f, 0f, 0f), entry.points)
        // a non-negative raw cutoff value is relative to the sample end, which requires syncing with the sample
        assertTrue(entry.needSync)
    }

    @Test
    fun testUtauSingerWriteRoundTrip() {
        val sampleDir = TestFixtures.deploy(
            "utau-singer",
            tempDir.resolve("utau-singer"),
            wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
        )
        val project = createTestProject(
            labeler = TestLabelers.utauSinger,
            sampleDirectory = sampleDir,
        )
        for (moduleName in listOf("C4", "A3")) {
            val moduleIndex = project.modules.indexOfFirst { it.name == moduleName }
            val output = project.singleModuleToRawLabels(moduleIndex)
            val original = sampleDir.resolve(moduleName).resolve("oto.ini").readText().trimEnd()
            assertEquals(original, output.trimEnd(), "Round trip failed for module $moduleName")
        }
    }

    @Test
    fun testNnsvsWriteRoundTrip() {
        val sampleDir = TestFixtures.deploy(
            "nnsvs-singer",
            tempDir.resolve("nnsvs-singer"),
            wavFiles = listOf("wav/doremi.wav", "wav/legato.wav"),
            wavDurationMs = 1500,
        )
        val project = createTestProject(
            labeler = TestLabelers.nnsvsSinger,
            sampleDirectory = sampleDir,
        )
        for (moduleName in listOf("doremi", "legato")) {
            val moduleIndex = project.modules.indexOfFirst { it.name == moduleName }
            val output = project.singleModuleToRawLabels(moduleIndex)
            val original = sampleDir.resolve("lab").resolve("$moduleName.lab").readText().trimEnd()
            assertEquals(original, output.trimEnd(), "Round trip failed for module $moduleName")
        }
    }
}
