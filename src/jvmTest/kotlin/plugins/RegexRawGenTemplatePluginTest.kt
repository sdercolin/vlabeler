package plugins

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.TemplatePluginResult
import testutil.TestLabelers
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Integration test for the bundled `regex-raw-gen` template plugin, which generates raw label lines from the sample
 * file names using a regex and a line template.
 */
class RegexRawGenTemplatePluginTest {

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
    fun testRegexRawGen() {
        val result = TemplatePluginRunner.run(
            pluginName = "regex-raw-gen",
            labeler = TestLabelers.audacity,
            sampleDirectory = tempDir,
            sampleFileNames = listOf("a_ka.wav", "i_ki.wav", "noise.wav"),
            paramOverrides = mapOf(
                "regex" to "(.+)_(.+)\\.wav",
                "template" to "0.0\t0.5\t\$1\n0.5\t1.0\t\$2",
            ),
        )
        val raw = assertIs<TemplatePluginResult.Raw>(result)
        // each matching sample produces one line per template line, with $1/$2 replaced by the captured
        // groups; "noise.wav" does not match the regex and is skipped
        assertEquals(
            listOf(
                "0.0\t0.5\ta",
                "0.5\t1.0\tka",
                "0.0\t0.5\ti",
                "0.5\t1.0\tki",
            ),
            raw.lines,
        )
    }
}
