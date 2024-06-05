package com.sdercolin.vlabeler.repository

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.res.loadImageBitmap
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.util.deleteRecursivelyLogged
import com.sdercolin.vlabeler.util.findUnusedFile
import com.sdercolin.vlabeler.util.getCacheDir
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.jetbrains.skiko.toBufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Repository for charts.
 */
@Stable
object ChartRepository {

    private lateinit var cacheDirectory: File
    private val cacheParamsFile get() = cacheDirectory.resolve("params.json")
    private lateinit var cacheParams: ChartCacheParams
    private val cacheMapFile get() = cacheDirectory.resolve("map.json")
    private var cacheMap: MutableMap<String, String> = mutableMapOf()

    /**
     * @param appConf Current app configuration.
     * @param version Version of the painting algorithm.
     * @return true if previous cache is not consistent with current app configuration.
     */
    fun needReset(appConf: AppConf, version: Int): Boolean {
        if (!::cacheParams.isInitialized) return true
        val params = ChartCacheParams(version, appConf.painter)
        return params != cacheParams
    }

    /**
     * Initialize the repository.
     *
     * @param project Current project.
     * @param appConf Current app configuration.
     * @param version Version of the painting algorithm.
     */
    fun init(project: Project, appConf: AppConf, version: Int) {
        cacheDirectory = project.getCacheDir().resolve(CHARTS_CACHE_FOLDER_NAME)
        cacheDirectory.mkdirs()
        cacheParams = ChartCacheParams(
            version,
            appConf.painter,
        )
        val existingCacheParams = runCatching {
            cacheParamsFile.takeIf { it.exists() }?.readText()?.parseJson<ChartCacheParams>()
        }.getOrNull()
        if (existingCacheParams != cacheParams) {
            cacheDirectory.deleteRecursivelyLogged()
            cacheDirectory.mkdirs()
            cacheParamsFile.writeText(cacheParams.stringifyJson())
        }
        cacheMap = runCatching {
            cacheMapFile.takeIf { it.exists() }?.readText()?.parseJson<Map<String, String>>()?.toMutableMap()
        }.getOrNull() ?: mutableMapOf()
    }

    /**
     * Get the waveform image of a chunk.
     */
    suspend fun getWaveform(sampleInfo: SampleInfo, channelIndex: Int, chunkIndex: Int): ImageBitmap {
        val file = getWaveformImageFile(sampleInfo, channelIndex, chunkIndex)
        waitingFile(file)
        return file.inputStream().buffered().use(::loadImageBitmap)
    }

    /**
     * Get the spectrogram image of a chunk.
     */
    suspend fun getSpectrogram(sampleInfo: SampleInfo, chunkIndex: Int): ImageBitmap {
        val file = getSpectrogramImageFile(sampleInfo, chunkIndex)
        waitingFile(file)
        return file.inputStream().buffered().use(::loadImageBitmap)
    }

    /**
     * Get the power graph image of a chunk.
     */
    suspend fun getPowerGraph(sampleInfo: SampleInfo, channelIndex: Int, chunkIndex: Int): ImageBitmap {
        val file = getPowerGraphImageFile(sampleInfo, channelIndex, chunkIndex)
        waitingFile(file)
        return file.inputStream().buffered().use(::loadImageBitmap)
    }

    private suspend fun waitingFile(file: File) {
        while (file.exists().not()) {
            Log.info("Waiting for $file to be created")
            delay(100)
        }
    }

    /**
     * Put the waveform image of a chunk.
     */
    fun putWaveform(
        sampleInfo: SampleInfo,
        channelIndex: Int,
        chunkIndex: Int,
        waveform: ImageBitmap,
    ) {
        val file = getWaveformImageFile(sampleInfo, channelIndex, chunkIndex)
        saveImage(waveform, file, sampleInfo.getCacheKey(KEY_WAVEFORM, channelIndex, chunkIndex))
    }

    /**
     * Put the spectrogram image of a chunk.
     */
    fun putSpectrogram(sampleInfo: SampleInfo, chunkIndex: Int, spectrogram: ImageBitmap) {
        val file = getSpectrogramImageFile(sampleInfo, chunkIndex)
        saveImage(spectrogram, file, sampleInfo.getCacheKey(KEY_SPECTROGRAM, chunkIndex))
    }

    /**
     * Put the power graph image of a chunk.
     */
    fun putPowerGraph(
        sampleInfo: SampleInfo,
        channelIndex: Int,
        chunkIndex: Int,
        powerGraph: ImageBitmap,
    ) {
        val file = getPowerGraphImageFile(sampleInfo, channelIndex, chunkIndex)
        saveImage(powerGraph, file, sampleInfo.getCacheKey(KEY_POWER_GRAPH, channelIndex, chunkIndex))
    }

