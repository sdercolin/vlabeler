package repository

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.repository.ChartRepository
import com.sdercolin.vlabeler.util.parseJson
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import testutil.TestEnv
import testutil.TestFixtures
import testutil.TestLabelers
import testutil.createTestProject
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [ChartRepository]: cache versioning/invalidation, cache file path computation, and move/clear logic.
 */
class ChartRepositoryTest {

    private lateinit var tempDir: File
    private var project: Project? = null

    @BeforeTest
    fun setup() {
        Log.muted = true
        TestEnv.ensureLogDirectory()
        tempDir = createTempDirectory("vlabeler-test").toFile()
    }

    @AfterTest
    fun teardown() {
        // reset the singleton's cache map so tests stay order-independent
        project?.let { ChartRepository.clear(it) }
        project = null
        tempDir.deleteRecursively()
        Log.muted = false
    }

    private fun createProject(): Project {
        val sampleDir = TestFixtures.deploy(
            "oto",
            tempDir.resolve("oto"),
            wavFiles = listOf("_a_ka.wav"),
        )
        return createTestProject(
            labeler = TestLabelers.utauOto,
            sampleDirectory = sampleDir,
            inputFilePath = sampleDir.resolve("oto.ini").absolutePath,
        ).also { project = it }
    }

    private fun sampleInfo(
        name: String = "x.wav",
        file: String = name,
        moduleName: String = "mod",
    ) = SampleInfo(
        name = name,
        file = file,
        convertedFile = null,
        moduleName = moduleName,
        sampleRate = 44100f,
        maxSampleRate = 44100,
        normalize = false,
        normalizeRatio = null,
        channels = 1,
        length = 44100,
        lengthMillis = 1000f,
        chunkSize = 44100,
        chunkCount = 1,
        hasSpectrogram = false,
        hasPower = false,
        powerChannels = 1,
        hasFundamental = false,
        lastModified = 0L,
        algorithmVersion = 1,
    )

    private fun createImageBitmap(width: Int = 4, height: Int = 4): ImageBitmap {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        for (x in 0 until width) {
            for (y in 0 until height) {
                image.setRGB(x, y, 0xFF336699.toInt())
            }
        }
        val pngFile = tempDir.resolve("input-${System.nanoTime()}.png")
        ImageIO.write(image, "png", pngFile)
        return pngFile.inputStream().buffered().use(::loadImageBitmap)
    }

    private val Project.chartsDir get() = cacheDirectory.resolve("charts")

    @Test
    fun testNeedResetOnVersionOrPainterChange() {
        val project = createProject()
        val appConf = AppConf()
        ChartRepository.init(project, appConf, version = 1)

        assertFalse(ChartRepository.needReset(appConf, version = 1))
        assertTrue(ChartRepository.needReset(appConf, version = 2))
        val changedConf = appConf.copy(
            painter = appConf.painter.copy(
                amplitude = appConf.painter.amplitude.copy(
                    normalize = appConf.painter.amplitude.normalize.not(),
                ),
            ),
        )
        assertTrue(ChartRepository.needReset(changedConf, version = 1))
    }

    @Test
    fun testInitKeepsCacheWhenParamsMatch() {
        val project = createProject()
        val appConf = AppConf()
        ChartRepository.init(project, appConf, version = 1)
        assertTrue(project.chartsDir.resolve("params.json").isFile)
        val marker = project.chartsDir.resolve("marker.png")
        marker.writeText("marker")

        ChartRepository.init(project, appConf, version = 1)

        assertTrue(marker.exists())
    }

    @Test
    fun testInitClearsCacheWhenVersionChanges() {
        val project = createProject()
        val appConf = AppConf()
        ChartRepository.init(project, appConf, version = 1)
        val marker = project.chartsDir.resolve("marker.png")
        marker.writeText("marker")

        ChartRepository.init(project, appConf, version = 2)

        assertFalse(marker.exists())
        val params = project.chartsDir.resolve("params.json").readText().parseJson<JsonObject>()
        assertEquals(2, params.getValue("algorithmVersion").jsonPrimitive.int)
    }

    @Test
    fun testInitClearsCacheWhenPainterConfigChanges() {
        val project = createProject()
        val appConf = AppConf()
        ChartRepository.init(project, appConf, version = 1)
        val marker = project.chartsDir.resolve("marker.png")
        marker.writeText("marker")

        val changedConf = appConf.copy(
            painter = appConf.painter.copy(
                spectrogram = appConf.painter.spectrogram.copy(
                    enabled = appConf.painter.spectrogram.enabled.not(),
                ),
            ),
        )
        ChartRepository.init(project, changedConf, version = 1)

        assertFalse(marker.exists())
    }

