package com.sdercolin.vlabeler.audio.conversion

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.util.CommandLine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A wrapper of FFmpeg to convert audio files.
 */
class FFmpegConverter : WaveConverter {

    override fun accept(inputFile: File, conf: AppConf.Conversion): Boolean {
        return inputFile.isFile && (inputFile.extension != "wav" || conf.useConversionForWav)
    }

    override suspend fun convert(inputFile: File, outputFile: File, conf: AppConf.Conversion) {
        withContext(Dispatchers.IO) {
            try {
                val commandSections = mutableListOf<String>(conf.ffmpegPath, "-i", inputFile.absolutePath)
                if (conf.ffmpegArgs.isNotBlank()) {
                    commandSections.addAll(conf.ffmpegArgs.split(" "))
                }
                commandSections.add(outputFile.absolutePath)
                val exitValue = CommandLine.execute(commandSections)
                if (exitValue != 0) {
                    throw Exception.CommandExecutionFailed(exitValue)
                }
                if (!outputFile.isFile) {
                    throw Exception.ResultFileNotFound(outputFile)
                }
            } catch (t: Throwable) {
                if (t is CancellationException) {
                    return@withContext // ignore cancellation
                }
                if (t is Exception) {
                    throw t
                }
                throw Exception.UnexpectedError(t)
            }
        }
    }

    sealed class Exception(cause: Throwable?) : WaveConverterException(Strings.FFmpegConverterException, cause) {

        class ResultFileNotFound(file: File) :
            Exception(IllegalStateException("The expected output file is not existing: ${file.absolutePath}"))

        class CommandExecutionFailed(exitValue: Int) :
            Exception(IllegalStateException("FFmpeg exited with error code: $exitValue"))

        class UnexpectedError(cause: Throwable?) : Exception(cause)
    }
}
