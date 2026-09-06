package plugins

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.loadPlugins
import com.sdercolin.vlabeler.model.PhonemizerRunner
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.key.Key
import com.sdercolin.vlabeler.model.key.KeySet
import com.sdercolin.vlabeler.ui.string.Language
import com.sdercolin.vlabeler.util.phonemizer.OnnxG2pPredictor
import testutil.TestEnv
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PhonemizerPluginTest {

    @BeforeTest
    fun setup() {
        Log.muted = true
        TestEnv.ensureLogDirectory()
    }

    @Test
    fun testLoadPhonemizerPlugins() {
        val plugins = loadPlugins(Plugin.Type.Phonemizer, Language.English)
        val pluginNames = plugins.map { it.name }.toSet()
        assertTrue(pluginNames.contains("phonemizer-raw"), "Expected phonemizer-raw to be loaded")
        assertTrue(pluginNames.contains("phonemizer-ja"), "Expected phonemizer-ja to be loaded")
        assertTrue(pluginNames.contains("phonemizer-en"), "Expected phonemizer-en to be loaded")
        assertTrue(pluginNames.contains("phonemizer-ru"), "Expected phonemizer-ru to be loaded")

        plugins.forEach { plugin ->
            assertEquals(Plugin.Type.Phonemizer, plugin.type)
        }
    }

    @Test
    fun testRawPhonemizer() {
        val plugins = loadPlugins(Plugin.Type.Phonemizer, Language.English)
        val rawPlugin = requireNotNull(plugins.find { it.name == "phonemizer-raw" })
        val result = PhonemizerRunner.run(rawPlugin, "apple banana , cherry ; date")
        assertEquals(listOf("apple", "banana", "cherry", "date"), result)
    }

    @Test
    fun testJapanesePhonemizer() {
        val plugins = loadPlugins(Plugin.Type.Phonemizer, Language.English)
        val jaPlugin = requireNotNull(plugins.find { it.name == "phonemizer-ja" })
        val resultKana = PhonemizerRunner.run(jaPlugin, "こんにちは")
        assertEquals(listOf("k", "o", "N", "n", "i", "ch", "i", "h", "a"), resultKana)

        val resultRomaji = PhonemizerRunner.run(jaPlugin, "sakura")
        assertEquals(listOf("s", "a", "k", "u", "r", "a"), resultRomaji)
    }

    @Test
    fun testEnglishPhonemizer() {
        val plugins = loadPlugins(Plugin.Type.Phonemizer, Language.English)
        val enPlugin = requireNotNull(plugins.find { it.name == "phonemizer-en" })
        val result = PhonemizerRunner.run(enPlugin, "hello world")
        assertTrue(result.isNotEmpty(), "Expected phonemes for 'hello world'")
    }

    @Test
    fun testRussianPhonemizer() {
        val plugins = loadPlugins(Plugin.Type.Phonemizer, Language.English)
        val ruPlugin = requireNotNull(plugins.find { it.name == "phonemizer-ru" })
        val result = PhonemizerRunner.run(ruPlugin, "привет мир")
        assertTrue(result.isNotEmpty(), "Expected phonemes for 'привет мир'")
    }

    @Test
    fun testOnnxG2pPredictor() {
        val plugins = loadPlugins(Plugin.Type.Phonemizer, Language.English)
        val enPlugin = requireNotNull(plugins.find { it.name == "phonemizer-en" })
        val modelFile = requireNotNull(enPlugin.directory).resolve("g2p.onnx")
        val graphemes = listOf(
            "", "", "", "", "'", "-", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
            "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
        )
        val phonemes = listOf(
            "", "", "", "", "aa", "ae", "ah", "ao", "aw", "ay", "b", "ch", "d", "dh", "eh", "er", "ey",
            "f", "g", "hh", "ih", "iy", "jh", "k", "l", "m", "n", "ng", "ow", "oy", "p", "r", "s", "sh",
            "t", "th", "uh", "uw", "v", "w", "y", "z", "zh",
        )

        val enPred = OnnxG2pPredictor.predict(modelFile, graphemes, phonemes, "testing")
        assertNotNull(enPred)
        assertTrue(enPred.isNotEmpty())
    }

    @Test
    fun testKeyActions() {
        val nextAction = KeyAction.EditEntryNameDialogNext
        assertEquals(KeySet(Key.Tab), nextAction.defaultKeySet)

        val prevAction = KeyAction.EditEntryNameDialogPrevious
        assertEquals(KeySet(Key.Tab, setOf(Key.Shift)), prevAction.defaultKeySet)
    }

    @Test
    fun testEnglishPhonemizerOov() {
        val plugins = loadPlugins(Plugin.Type.Phonemizer, Language.English)
        val enPlugin = requireNotNull(plugins.find { it.name == "phonemizer-en" })
        val result = PhonemizerRunner.run(enPlugin, "thoughtfulest")
        assertTrue(result.isNotEmpty(), "Expected phonemes for OOV word 'thoughtfulest'")
    }

    @Test
    fun testRussianPhonemizerOov() {
        val plugins = loadPlugins(Plugin.Type.Phonemizer, Language.English)
        val ruPlugin = requireNotNull(plugins.find { it.name == "phonemizer-ru" })
        val result = PhonemizerRunner.run(ruPlugin, "абракадабрище")
        assertTrue(result.isNotEmpty(), "Expected phonemes for OOV word 'абракадабрище'")
    }

    @Test
    fun testRenameMultipleEntries() {
        val entries = listOf(
            com.sdercolin.vlabeler.model.Entry(
                sample = "test.wav",
                name = "e0",
                start = 0f,
                end = 100f,
                points = emptyList(),
                extras = emptyList(),
            ),
            com.sdercolin.vlabeler.model.Entry(
                sample = "test.wav",
                name = "e1",
                start = 100f,
                end = 200f,
                points = emptyList(),
                extras = emptyList(),
            ),
            com.sdercolin.vlabeler.model.Entry(
                sample = "test.wav",
                name = "e2",
                start = 200f,
                end = 300f,
                points = emptyList(),
                extras = emptyList(),
            ),
        )
        val module = com.sdercolin.vlabeler.model.Module(
            name = "test",
            sampleDirectoryPath = "test",
            entries = entries,
            currentIndex = 0,
        )
        val renamed = module.renameMultipleEntries(0, listOf("k", "o", "n", "extra"))
        assertEquals("k", renamed.entries[0].name)
        assertEquals("o", renamed.entries[1].name)
        assertEquals("n", renamed.entries[2].name)
        assertEquals(3, renamed.entries.size)
    }

    @Test
    fun testPhonemizerClearCache() {
        PhonemizerRunner.clearCache()
    }
}
