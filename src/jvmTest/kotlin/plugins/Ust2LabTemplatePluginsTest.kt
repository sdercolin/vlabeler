package plugins

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.TemplatePluginResult
import testutil.TestFixtures
import testutil.TestLabelers
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Integration tests for the bundled `ust2lab-ja-kana` and `ust2lab-any` template plugins.
 *
 * The fixture usts use `Tempo=120` and notes with `Length=480` (one beat), so each note lasts exactly 500 ms.
 */
class Ust2LabTemplatePluginsTest {

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
    fun testUst2LabJaKana() {
        // notes: あ (0-500), か (500-1000), R (1000-1500)
        val inputFile = TestFixtures.getDir("plugins/ust2lab-ja-kana").resolve("song.ust")
        val result = TemplatePluginRunner.run(
            pluginName = "ust2lab-ja-kana",
            labeler = TestLabelers.sinsy,
            sampleDirectory = tempDir,
            sampleFileNames = listOf("song.wav"),
            paramOverrides = mapOf(
                "inputFile" to inputFile.absolutePath,
                "encoding" to "UTF-8",
            ),
        )
        val entries = assertIs<TemplatePluginResult.Parsed>(result).entries
        // か is expanded to "k a" by the bundled dictionary; the default overlap of 50 ms cuts the previous
        // vowel and puts the consonant before the note start. R is mapped to "pau".
        assertEquals(
            listOf(
                Entry("song.wav", "a", 0f, 450f, listOf(), listOf()),
                Entry("song.wav", "k", 450f, 500f, listOf(), listOf()),
                Entry("song.wav", "a", 500f, 1000f, listOf(), listOf()),
                Entry("song.wav", "pau", 1000f, 1500f, listOf(), listOf()),
            ),
            entries,
        )
    }

    @Test
    fun testUst2LabAnyWithDefaultDictionary() {
        // the default dictionary is injected from `dict-ja-kana.txt` in the plugin directory at load time
        val plugin = TemplatePluginRunner.getPlugin("ust2lab-any")
        val defaultDictionary = plugin.getDefaultParams()["dictionary"] as String
        assertFalse(defaultDictionary.startsWith("file::"))
        assertTrue(defaultDictionary.lines().contains("か k a"))

        // notes: あ (0-500), きゃ (500-1000), R (1000-1500); きゃ is "ky a" in the default dictionary
        val inputFile = TestFixtures.getDir("plugins/ust2lab-any").resolve("song.ust")
        val result = TemplatePluginRunner.run(
            pluginName = "ust2lab-any",
            labeler = TestLabelers.sinsy,
            sampleDirectory = tempDir,
            sampleFileNames = listOf("song.wav"),
            paramOverrides = mapOf(
                "inputFile" to inputFile.absolutePath,
                "encoding" to "UTF-8",
            ),
        )
        val entries = assertIs<TemplatePluginResult.Parsed>(result).entries
        assertEquals(
            listOf(
                Entry("song.wav", "a", 0f, 450f, listOf(), listOf()),
                Entry("song.wav", "ky", 450f, 500f, listOf(), listOf()),
                Entry("song.wav", "a", 500f, 1000f, listOf(), listOf()),
                Entry("song.wav", "pau", 1000f, 1500f, listOf(), listOf()),
            ),
            entries,
        )
    }

    @Test
    fun testUst2LabAnyWithCustomThreePhonemeDictionary() {
        // a custom dictionary expanding きゃ to three phonemes exercises the semivowel branch:
        // consonantLength = semivowelLength = (overlap 50 + vowelDelay 30) / 2 = 40
        val inputFile = TestFixtures.getDir("plugins/ust2lab-any").resolve("song.ust")
        val result = TemplatePluginRunner.run(
            pluginName = "ust2lab-any",
            labeler = TestLabelers.sinsy,
            sampleDirectory = tempDir,
            sampleFileNames = listOf("song.wav"),
            paramOverrides = mapOf(
                "inputFile" to inputFile.absolutePath,
                "encoding" to "UTF-8",
                "dictionary" to "あ a\nきゃ k y a\nR pau",
            ),
        )
        val entries = assertIs<TemplatePluginResult.Parsed>(result).entries
        assertEquals(
            listOf(
                Entry("song.wav", "a", 0f, 450f, listOf(), listOf()),
                Entry("song.wav", "k", 450f, 490f, listOf(), listOf()),
                Entry("song.wav", "y", 490f, 530f, listOf(), listOf()),
                Entry("song.wav", "a", 530f, 1000f, listOf(), listOf()),
                Entry("song.wav", "pau", 1000f, 1500f, listOf(), listOf()),
            ),
            entries,
        )
    }
}
