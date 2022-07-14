package com.sdercolin.vlabeler.repository

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.res.loadImageBitmap
import com.sdercolin.vlabeler.model.SampleInfo
import org.jetbrains.skiko.toBufferedImage
import java.io.File
import javax.imageio.ImageIO

@Stable
object ChartRepository {

    private lateinit var workingDirectory: File

    fun setWorkingDirectory(workingDirectory: File) {
        this.workingDirectory = workingDirectory
    }

    fun getWaveform(sampleInfo: SampleInfo, channelIndex: Int, chunkIndex: Int): ImageBitmap {
        val file = getWaveformImageFile(workingDirectory, sampleInfo, channelIndex, chunkIndex)
        return file.inputStream().buffered().use(::loadImageBitmap)
    }

    fun getSpectrogram(sampleInfo: SampleInfo, chunkIndex: Int): ImageBitmap {
        val file = getSpectrogramImageFile(workingDirectory, sampleInfo, chunkIndex)
        return file.inputStream().buffered().use(::loadImageBitmap)
    }

    fun putWaveform(
        sampleInfo: SampleInfo,
        channelIndex: Int,
        chunkIndex: Int,
        waveform: ImageBitmap
    ) {
        val file = getWaveformImageFile(workingDirectory, sampleInfo, channelIndex, chunkIndex)
        saveImage(waveform, file)
    }

    fun putSpectrogram(sampleInfo: SampleInfo, chunkIndex: Int, spectrogram: ImageBitmap) {
        val file = getSpectrogramImageFile(workingDirectory, sampleInfo, chunkIndex)
        saveImage(spectrogram, file)
    }

    private fun saveImage(image: ImageBitmap, file: File) {
        if (file.parentFile.exists().not()) {
            file.parentFile.mkdirs()
        }
        ImageIO.write(image.asSkiaBitmap().toBufferedImage(), "png", file)
    }

    fun clear() {
        // TODO: clear the working directory at a correct time
    }

    private fun getWaveformImageFile(
        workingDirectory: File,
        sampleInfo: SampleInfo,
        channelIndex: Int,
        chunkIndex: Int
    ) = workingDirectory.resolve(".images").resolve(
        "${sampleInfo.name}_waveform_${channelIndex}_$chunkIndex.png"
    )

    private fun getSpectrogramImageFile(
        workingDirectory: File,
        sampleInfo: SampleInfo,
        chunkIndex: Int
    ) = workingDirectory.resolve(".images").resolve(
        "${sampleInfo.name}_spectrogram_$chunkIndex.png"
    )
}
