package io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.asLabelerConf
import com.sdercolin.vlabeler.io.getCustomLabelers
import com.sdercolin.vlabeler.io.install
import com.sdercolin.vlabeler.io.loadAvailableLabelerConfs
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.util.CustomLabelerDir
import com.sdercolin.vlabeler.util.DefaultLabelerDir
import com.sdercolin.vlabeler.util.stringifyJson
import kotlinx.coroutines.runBlocking
import testutil.TestLabelers
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for `io/LabelerConf.kt`: [asLabelerConf], [install] and [loadAvailableLabelerConfs].
 *
 * Custom labelers are installed into the [CustomLabelerDir] under the redirected application directory
 * (`build/test-app-dir`) and removed again in [teardown].
 */
class LabelerConfIoTest {

    private lateinit var tempDir: File
    private val installedCustomFiles = mutableListOf<File>()

    @BeforeTest
    fun setup() {
        Log.muted = true
        tempDir = createTempDirectory("vlabeler-test").toFile()
        CustomLabelerDir.mkdirs()
    }

    @AfterTest
    fun teardown() {
        installedCustomFiles.forEach { it.deleteRecursively() }
        installedCustomFiles.clear()
        tempDir.deleteRecursively()
        Log.muted = false
    }

    @Test
    fun testAsLabelerConfParsesBuiltInLabeler() {
        val file = DefaultLabelerDir.resolve("oto-labeler").resolve(LabelerConf.LABELER_FILE_EXTENSION)

        val conf = file.asLabelerConf(isBuiltIn = true).getOrThrow()

        assertEquals("oto-plus.default", conf.name)
        assertTrue(conf.builtIn)
        assertFalse(conf.singleFile)
        assertEquals(file.parentFile, conf.directory)
        // scripts are preloaded into lines
        assertTrue(conf.parser.scripts.lines.orEmpty().isNotEmpty())
        assertTrue(conf.quickProjectBuilders.single().scripts.lines.orEmpty().isNotEmpty())
    }

    @Test
    fun testAsLabelerConfReturnsFailureForInvalidFile() {
        val file = tempDir.resolve("broken.${LabelerConf.LABELER_FILE_EXTENSION}")
        file.writeText("{ this is not a labeler conf")

        assertTrue(file.asLabelerConf(isBuiltIn = false).isFailure)
    }

    @Test
    fun testInstallDirectoryLabeler() {
        val conf = TestLabelers.utauOto

        val installedFile = conf.install(tempDir).getOrThrow()

        val folder = tempDir.resolve("oto-plus-labeler")
        assertEquals(folder.resolve(LabelerConf.LABELER_FILE_EXTENSION), installedFile)
        assertTrue(folder.resolve("parser.js").exists())
        assertTrue(folder.resolve("fixed-getter.js").exists())

        val reloaded = installedFile.asLabelerConf(isBuiltIn = false).getOrThrow()
        assertEquals(conf.name, reloaded.name)
        assertEquals(conf.version, reloaded.version)
        assertFalse(reloaded.builtIn)
        assertEquals(
            conf.parser.scripts.getScripts(conf.directory),
            reloaded.parser.scripts.getScripts(reloaded.directory),
        )
        assertEquals(
            conf.quickProjectBuilders.single().scripts.getScripts(conf.directory),
            reloaded.quickProjectBuilders.single().scripts.getScripts(reloaded.directory),
        )
    }

    @Test
    fun testInstallSingleFileLabeler() {
        val conf = TestLabelers.utauOto.copy(name = "zz-single-test", singleFile = true)

        val installedFile = conf.install(tempDir).getOrThrow()

        assertEquals(tempDir.resolve("zz-single-test.${LabelerConf.LABELER_FILE_EXTENSION}"), installedFile)

        val reloaded = installedFile.asLabelerConf(isBuiltIn = false).getOrThrow()
        assertEquals("zz-single-test", reloaded.name)
        assertEquals(conf.version, reloaded.version)
        assertEquals(
            conf.parser.scripts.getScripts(conf.directory),
            reloaded.parser.scripts.getScripts(reloaded.directory),
        )
    }

    @Test
    fun testLoadAvailableLabelerConfsContainsDefaultLabelers() {
        val labelers = runBlocking { loadAvailableLabelerConfs() }

        val names = labelers.map { it.name }
        assertTrue(names.contains("oto-plus.default"))
        assertTrue(names.contains("utau-singer.default"))
        assertTrue(labelers.first { it.name == "oto-plus.default" }.builtIn)
        assertEquals(names.sorted(), names)
    }

    @Test
    fun testCustomLabelerIsLoadedAndDefaultNameIsShadowed() {
        val custom = TestLabelers.utauOto.copy(name = "zz-custom-test", singleFile = true)
        installedCustomFiles += custom.install(CustomLabelerDir).getOrThrow()
        // a custom labeler with the same name as a default one is ignored, even with a higher version
        val conflicting = TestLabelers.utauOto.copy(version = 9999, singleFile = true)
        installedCustomFiles += conflicting.install(CustomLabelerDir).getOrThrow()

        val labelers = runBlocking { loadAvailableLabelerConfs() }

        val customLoaded = labelers.single { it.name == "zz-custom-test" }
        assertFalse(customLoaded.builtIn)
        val otoLoaded = labelers.single { it.name == "oto-plus.default" }
        assertTrue(otoLoaded.builtIn)
        assertEquals(TestLabelers.utauOto.version, otoLoaded.version)
    }

    @Test
    fun testCustomLabelersWithSameNameAreDeduplicatedByVersion() {
        val v1 = TestLabelers.utauOto.copy(name = "zz-dup-test", version = 1, singleFile = true)
        val v2 = TestLabelers.utauOto.copy(name = "zz-dup-test", version = 2, singleFile = true)
        val fileV1 = CustomLabelerDir.resolve("zz-dup-1.${LabelerConf.LABELER_FILE_EXTENSION}")
        val fileV2 = CustomLabelerDir.resolve("zz-dup-2.${LabelerConf.LABELER_FILE_EXTENSION}")
        fileV1.writeText(v1.stringifyJson())
        fileV2.writeText(v2.stringifyJson())
        installedCustomFiles += fileV1
        installedCustomFiles += fileV2

        assertTrue(getCustomLabelers().containsAll(listOf(fileV1, fileV2)))

        val labelers = runBlocking { loadAvailableLabelerConfs() }

        assertEquals(2, labelers.single { it.name == "zz-dup-test" }.version)
    }
}
