package io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.WAVE_LOADING_ALGORITHM_VERSION
import com.sdercolin.vlabeler.io.getSampleValueFromFrame
import com.sdercolin.vlabeler.io.loadSampleChunk
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleInfo
import kotlinx.coroutines.runBlocking
import testutil.TestFixtures
import testutil.TestLabelers
import testutil.TestWav
import testutil.createTestProject
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [SampleInfo.load] and [loadSampleChunk] driven by real generated wav files.
 */
class WaveLoadingTest {

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
        )
    }

    private fun loadSampleInfo(project: Project, file: File, appConf: AppConf): SampleInfo = runBlocking {
        SampleInfo.load(project, moduleName = "", file = file, appConf = appConf).getOrThrow()
    }

    @Test
    fun testGetSampleValueFromFrameMonoLittleEndian() {
        val frame = byteArrayOf(0x34, 0x12)
        val value = getSampleValueFromFrame(frame, 2, 0, 1, isBigEndian = false)
        assertEquals(0x1234.toFloat(), value)
    }

    @Test
    fun testGetSampleValueFromFrameNegativeValues() {
        val minusOne = byteArrayOf(0xFF.toByte(), 0xFF.toByte())
        assertEquals(-1f, getSampleValueFromFrame(minusOne, 2, 0, 1, isBigEndian = false))
        val minValue = byteArrayOf(0x00, 0x80.toByte())
        assertEquals(-32768f, getSampleValueFromFrame(minValue, 2, 0, 1, isBigEndian = false))
    }

    @Test
    fun testGetSampleValueFromFrameBigEndian() {
        val frame = byteArrayOf(0x12, 0x34)
        val value = getSampleValueFromFrame(frame, 2, 0, 1, isBigEndian = true)
        assertEquals(0x1234.toFloat(), value)
    }

    @Test
    fun testGetSampleValueFromFrameStereo() {
        val frame = byteArrayOf(0x01, 0x00, 0x00, 0x80.toByte())
        assertEquals(1f, getSampleValueFromFrame(frame, 4, 0, 2, isBigEndian = false))
        assertEquals(-32768f, getSampleValueFromFrame(frame, 4, 1, 2, isBigEndian = false))
    }

    @Test
    fun testLoadSampleInfo() {
        val project = createProject()
        val file = project.rootSampleDirectory.resolve("_a_ka.wav")
        val appConf = AppConf()

        val sampleInfo = loadSampleInfo(project, file, appConf)

        assertEquals("_a_ka.wav", sampleInfo.name)
        assertEquals("_a_ka.wav", sampleInfo.file)
        assertNull(sampleInfo.convertedFile)
        assertEquals(44100f, sampleInfo.sampleRate)
        assertEquals(1, sampleInfo.channels)
        assertEquals(44100, sampleInfo.length)
        assertEquals(1000f, sampleInfo.lengthMillis, 1f)
        assertEquals(1, sampleInfo.chunkCount)
        assertEquals(44100, sampleInfo.chunkSize)
        assertNull(sampleInfo.normalizeRatio)
        assertEquals(appConf.painter.spectrogram.enabled, sampleInfo.hasSpectrogram)
        assertEquals(appConf.painter.power.enabled, sampleInfo.hasPower)
        assertEquals(appConf.painter.fundamental.enabled, sampleInfo.hasFundamental)
        assertEquals(WAVE_LOADING_ALGORITHM_VERSION, sampleInfo.algorithmVersion)
    }

    @Test
    fun testLoadSampleInfoWithNormalize() {
        val project = createProject()
        val file = project.rootSampleDirectory.resolve("_a_ka.wav")
        val appConf = AppConf().run {
            copy(painter = painter.copy(amplitude = painter.amplitude.copy(normalize = true)))
        }

        val sampleInfo = loadSampleInfo(project, file, appConf)

        // the test wav has a peak of about half the full scale, so the ratio should be about 2
        val ratio = assertNotNull(sampleInfo.normalizeRatio)
        assertEquals(2f, ratio, 0.05f)
    }

    @Test
    fun testLoadSampleInfoWithResampling() {
        val project = createProject()
        val file = project.rootSampleDirectory.resolve("high-rate.wav")
        TestWav.write(file, durationMs = 1000, sampleRate = 88200)
        val appConf = AppConf()

        val sampleInfo = loadSampleInfo(project, file, appConf)

        assertEquals(44100, appConf.painter.amplitude.resampleDownToHz)
        assertEquals(44100f, sampleInfo.sampleRate)
        assertEquals(44100, sampleInfo.length)
        assertEquals(1000f, sampleInfo.lengthMillis, 1f)
    }

    @Test
    fun testLoadSampleChunk() {
        val project = createProject()
        val file = project.rootSampleDirectory.resolve("sine.wav")
        TestWav.write(file, durationMs = 1000, sampleRate = 44100, frequency = 440.0)
        val appConf = AppConf().run {
            copy(
                painter = painter.copy(
                    power = painter.power.copy(enabled = true),
                    fundamental = painter.fundamental.copy(enabled = true),
                ),
            )
        }
        val sampleInfo = loadSampleInfo(project, file, appConf)

        val chunk = runBlocking {
            loadSampleChunk(project, sampleInfo, appConf, chunkIndex = 0, chunkSize = sampleInfo.chunkSize)
        }.getOrThrow()

        assertEquals(0, chunk.index)
        assertEquals(sampleInfo, chunk.info)
        assertEquals(1, chunk.wave.channels.size)
        assertEquals(sampleInfo.length, chunk.wave.length)
        // the generated sine wave has an amplitude of about half the full 16-bit scale
        val data = chunk.wave.channels[0].data
        val max = data.max()
        val min = data.min()
        assertTrue(max in 16000f..16384f, "Unexpected max amplitude: $max")
        assertTrue(min in -16384f..-16000f, "Unexpected min amplitude: $min")
        assertNotNull(chunk.spectrogram)
        assertNotNull(chunk.power)
        assertNotNull(chunk.fundamental)
    }

    @Test
    fun testLoadSampleChunkDisabledFeatures() {
        val project = createProject()
        val file = project.rootSampleDirectory.resolve("_a_ka.wav")
        val appConf = AppConf().run {
            copy(
                painter = painter.copy(
                    spectrogram = painter.spectrogram.copy(enabled = false),
                ),
            )
        }
        val sampleInfo = loadSampleInfo(project, file, appConf)

        val chunk = runBlocking {
            loadSampleChunk(project, sampleInfo, appConf, chunkIndex = 0, chunkSize = sampleInfo.chunkSize)
        }.getOrThrow()

        assertNull(chunk.spectrogram)
        assertNull(chunk.power)
        assertNull(chunk.fundamental)
    }

    @Test
    fun testChunkedLoadingMatchesSingleChunk() {
        val project = createProject()
        val file = project.rootSampleDirectory.resolve("chunked.wav")
        TestWav.write(file, durationMs = 3000, sampleRate = 8000)

        // a small max chunk size forces the file to be split into multiple chunks
        val chunkedConf = AppConf().run { copy(painter = painter.copy(maxDataChunkSize = 8000)) }
        val chunkedInfo = loadSampleInfo(project, file, chunkedConf)
        assertEquals(3, chunkedInfo.chunkCount)
        assertEquals(8000, chunkedInfo.chunkSize)

        val singleConf = AppConf()
        val singleInfo = loadSampleInfo(project, file, singleConf)
        assertEquals(1, singleInfo.chunkCount)
        val fullWave = runBlocking {
            loadSampleChunk(project, singleInfo, singleConf, chunkIndex = 0, chunkSize = singleInfo.chunkSize)
        }.getOrThrow().wave

        val chunkWaves = (0 until chunkedInfo.chunkCount).map { index ->
            runBlocking {
                loadSampleChunk(
                    project = project,
                    sampleInfo = chunkedInfo,
                    appConf = chunkedConf,
                    chunkIndex = index,
                    chunkSize = chunkedInfo.chunkSize,
                )
            }.getOrThrow().wave
        }

        assertEquals(fullWave.length, chunkWaves.sumOf { it.length })
        val concatenated = chunkWaves.flatMap { it.channels[0].data.toList() }.toFloatArray()
        assertTrue(fullWave.channels[0].data.contentEquals(concatenated))
    }
}
