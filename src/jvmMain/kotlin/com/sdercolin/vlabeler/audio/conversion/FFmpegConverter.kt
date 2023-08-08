package com.sdercolin.vlabeler.audio.conversion

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.util.CommandLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A wrapper of FFmpeg to convert audio files.
 */
class FFmpegConverter : WaveConverter {

    override fun accept(inputFile: File): Boolean {
        return inputFile.extension != "wav"
    }

    override suspend fun convert(inputFile: File, outputFile: File, conf: AppConf.Conversion) {
        withContext(Dispatchers.IO) {
            val commandSections = mutableListOf<String>(conf.ffmpegPath, "-i", inputFile.absolutePath)
            if (conf.ffmpegArgs.isNotBlank()) {
                commandSections.addAll(conf.ffmpegArgs.split(" "))
            }
            commandSections.add(outputFile.absolutePath)
            CommandLine.execute(commandSections)
        }
    }
}
