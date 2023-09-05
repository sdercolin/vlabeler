package com.sdercolin.vlabeler.io

import java.io.File

/**
 * An object to list sample files in a directory.
 */
object Sample {

    fun listSampleFiles(sampleDirectory: File): List<File> {
        return sampleDirectory.listFiles().orEmpty().filter { it.extension in acceptableSampleFileExtensions }
    }

    val acceptableSampleFileExtensions = listOf("wav", "mp3", "flac", "ogg", "m4a")
}
