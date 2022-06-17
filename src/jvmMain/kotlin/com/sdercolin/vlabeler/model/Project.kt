package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
@Immutable
data class Project(
    val sampleDirectory: String,
    val workingDirectory: String,
    val projectName: String,
    val entriesBySampleName: Map<String, List<Entry>>,
    val labelerConf: LabelerConf,
    val currentSampleName: String?,
    val currentEntryIndex: Int
) {

    val currentSampleFile: File?
        get() = currentSampleName?.let {
            File(sampleDirectory).resolve("$it.$SampleFileExtension")
        }

    val projectFile: File
        get() = File(workingDirectory).resolve("$projectName.$ProjectFileExtension")

    companion object {
        const val SampleFileExtension = "wav"
        const val ProjectFileExtension = "lbp"

        fun fromSingleFile(
            directoryPath: String,
            fileName: String,
            labelerConf: LabelerConf
        ): Project {
            val file = File(directoryPath, fileName)
            return Project(
                sampleDirectory = directoryPath,
                workingDirectory = directoryPath,
                projectName = file.nameWithoutExtension,
                entriesBySampleName = mapOf(
                    file.nameWithoutExtension to listOf(
                        Entry("i あ", 2615f, 3315f, listOf(3055f, 2915f, 2715f))
                    )
                ),
                labelerConf,
                currentSampleName = file.nameWithoutExtension,
                currentEntryIndex = 0
            )
        }
    }
}
