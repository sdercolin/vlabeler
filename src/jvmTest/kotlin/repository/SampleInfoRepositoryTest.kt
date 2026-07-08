package repository

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.WAVE_LOADING_ALGORITHM_VERSION
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.repository.SampleInfoRepository
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for [SampleInfoRepository]: load/store round-trip of [SampleInfo] JSON, memory/file cache hits, eviction on
 * configuration or content changes, and move/clear logic.
 */
class SampleInfoRepositoryTest {

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
        // reset the singleton's memory cache and cache map so tests stay order-independent
        project?.let { SampleInfoRepository.clear(it) }
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

    private fun load(project: Project, appConf: AppConf = AppConf()): SampleInfo = runBlocking {
        SampleInfoRepository.load(
            project = project,
            sampleFile = project.rootSampleDirectory.resolve("_a_ka.wav"),
            moduleName = "",
            appConf = appConf,
        ).getOrThrow()
    }

    private val Project.sampleInfoDir get() = cacheDirectory.resolve("sample-info")

    private val Project.infoFile get() = sampleInfoDir.resolve("__a_ka.wav.info.json")

    @Test
    fun testLoadStoresInfoFileAndCacheMap() {
        val project = createProject()
        SampleInfoRepository.init(project)

        val info = load(project)

        assertEquals("_a_ka.wav", info.name)
        assertEquals("_a_ka.wav", info.file)
        assertEquals(WAVE_LOADING_ALGORITHM_VERSION, info.algorithmVersion)
        assertTrue(project.infoFile.isFile)
        assertEquals(info, project.infoFile.readText().parseJson<SampleInfo>())
        val map = project.sampleInfoDir.resolve("map.json").readText().parseJson<Map<String, String>>()
        assertEquals("__a_ka.wav.info.json", map["_a_ka.wav"])
    }

    @Test
    fun testLoadReturnsMemoryCachedInstance() {
        val project = createProject()
        SampleInfoRepository.init(project)

        val first = load(project)
        val second = load(project)

        assertSame(first, second)
    }

    @Test
    fun testMemoryCacheEvictedWhenPainterConfigChanges() {
        val project = createProject()
        SampleInfoRepository.init(project)
        val appConf = AppConf()
        val first = load(project, appConf)
        assertFalse(first.normalize)

        val changedConf = appConf.copy(
            painter = appConf.painter.copy(
                amplitude = appConf.painter.amplitude.copy(normalize = true),
            ),
        )
        val second = load(project, changedConf)

        assertNotSame(first, second)
        assertTrue(second.normalize)
    }

    @Test
    fun testMemoryCacheEvictedWhenSampleFileIsModified() {
        val project = createProject()
        SampleInfoRepository.init(project)
        val first = load(project)

        val wavFile = project.rootSampleDirectory.resolve("_a_ka.wav")
        assertTrue(wavFile.setLastModified(first.lastModified + 10000))
        val second = load(project)

        assertNotSame(first, second)
        assertEquals(wavFile.lastModified(), second.lastModified)
    }

    @Test
    fun testFileCacheIsUsedAfterRestart() {
        val project = createProject()
        SampleInfoRepository.init(project)
        val info = load(project)

        // simulate a restart: drop the memory cache but keep the cache files
        val backup = tempDir.resolve("backup")
        project.sampleInfoDir.copyRecursively(backup)
        SampleInfoRepository.clear(project)
        backup.copyRecursively(project.sampleInfoDir)
        // tamper a field that is not checked by shouldReload to detect whether the file cache is used
        project.infoFile.writeText(info.copy(lengthMillis = 123.456f).stringifyJson())
        SampleInfoRepository.init(project)

        val reloaded = load(project)

        assertEquals(123.456f, reloaded.lengthMillis)
    }

    @Test
    fun testFileCacheEvictedOnAlgorithmVersionMismatch() {
        val project = createProject()
        SampleInfoRepository.init(project)
        val info = load(project)

        val backup = tempDir.resolve("backup")
        project.sampleInfoDir.copyRecursively(backup)
        SampleInfoRepository.clear(project)
        backup.copyRecursively(project.sampleInfoDir)
        project.infoFile.writeText(info.copy(algorithmVersion = -1, lengthMillis = 123.456f).stringifyJson())
        SampleInfoRepository.init(project)

        val reloaded = load(project)

        // the stale cache is not returned; the sample is reloaded from the wav file and the cache file is rewritten
        assertEquals(WAVE_LOADING_ALGORITHM_VERSION, reloaded.algorithmVersion)
        assertNotEquals(123.456f, reloaded.lengthMillis)
        assertEquals(reloaded, project.infoFile.readText().parseJson<SampleInfo>())
    }

    @Test
    fun testClearRemovesDirectory() {
        val project = createProject()
        SampleInfoRepository.init(project)
        load(project)
        assertTrue(project.sampleInfoDir.isDirectory)

        SampleInfoRepository.clear(project)

        assertFalse(project.sampleInfoDir.exists())
    }

    @Test
    fun testMoveToCopiesCache() {
        val oldDir = tempDir.resolve("old").also { it.mkdirs() }
        oldDir.resolve("sample-info").also { it.mkdirs() }.resolve("a.info.json").writeText("a")
        val newDir = tempDir.resolve("new")

        SampleInfoRepository.moveTo(oldDir, newDir, clearOld = false)

        assertEquals("a", newDir.resolve("sample-info").resolve("a.info.json").readText())
        assertTrue(oldDir.resolve("sample-info").resolve("a.info.json").exists())
    }

    @Test
    fun testMoveToClearsOldCache() {
        val oldDir = tempDir.resolve("old").also { it.mkdirs() }
        oldDir.resolve("sample-info").also { it.mkdirs() }.resolve("a.info.json").writeText("a")
        val newDir = tempDir.resolve("new")

        SampleInfoRepository.moveTo(oldDir, newDir, clearOld = true)

        assertEquals("a", newDir.resolve("sample-info").resolve("a.info.json").readText())
        assertFalse(oldDir.resolve("sample-info").exists())
    }

    @Test
    fun testMoveToWithoutExistingCacheIsNoOp() {
        val oldDir = tempDir.resolve("old").also { it.mkdirs() }
        val newDir = tempDir.resolve("new")

        SampleInfoRepository.moveTo(oldDir, newDir, clearOld = true)

        assertFalse(newDir.exists())
    }
}
