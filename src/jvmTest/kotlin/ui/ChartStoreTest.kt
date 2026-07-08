package ui

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.repository.ChartRepository
import com.sdercolin.vlabeler.ui.editor.ChartStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import testutil.TestEnv
import testutil.TestFixtures
import testutil.TestLabelers
import testutil.TestWav
import testutil.createTestProject
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [ChartStore], driven by real wav files loaded through the real sample loading path
 * ([SampleInfo.load] and `loadSampleChunk` inside [ChartStore.load]).
 *
 * The charts are rendered into the [ChartRepository] singleton, which points into the per-test project's cache
 * directory and is cleared in the teardown to keep the tests order-independent.
 */
class ChartStoreTest {

    private lateinit var scope: CoroutineScope
    private lateinit var tempDir: File
    private var project: Project? = null

    @BeforeTest
    fun setup() {
        TestEnv.ensureLogDirectory()
        Log.muted = true
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        tempDir = createTempDirectory("vlabeler-test").toFile()
    }

    @AfterTest
    fun teardown() {
        scope.cancel()
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

    private fun loadSampleInfo(project: Project, file: File, appConf: AppConf): SampleInfo = runBlocking {
        SampleInfo.load(project, moduleName = "", file = file, appConf = appConf).getOrThrow()
    }

    /**
     * Runs [ChartStore.load] and joins the rendering job deterministically via [ChartStore.awaitLoad].
     */
    private fun render(
        store: ChartStore,
        project: Project,
        info: SampleInfo,
        appConf: AppConf,
        startingChunkIndex: Int = 0,
        onRenderProgress: suspend () -> Unit = {},
    ) {
        store.load(
            scope = scope,
            project = project,
            sampleInfo = info,
            appConf = appConf,
            density = Density(1f),
            layoutDirection = LayoutDirection.Ltr,
            startingChunkIndex = startingChunkIndex,
            onRenderProgress = onRenderProgress,
        )
        runBlocking { store.awaitLoad() }
    }

    @Test
    fun `prepareForNewLoading initializes statuses and the chart repository`() {
        val project = createProject()
        val appConf = AppConf()
        val info = loadSampleInfo(project, project.rootSampleDirectory.resolve("_a_ka.wav"), appConf)
        val store = ChartStore()

        assertTrue(store.prepareForNewLoading(project, appConf, info))
        assertEquals(ChartStore.ChartLoadingStatus.Loading, store.getWaveformStatus(0, 0))
        assertEquals(ChartStore.ChartLoadingStatus.Loading, store.getSpectrogramStatus(0))
        // power and fundamental statuses are only written when the corresponding charts are rendered
        assertNull(store.getPowerGraphStatus(0, 0))
        assertNull(store.getFundamentalGraphStatus(0))
        assertTrue(project.cacheDirectory.resolve("charts").resolve("params.json").isFile)

        // the same sample with the same painter parameters does not need a new loading
        assertFalse(store.prepareForNewLoading(project, appConf, info))

        // a painter configuration change requires a reset even for the same sample
        val changedConf = appConf.copy(
            painter = appConf.painter.copy(
                amplitude = appConf.painter.amplitude.copy(
                    normalize = appConf.painter.amplitude.normalize.not(),
                ),
            ),
        )
        assertTrue(store.prepareForNewLoading(project, changedConf, info))
    }

    @Test
    fun `prepareForNewLoading resets when another sample is loaded`() {
        val project = createProject()
        val appConf = AppConf()
        val otherWav = project.rootSampleDirectory.resolve("other.wav")
        TestWav.write(otherWav, durationMs = 500)
        val store = ChartStore()

        val infoA = loadSampleInfo(project, project.rootSampleDirectory.resolve("_a_ka.wav"), appConf)
        assertTrue(store.prepareForNewLoading(project, appConf, infoA))

        val infoB = loadSampleInfo(project, otherWav, appConf)
        assertTrue(store.prepareForNewLoading(project, appConf, infoB))
        assertEquals(ChartStore.ChartLoadingStatus.Loading, store.getWaveformStatus(0, 0))
    }

    @Test
    fun `load renders all charts into the repository and reports progress`() {
        val project = createProject()
        val wav = project.rootSampleDirectory.resolve("sine.wav")
        TestWav.write(wav, durationMs = 1000, sampleRate = 44100, frequency = 440.0)
        val appConf = AppConf().run {
            copy(
                painter = painter.copy(
                    power = painter.power.copy(enabled = true),
                    fundamental = painter.fundamental.copy(enabled = true),
                ),
            )
        }
        val info = loadSampleInfo(project, wav, appConf)
        assertTrue(info.hasSpectrogram)
        assertTrue(info.hasPower)
        assertTrue(info.hasFundamental)
        assertEquals(4, info.totalChartCount)

        val store = ChartStore()
        store.prepareForNewLoading(project, appConf, info)
        assertFalse(store.hasCachedSample(info))

        val progressCount = AtomicInteger(0)
        render(store, project, info, appConf) { progressCount.incrementAndGet() }

        assertEquals(info.totalChartCount, progressCount.get())
        assertEquals(ChartStore.ChartLoadingStatus.Loaded, store.getWaveformStatus(0, 0))
        assertEquals(ChartStore.ChartLoadingStatus.Loaded, store.getSpectrogramStatus(0))
        assertEquals(ChartStore.ChartLoadingStatus.Loaded, store.getPowerGraphStatus(0, 0))
        assertEquals(ChartStore.ChartLoadingStatus.Loaded, store.getFundamentalGraphStatus(0))
        assertTrue(ChartRepository.getWaveformImageFile(info, channelIndex = 0, chunkIndex = 0).isFile)
        assertTrue(ChartRepository.getSpectrogramImageFile(info, chunkIndex = 0).isFile)
        assertTrue(ChartRepository.getPowerGraphImageFile(info, channelIndex = 0, chunkIndex = 0).isFile)
        assertTrue(ChartRepository.getFundamentalGraphImageFile(info, chunkIndex = 0).isFile)
        assertTrue(store.hasCachedSample(info))
    }

    @Test
    fun `load reuses cached chart files without rerendering`() {
        val project = createProject()
        val appConf = AppConf()
        val info = loadSampleInfo(project, project.rootSampleDirectory.resolve("_a_ka.wav"), appConf)
        val firstStore = ChartStore()
        assertTrue(firstStore.prepareForNewLoading(project, appConf, info))
        render(firstStore, project, info, appConf)
        val waveformFile = ChartRepository.getWaveformImageFile(info, channelIndex = 0, chunkIndex = 0)
        val spectrogramFile = ChartRepository.getSpectrogramImageFile(info, chunkIndex = 0)
        val stamps = listOf(waveformFile.lastModified(), spectrogramFile.lastModified())

        // a new store instance always initializes its statuses, but the chart files are kept
        val secondStore = ChartStore()
        assertTrue(secondStore.prepareForNewLoading(project, appConf, info))
        assertTrue(secondStore.hasCachedSample(info))

        val progressCount = AtomicInteger(0)
        render(secondStore, project, info, appConf) { progressCount.incrementAndGet() }

        // the cached branch reports progress for every chart as well
        assertEquals(info.totalChartCount, progressCount.get())
        assertEquals(ChartStore.ChartLoadingStatus.Loaded, secondStore.getWaveformStatus(0, 0))
        assertEquals(ChartStore.ChartLoadingStatus.Loaded, secondStore.getSpectrogramStatus(0))
        assertEquals(stamps, listOf(waveformFile.lastModified(), spectrogramFile.lastModified()))
    }

    @Test
    fun `load renders the chunks around the starting chunk first`() {
        val project = createProject()
        val wav = project.rootSampleDirectory.resolve("chunked.wav")
        TestWav.write(wav, durationMs = 3000, sampleRate = 8000)
        val appConf = AppConf().run {
            copy(
                painter = painter.copy(
                    maxDataChunkSize = 8000,
                    spectrogram = painter.spectrogram.copy(enabled = false),
                ),
            )
        }
        val info = loadSampleInfo(project, wav, appConf)
        assertEquals(3, info.chunkCount)

        // with one channel and only waveforms enabled, each progress callback completes exactly one chunk,
        // so the order in which the chunk statuses turn loaded is the rendering order
        fun renderAndRecordOrder(startingChunkIndex: Int): List<Int> {
            val store = ChartStore()
            assertTrue(store.prepareForNewLoading(project, appConf, info))
            val loadedOrder = mutableListOf<Int>()
            render(store, project, info, appConf, startingChunkIndex) {
                (0 until info.chunkCount)
                    .filter { store.getWaveformStatus(0, it) == ChartStore.ChartLoadingStatus.Loaded }
                    .filterNot { it in loadedOrder }
                    .forEach { loadedOrder.add(it) }
            }
            return loadedOrder
        }

        assertEquals(listOf(1, 0, 2), renderAndRecordOrder(startingChunkIndex = 1))
        assertEquals(listOf(2, 1, 0), renderAndRecordOrder(startingChunkIndex = 2))
        assertEquals(listOf(0, 1, 2), renderAndRecordOrder(startingChunkIndex = 0))
    }

    @Test
    fun `clear resets the statuses and drops the loading job`() {
        val project = createProject()
        val appConf = AppConf()
        val info = loadSampleInfo(project, project.rootSampleDirectory.resolve("_a_ka.wav"), appConf)
        val store = ChartStore()

        // clearing and awaiting an empty store is a no-op
        store.clear()
        runBlocking { store.awaitLoad() }
        assertNull(store.getWaveformStatus(0, 0))

        store.prepareForNewLoading(project, appConf, info)
        render(store, project, info, appConf)
        assertEquals(ChartStore.ChartLoadingStatus.Loaded, store.getWaveformStatus(0, 0))

        store.clear()
        assertNull(store.getWaveformStatus(0, 0))
        assertNull(store.getSpectrogramStatus(0))
        assertNull(store.getPowerGraphStatus(0, 0))
        assertNull(store.getFundamentalGraphStatus(0))
        runBlocking { store.awaitLoad() }
    }
}
