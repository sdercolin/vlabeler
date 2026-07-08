package repository

import com.sdercolin.vlabeler.audio.conversion.WaveConverter
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.repository.ConvertedAudioRepository
import com.sdercolin.vlabeler.util.parseJson
import kotlinx.coroutines.runBlocking
import testutil.TestEnv
import testutil.TestFixtures
import testutil.TestLabelers
import testutil.createTestProject
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for [ConvertedAudioRepository]: cache path computation, cache map persistence, and move/clear logic. A fake
 * [WaveConverter] is used so that no real ffmpeg execution is required.
 */
class ConvertedAudioRepositoryTest {

    private class FakeWaveConverter : WaveConverter {
        override fun accept(inputFile: File, conf: AppConf.Conversion): Boolean = true

        override suspend fun convert(inputFile: File, outputFile: File, conf: AppConf.Conversion) {
            inputFile.copyTo(outputFile, overwrite = true)
        }
    }

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
        project?.let { ConvertedAudioRepository.clear(it) }
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

    private fun create(project: Project, file: File): File = runBlocking {
        ConvertedAudioRepository.create(
            project = project,
            file = file,
            moduleName = "",
            converter = FakeWaveConverter(),
            appConf = AppConf(),
        )
    }

    private val Project.wavCacheDir get() = cacheDirectory.resolve("wav")

    @Test
    fun testCreateWritesConvertedFileAndCacheMap() {
        val project = createProject()
        ConvertedAudioRepository.init(project)
        val wavFile = project.rootSampleDirectory.resolve("_a_ka.wav")

        val output = create(project, wavFile)

        assertEquals(project.wavCacheDir.resolve("__a_ka.wav.converted.wav"), output)
        assertTrue(output.isFile)
        assertContentEquals(wavFile.readBytes(), output.readBytes())
        val map = project.wavCacheDir.resolve("map.json").readText().parseJson<Map<String, String>>()
        assertEquals("__a_ka.wav.converted.wav", map["_a_ka.wav"])
    }

    @Test
    fun testCreateReusesCachedPathAfterRestart() {
        val project = createProject()
        ConvertedAudioRepository.init(project)
        val wavFile = project.rootSampleDirectory.resolve("_a_ka.wav")
        val first = create(project, wavFile)

        // simulate a restart: the cache map is reloaded from map.json
        ConvertedAudioRepository.init(project)
        val second = create(project, wavFile)

        assertEquals(first, second)
    }

    @Test
    fun testFileNameCollisionBetweenDifferentSamplesWithSameName() {
        val project = createProject()
        ConvertedAudioRepository.init(project)
        val wavFile = project.rootSampleDirectory.resolve("_a_ka.wav")
        create(project, wavFile)

        val otherFile = project.rootSampleDirectory.resolve("sub").resolve("_a_ka.wav")
        otherFile.parentFile.mkdirs()
        wavFile.copyTo(otherFile)
        val output = create(project, otherFile)

        assertEquals("__a_ka.wav.converted.1.wav", output.name)
        assertTrue(output.isFile)
    }

    @Test
    fun testClearRemovesDirectory() {
        val project = createProject()
        ConvertedAudioRepository.init(project)
        create(project, project.rootSampleDirectory.resolve("_a_ka.wav"))
        assertTrue(project.wavCacheDir.isDirectory)

        ConvertedAudioRepository.clear(project)

        assertFalse(project.wavCacheDir.exists())
    }

    @Test
    fun testClearKeepsCacheMapEntries() {
        // Pins current behavior: unlike SampleInfoRepository.clear and ChartRepository.clear, this clear() does not
        // reset the in-memory cache map, so the next conversion of the same sample gets a suffixed file name
        // because the stale map entry still reserves the base name. Suspected bug in production code.
        val project = createProject()
        ConvertedAudioRepository.init(project)
        val wavFile = project.rootSampleDirectory.resolve("_a_ka.wav")
        create(project, wavFile)

        ConvertedAudioRepository.clear(project)
        val output = create(project, wavFile)

        assertEquals("__a_ka.wav.converted.1.wav", output.name)
    }

    @Test
    fun testMoveToCopiesCache() {
        val oldDir = tempDir.resolve("old").also { it.mkdirs() }
        oldDir.resolve("wav").also { it.mkdirs() }.resolve("a.converted.wav").writeText("a")
        val newDir = tempDir.resolve("new")

        ConvertedAudioRepository.moveTo(oldDir, newDir, clearOld = false)

        assertEquals("a", newDir.resolve("wav").resolve("a.converted.wav").readText())
        assertTrue(oldDir.resolve("wav").resolve("a.converted.wav").exists())
    }

    @Test
    fun testMoveToClearsOldCache() {
        val oldDir = tempDir.resolve("old").also { it.mkdirs() }
        oldDir.resolve("wav").also { it.mkdirs() }.resolve("a.converted.wav").writeText("a")
        val newDir = tempDir.resolve("new")

        ConvertedAudioRepository.moveTo(oldDir, newDir, clearOld = true)

        assertEquals("a", newDir.resolve("wav").resolve("a.converted.wav").readText())
        assertFalse(oldDir.resolve("wav").exists())
    }

    @Test
    fun testMoveToWithoutExistingCacheIsNoOp() {
        val oldDir = tempDir.resolve("old").also { it.mkdirs() }
        val newDir = tempDir.resolve("new")

        ConvertedAudioRepository.moveTo(oldDir, newDir, clearOld = true)

        assertFalse(newDir.exists())
    }
}
