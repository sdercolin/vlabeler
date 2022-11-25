package com.sdercolin.vlabeler.video

import java.io.File
import java.io.FileNotFoundException

sealed interface VideoFinder {
    fun find(path: String, acceptableExtensions: List<String>): String
}

enum class FindVideoStrategy : VideoFinder {
    SamePlaceOfReferenceAudio {
        override fun find(path: String, acceptableExtensions: List<String>): String {
            val fileName = path.substringBeforeLast('.')
            return acceptableExtensions.asSequence().map { fileName + it }.filter { File(it).isFile }.firstOrNull() ?: (
                throw FileNotFoundException(
                    "Video not found at the same place of audio ($path)" +
                        "with name $fileName and extensions among $acceptableExtensions",
                )
                )
        }
    },

    UserSpecifyLocation {
        override fun find(path: String, acceptableExtensions: List<String>): String {
            TODO("Not yet implemented")
        }
    },
}