    @Test
    fun testImageFileNamesForFreshCache() {
        val project = createProject()
        ChartRepository.init(project, AppConf(), version = 1)
        val info = sampleInfo(name = "x.wav", moduleName = "mod")
        val chartsDir = project.chartsDir

        assertEquals(
            chartsDir.resolve("mod_x.wav_waveform_0_0.png"),
            ChartRepository.getWaveformImageFile(info, channelIndex = 0, chunkIndex = 0),
        )
        assertEquals(
            chartsDir.resolve("mod_x.wav_spectrogram_1.png"),
            ChartRepository.getSpectrogramImageFile(info, chunkIndex = 1),
        )
        assertEquals(
            chartsDir.resolve("mod_x.wav_power_1_2.png"),
            ChartRepository.getPowerGraphImageFile(info, channelIndex = 1, chunkIndex = 2),
        )
        assertEquals(
            chartsDir.resolve("mod_x.wav_fundamental_0.png"),
            ChartRepository.getFundamentalGraphImageFile(info, chunkIndex = 0),
        )
    }

    @Test
    fun testPutAndGetWaveformRoundTrip() {
        val project = createProject()
        ChartRepository.init(project, AppConf(), version = 1)
        val info = sampleInfo()

        ChartRepository.putWaveform(info, channelIndex = 0, chunkIndex = 0, waveform = createImageBitmap(8, 5))

        val file = ChartRepository.getWaveformImageFile(info, channelIndex = 0, chunkIndex = 0)
        assertEquals(project.chartsDir.resolve("mod_x.wav_waveform_0_0.png"), file)
        assertTrue(file.isFile)
        val map = project.chartsDir.resolve("map.json").readText().parseJson<Map<String, String>>()
        assertEquals("mod_x.wav_waveform_0_0.png", map["${info.file}//waveform//0//0"])

        val loaded = runBlocking { ChartRepository.getWaveform(info, channelIndex = 0, chunkIndex = 0) }
        assertEquals(8, loaded.width)
        assertEquals(5, loaded.height)
    }

    @Test
    fun testCacheMapIsReloadedByInit() {
        val project = createProject()
        val appConf = AppConf()
        ChartRepository.init(project, appConf, version = 1)
        val info = sampleInfo()
        ChartRepository.putWaveform(info, channelIndex = 0, chunkIndex = 0, waveform = createImageBitmap())
        val file = ChartRepository.getWaveformImageFile(info, channelIndex = 0, chunkIndex = 0)

        // simulate a restart with the same params: the cache map is reloaded from map.json
        ChartRepository.init(project, appConf, version = 1)

        assertTrue(file.isFile)
        assertEquals(file, ChartRepository.getWaveformImageFile(info, channelIndex = 0, chunkIndex = 0))
    }

    @Test
    fun testFileNameCollisionBetweenDifferentSamplesWithSameName() {
        val project = createProject()
        ChartRepository.init(project, AppConf(), version = 1)
        val infoA = sampleInfo(name = "x.wav", file = "a/x.wav")
        ChartRepository.putWaveform(infoA, channelIndex = 0, chunkIndex = 0, waveform = createImageBitmap())

        val infoB = sampleInfo(name = "x.wav", file = "b/x.wav")
        val fileB = ChartRepository.getWaveformImageFile(infoB, channelIndex = 0, chunkIndex = 0)

        assertEquals("mod_x.wav_waveform_0_0.1.png", fileB.name)
    }

    @Test
    fun testClearRemovesDirectoryAndCacheMap() {
        val project = createProject()
        ChartRepository.init(project, AppConf(), version = 1)
        val info = sampleInfo()
        ChartRepository.putWaveform(info, channelIndex = 0, chunkIndex = 0, waveform = createImageBitmap())

        ChartRepository.clear(project)

        assertFalse(project.chartsDir.exists())
        // the cache map is cleared, so the base file name is offered again
        assertEquals(
            project.chartsDir.resolve("mod_x.wav_waveform_0_0.png"),
            ChartRepository.getWaveformImageFile(info, channelIndex = 0, chunkIndex = 0),
        )
    }

    @Test
    fun testMoveToCopiesCache() {
        val oldDir = tempDir.resolve("old").also { it.mkdirs() }
        oldDir.resolve("charts").also { it.mkdirs() }.resolve("a.png").writeText("a")
        val newDir = tempDir.resolve("new")

        ChartRepository.moveTo(oldDir, newDir, clearOld = false)

        assertEquals("a", newDir.resolve("charts").resolve("a.png").readText())
        assertTrue(oldDir.resolve("charts").resolve("a.png").exists())
    }

    @Test
    fun testMoveToClearsOldCache() {
        val oldDir = tempDir.resolve("old").also { it.mkdirs() }
        oldDir.resolve("charts").also { it.mkdirs() }.resolve("a.png").writeText("a")
        val newDir = tempDir.resolve("new")

        ChartRepository.moveTo(oldDir, newDir, clearOld = true)

        assertEquals("a", newDir.resolve("charts").resolve("a.png").readText())
        assertFalse(oldDir.resolve("charts").exists())
    }

    @Test
    fun testMoveToWithoutExistingCacheIsNoOp() {
        val oldDir = tempDir.resolve("old").also { it.mkdirs() }
        val newDir = tempDir.resolve("new")

        ChartRepository.moveTo(oldDir, newDir, clearOld = true)

        assertFalse(newDir.exists())
    }
}
