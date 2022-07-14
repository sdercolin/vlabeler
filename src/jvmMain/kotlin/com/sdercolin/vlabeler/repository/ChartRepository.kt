package com.sdercolin.vlabeler.repository

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.res.loadImageBitmap
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.SampleInfo
import kotlinx.coroutines.delay
import org.jetbrains.skiko.toBufferedImage
import java.io.File
import javax.imageio.ImageIO

@Stable
object ChartRepository {

    private lateinit var workingDirectory: File

    fun setWorkingDirectory(workingDirectory: File) {
        this.workingDirectory = workingDirectory
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

    fun clear() {
        // TODO: clear the working directory at a correct time
    }

    fun getWaveformImageFile(
        sampleInfo: SampleInfo,
        channelIndex: Int,
        chunkIndex: Int
    ) = workingDirectory.resolve(".images").resolve(
        "${sampleInfo.name}_waveform_${channelIndex}_$chunkIndex.png"
    )

    fun getSpectrogramImageFile(
        sampleInfo: SampleInfo,
        chunkIndex: Int
    ) = workingDirectory.resolve(".images").resolve(
        "${sampleInfo.name}_spectrogram_$chunkIndex.png"
    )
}
