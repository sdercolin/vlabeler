package plugins

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.TemplatePluginResult
import testutil.TestFixtures
import testutil.TestLabelers
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Integration tests for the bundled `audacity2lab` and `lab2audacity` template plugins, executed through the real
 * plugin loading and execution path, including their `find-input.js` input finder scripts.
 */
class ConversionTemplatePluginsTest {

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
    fun testAudacity2Lab() {
        val inputFile = TestFixtures.getDir("plugins/audacity2lab").resolve("labels.txt")
        val result = TemplatePluginRunner.run(
            pluginName = "audacity2lab",
            labeler = TestLabelers.sinsy,
            sampleDirectory = tempDir,
            sampleFileNames = listOf("song.wav"),
            paramOverrides = mapOf("inputFile" to inputFile.absolutePath),
        )
        val raw = assertIs<TemplatePluginResult.Raw>(result)
        // seconds are converted to 100ns units (1s = 10^7 * 100ns) with the default time unit of 1.0
        assertEquals(
            listOf(
                "0 2500000 a",
                "2500000 5000000 i",
            ),
            raw.lines,
        )
    }

    @Test
    fun testAudacity2LabWithCustomTimeUnit() {
        val inputFile = TestFixtures.getDir("plugins/audacity2lab").resolve("labels.txt")
        val result = TemplatePluginRunner.run(
            pluginName = "audacity2lab",
            labeler = TestLabelers.sinsy,
            sampleDirectory = tempDir,
            sampleFileNames = listOf("song.wav"),
            paramOverrides = mapOf(
                "inputFile" to inputFile.absolutePath,
                "unit" to 2f,
            ),
        )
        val raw = assertIs<TemplatePluginResult.Raw>(result)
        assertEquals(
            listOf(
                "0 5000000 a",
                "5000000 10000000 i",
            ),
            raw.lines,
        )
    }

    @Test
    fun testLab2Audacity() {
        val inputFile = TestFixtures.getDir("plugins/lab2audacity").resolve("labels.lab")
        val result = TemplatePluginRunner.run(
            pluginName = "lab2audacity",
            labeler = TestLabelers.audacity,
            sampleDirectory = tempDir,
            sampleFileNames = listOf("song.wav"),
            paramOverrides = mapOf("inputFile" to inputFile.absolutePath),
        )
        val raw = assertIs<TemplatePluginResult.Raw>(result)
        // 100ns units are converted to seconds with the default time unit of 1e-7
        assertEquals(
            listOf(
                "0\t0.25\ta",
                "0.25\t0.5\ti",
            ),
            raw.lines,
        )
    }
}
