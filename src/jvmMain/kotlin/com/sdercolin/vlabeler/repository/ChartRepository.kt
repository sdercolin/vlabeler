package com.sdercolin.vlabeler.repository

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.res.loadImageBitmap
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.SampleInfo
import com.sdercolin.vlabeler.util.getCacheDir
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import org.jetbrains.skiko.toBufferedImage
import java.io.File
import javax.imageio.ImageIO

@Stable
object ChartRepository {

    private lateinit var cacheDirectory: File
    private val cacheParamsFile get() = cacheDirectory.resolve("params.json")
    private lateinit var cacheParams: ChartCacheParams

    fun init(project: Project, appConf: AppConf, version: Int) {
        cacheDirectory = project.getCacheDir().resolve(ChartsCacheFolderName)
        cacheDirectory.mkdirs()
        cacheParams = ChartCacheParams(
            version,
            appConf.painter,
        )
        val existingCacheParams = runCatching {
            cacheParamsFile.takeIf { it.exists() }?.readText()?.parseJson<ChartCacheParams>()
        }.getOrNull()
        if (existingCacheParams != cacheParams) {
            cacheDirectory.deleteRecursively()
            cacheDirectory.mkdirs()
            cacheParamsFile.writeText(cacheParams.stringifyJson())
        }
    }

    suspend fun getWaveform(sampleInfo: SampleInfo, channelIndex: Int, chunkIndex: Int): ImageBitmap {
        val file = getWaveformImageFile(sampleInfo, channelIndex, chunkIndex)
        waitingFile(file)
        return file.inputStream().buffered().use(::loadImageBitmap)
    }

    suspend fun getSpectrogram(sampleInfo: SampleInfo, chunkIndex: Int): ImageBitmap {
        val file = getSpectrogramImageFile(sampleInfo, chunkIndex)
        waitingFile(file)
        return file.inputStream().buffered().use(::loadImageBitmap)
    }

    private suspend fun waitingFile(file: File) {
        while (file.exists().not()) {
            Log.info("Waiting for $file to be created")
            delay(100)
        }
    }

    fun putWaveform(
        sampleInfo: SampleInfo,
        channelIndex: Int,
        chunkIndex: Int,
        waveform: ImageBitmap
    ) {
        val file = getWaveformImageFile(sampleInfo, channelIndex, chunkIndex)
        saveImage(waveform, file)
    }

    fun putSpectrogram(sampleInfo: SampleInfo, chunkIndex: Int, spectrogram: ImageBitmap) {
        val file = getSpectrogramImageFile(sampleInfo, chunkIndex)
        saveImage(spectrogram, file)
    }

    private fun saveImage(image: ImageBitmap, file: File) {
        if (file.parentFile.exists().not()) {
            file.parentFile.mkdirs()
        }
        val outputStream = file.outputStream()
        ImageIO.write(image.asSkiaBitmap().toBufferedImage(), "png", outputStream)
        outputStream.flush()
        outputStream.close()
        Log.debug("Written to $file")
    }

    fun getWaveformImageFile(
        sampleInfo: SampleInfo,
        channelIndex: Int,
        chunkIndex: Int
    ) = cacheDirectory.resolve("${sampleInfo.name}_waveform_${channelIndex}_$chunkIndex.png")

    fun getSpectrogramImageFile(
        sampleInfo: SampleInfo,
        chunkIndex: Int
    ) = cacheDirectory.resolve("${sampleInfo.name}_spectrogram_$chunkIndex.png")

    private const val ChartsCacheFolderName = "charts"
}

@Serializable
data class ChartCacheParams(
    val algorithmVersion: Int,
    val painterConfig: AppConf.Painter
)
