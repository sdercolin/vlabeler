package plugins

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.EntryNotes
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
 * Integration tests for the bundled oto generator template plugins (`cv-oto-gen`, `vcv-oto-gen`, `cvvc-oto-gen`).
 *
 * These plugins work purely from sample file names and parameters, so the sample files do not need to exist.
 */
class OtoGenTemplatePluginsTest {

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
    fun testCvOtoGen() {
        val result = TemplatePluginRunner.run(
            pluginName = "cv-oto-gen",
            labeler = TestLabelers.utauOto,
            sampleDirectory = tempDir,
            sampleFileNames = listOf("あ.wav", "か.wav"),
        )
        val entries = assertIs<TemplatePluginResult.Parsed>(result).entries
        // defaults: offset 100, overlap 30, preutterance 60, fixed 100, cutoff -1000
        // points are absolute positions: [fixed, preutterance, overlap, offset(start)]
        // note: the current script uses the full sample file name (with extension) as the entry name
        assertEquals(
            listOf(
                Entry("あ.wav", "あ.wav", 100f, 1100f, listOf(200f, 160f, 130f, 100f), listOf("-1000")),
                Entry("か.wav", "か.wav", 100f, 1100f, listOf(200f, 160f, 130f, 100f), listOf("-1000")),
            ),
            entries,
        )
    }

    @Test
    fun testVcvOtoGen() {
        // bpm 100 -> beatLength 600, preutterance 300, overlap 100, cutoff -700, fixed 450
        val result = TemplatePluginRunner.run(
            pluginName = "vcv-oto-gen",
            labeler = TestLabelers.utauOto,
            sampleDirectory = tempDir,
            sampleFileNames = listOf("_ああ.wav", "_あR.wav", "hello.wav"),
            paramOverrides = mapOf("bpm" to 100f),
        )
        val entries = assertIs<TemplatePluginResult.Parsed>(result).entries
        assertEquals(
            listOf(
                // beat 0 of "_ああ.wav": start = offset 500 - preutterance 300
                Entry("_ああ.wav", "- あ", 200f, 900f, listOf(650f, 500f, 300f, 200f), listOf("-700")),
                // beat 1: shifted by one beat (600 ms)
                Entry("_ああ.wav", "a あ", 800f, 1500f, listOf(1250f, 1100f, 900f, 800f), listOf("-700")),
                // "- あ" is repeated, so the second one gets the repeat suffix "2"
                Entry("_あR.wav", "- あ2", 200f, 900f, listOf(650f, 500f, 300f, 200f), listOf("-700")),
                // "R" is in the default suffix list and creates the tail entry "a R"
                Entry("_あR.wav", "a R", 800f, 1500f, listOf(1250f, 1100f, 900f, 800f), listOf("-700")),
                // a sample without the "_" prefix is kept as-is with its full file name
                Entry("hello.wav", "hello.wav", 200f, 900f, listOf(650f, 500f, 300f, 200f), listOf("-700")),
            ),
            entries,
        )
    }

    @Test
    fun testCvvcOtoGen() {
        // bpm 100 -> beatLength 600; with tempo compensation:
        // fixBuffer = min(100, 100) = 100, consLength = min(100, 120) = 100, ovlVC = min(80, 100) = 80
        val result = TemplatePluginRunner.run(
            pluginName = "cvvc-oto-gen",
            labeler = TestLabelers.utauOto,
            sampleDirectory = tempDir,
            sampleFileNames = listOf("_かか.wav", "_あR.wav"),
            paramOverrides = mapOf("bpm" to 100f),
        )
        val entries = assertIs<TemplatePluginResult.Parsed>(result).entries
        assertEquals(
            listOf(
                // head CV at beat 0; cutoff accounts for the following consonant (next is か)
                entry("_かか.wav", "- か", 390f, 920f, listOf(490f, 390f, 400f, 390f), "-530", "Head cv"),
                // VC between the two か
                entry("_かか.wav", "a k", 840f, 1100f, listOf(1010f, 1000f, 920f, 840f), "-260", "VC"),
                // CV at beat 1
                entry("_かか.wav", "か", 1000f, 1620f, listOf(1200f, 1100f, 1050f, 1000f), "-620", "CV"),
                // head V at beat 0 of the second sample
                entry("_あR.wav", "- あ", 480f, 1020f, listOf(580f, 480f, 490f, 480f), "-540", "Head V"),
                entry("_あR.wav", "あ", 600f, 1020f, listOf(600f, 680f, 680f, 600f), "-420", "Solo V"),
                // "R" is in the default suffix list and creates the tail entry "a R"
                entry("_あR.wav", "a R", 940f, 1120f, listOf(1110f, 1100f, 1020f, 940f), "-180", "Tail"),
            ),
            entries,
        )
    }

    private fun entry(
        sample: String,
        name: String,
        start: Float,
        end: Float,
        points: List<Float>,
        cutoff: String,
        tag: String,
    ) = Entry(sample, name, start, end, points, listOf(cutoff), EntryNotes(tag = tag))
}
