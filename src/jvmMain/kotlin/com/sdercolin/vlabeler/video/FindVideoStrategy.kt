package com.sdercolin.vlabeler.video

import java.io.File
import java.io.FileNotFoundException

/**
 * Represents the logic to find video file from audio file.
 */
sealed interface FindVideoStrategy {
    fun find(path: String, acceptableExtensions: List<String>): Result<String>

    object SamePlaceOfReferenceAudio : FindVideoStrategy {
        override fun find(path: String, acceptableExtensions: List<String>): Result<String> {
            val fileName = path.substringBeforeLast('.')
            val existingFilePath =
                acceptableExtensions.asSequence().map { fileName + it }.filter { File(it).isFile }.firstOrNull()
            return if (existingFilePath != null) {
                Result.success(existingFilePath)
            } else {
                Result.failure(FileNotFoundException())
            }
        }
    }

    // TODO: implement other strategies such as `UserSpecifyLocation`
}
