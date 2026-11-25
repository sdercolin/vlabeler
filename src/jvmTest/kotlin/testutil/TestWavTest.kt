package testutil

import java.io.File
import javax.sound.sampled.AudioSystem
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [TestWav].
 */
class TestWavTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        tempDir = createTempDirectory("vlabeler-test").toFile()
    }

    @AfterTest
    fun teardown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun testGeneratedWavIsReadable() {
        val file = tempDir.resolve("test.wav")
        TestWav.write(file, durationMs = 1500)
        val stream = AudioSystem.getAudioInputStream(file)
        stream.use {
            assertEquals(TestWav.DEFAULT_SAMPLE_RATE.toFloat(), it.format.sampleRate)
            assertEquals(1, it.format.channels)
            assertEquals(16, it.format.sampleSizeInBits)
            assertEquals(TestWav.DEFAULT_SAMPLE_RATE.toLong() * 1500 / 1000, it.frameLength)
        }
    }
}
