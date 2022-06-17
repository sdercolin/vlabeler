package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import java.io.File

@Immutable
data class Project(
    val workingDirectory: File,
    val entriesBySampleName: Map<String, List<Entry>>,
    val appConf: AppConf,
    val labelerConf: LabelerConf,
    val currentSampleName: String,
    val currentEntryIndex: Int
) {

    val currentSampleFile get() = workingDirectory.resolve(currentSampleName + SampleFileExtension)

    companion object {
        const val SampleFileExtension = ".wav"

        fun fromSingleFile(
            directoryPath: String,
            fileName: String,
            appConf: AppConf,
            labelerConf: LabelerConf
        ): Project {
            val file = File(directoryPath, fileName)
            return Project(
                workingDirectory = File(directoryPath),
                entriesBySampleName = mapOf(
                    file.nameWithoutExtension to listOf(
                        Entry("i あ", 2615f, 3315f, listOf(3055f, 2915f, 2715f))
                    )
                ),
                appConf,
                labelerConf,
                currentSampleName = file.nameWithoutExtension,
                currentEntryIndex = 0
            )
        }
    }
}
