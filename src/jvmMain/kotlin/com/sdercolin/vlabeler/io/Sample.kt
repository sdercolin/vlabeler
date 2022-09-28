package com.sdercolin.vlabeler.io

import java.io.File

object Sample {

    fun listSampleFiles(sampleDirectory: File): List<File> {
        return sampleDirectory.listFiles().orEmpty().filter { it.extension in acceptableSampleFileExtensions }
    }

    private const val SampleFileWavExtension = "wav"
    val acceptableSampleFileExtensions = listOf(SampleFileWavExtension)
}
