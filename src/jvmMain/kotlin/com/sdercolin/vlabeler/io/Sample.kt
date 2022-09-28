package com.sdercolin.vlabeler.io

import java.io.File

object Sample {

    fun listSampleFiles(sampleDirectory: File): List<File> {
        val names = sampleDirectory.listFiles()?.map { it.nameWithoutExtension }?.distinct().orEmpty()
        return names.mapNotNull { findSampleFile(sampleDirectory, it) }
    }

    fun findSampleFile(sampleDirectory: File, sampleName: String): File? {
        for (extension in acceptableSampleFileExtensions) {
            val file = File(sampleDirectory, "$sampleName.$extension")
            if (file.exists() && file.isFile) return file
        }
        return null
    }

    const val SampleFileWavExtension = "wav"
    val acceptableSampleFileExtensions = listOf(SampleFileWavExtension)
}