    private fun saveImage(image: ImageBitmap, file: File, cacheKey: String) {
        if (file.parentFile.exists().not()) {
            file.parentFile.mkdirs()
        }
        val outputStream = file.outputStream()
        ImageIO.write(image.asSkiaBitmap().toBufferedImage(), "png", outputStream)
        outputStream.flush()
        outputStream.close()
        cacheMap[cacheKey] = file.relativeTo(cacheDirectory).path.replace(File.separatorChar, '/')
        cacheDirectory.mkdirs()
        cacheMapFile.writeText(cacheMap.stringifyJson())
        Log.debug("Written to $file")
    }

    /**
     * Get the target [File] for the waveform image of a chunk.
     */
    fun getWaveformImageFile(
        sampleInfo: SampleInfo,
        channelIndex: Int,
        chunkIndex: Int,
    ) = cacheMap[sampleInfo.getCacheKey(KEY_WAVEFORM, channelIndex, chunkIndex)]
        ?.let { cacheDirectory.resolve(it) }
        ?.takeIf { it.isFile }
        ?: run {
            val modulePrefix = sampleInfo.moduleName.let { "${it}_" }
            val baseFileName =
                "${modulePrefix}${sampleInfo.name}_${KEY_WAVEFORM}_${channelIndex}_$chunkIndex.$EXTENSION"
            cacheDirectory.findUnusedFile(
                base = baseFileName,
                existingAbsolutePaths = cacheMap.values.map { cacheDirectory.resolve(it).absolutePath }.toSet(),
            )
        }

    /**
     * Get the target [File] for the spectrogram image of a chunk.
     */
    fun getSpectrogramImageFile(
        sampleInfo: SampleInfo,
        chunkIndex: Int,
    ) = cacheMap[sampleInfo.getCacheKey(KEY_SPECTROGRAM, chunkIndex)]
        ?.let { cacheDirectory.resolve(it) }
        ?.takeIf { it.isFile }
        ?: run {
            val modulePrefix = sampleInfo.moduleName.let { "${it}_" }
            val baseFileName = "${modulePrefix}${sampleInfo.name}_${KEY_SPECTROGRAM}_$chunkIndex.$EXTENSION"
            cacheDirectory.findUnusedFile(
                base = baseFileName,
                existingAbsolutePaths = cacheMap.values.map { cacheDirectory.resolve(it).absolutePath }.toSet(),
            )
        }

    /**
     * Get the target [File] for the power graph image of a chunk.
     */
    fun getPowerGraphImageFile(
        sampleInfo: SampleInfo,
        channelIndex: Int,
        chunkIndex: Int,
    ) = cacheMap[sampleInfo.getCacheKey(KEY_POWER_GRAPH, channelIndex, chunkIndex)]
        ?.let { cacheDirectory.resolve(it) }
        ?.takeIf { it.isFile }
        ?: run {
            val baseFileName = "${sampleInfo.name}_${KEY_POWER_GRAPH}_${channelIndex}_$chunkIndex.$EXTENSION"
            cacheDirectory.findUnusedFile(
                base = baseFileName,
                existingAbsolutePaths = cacheMap.values.map { cacheDirectory.resolve(it).absolutePath }.toSet(),
            )
        }

    private fun SampleInfo.getCacheKey(vararg keys: Any): String {
        return (listOf(file) + keys).joinToString(separator = "//")
    }

    fun clear(project: Project) {
        cacheMap.clear()
        project.getCacheDir().resolve(CHARTS_CACHE_FOLDER_NAME).deleteRecursivelyLogged()
    }

    /**
     * Move the cache from the old cache directory to the new cache directory.
     */
    fun moveTo(oldCacheDirectory: File, newCacheDirectory: File, clearOld: Boolean) {
        Log.debug("Moving cache from $oldCacheDirectory to $newCacheDirectory")
        val oldDirectory = oldCacheDirectory.resolve(CHARTS_CACHE_FOLDER_NAME)
        if (oldDirectory.isDirectory.not()) return
        oldDirectory.copyRecursively(newCacheDirectory.resolve(CHARTS_CACHE_FOLDER_NAME), overwrite = true)
        if (clearOld) oldDirectory.deleteRecursivelyLogged()
    }

    private const val CHARTS_CACHE_FOLDER_NAME = "charts"
    private const val KEY_WAVEFORM = "waveform"
    private const val KEY_SPECTROGRAM = "spectrogram"
    private const val KEY_POWER_GRAPH = "power_graph"
    private const val EXTENSION = "png"
}

@Serializable
private data class ChartCacheParams(
    val algorithmVersion: Int,
    val painterConfig: AppConf.Painter,
)
