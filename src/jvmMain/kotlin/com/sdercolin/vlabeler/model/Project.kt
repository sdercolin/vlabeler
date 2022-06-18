package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.io.parseRawLabels
import kotlinx.serialization.Serializable
import java.io.File
import java.nio.charset.Charset

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

        fun from(
            sampleDirectory: String,
            workingDirectory: String,
            projectName: String,
            labelerConf: LabelerConf,
            inputLabelFile: String,
            encoding: String
        ): Project {
            val sampleDirectoryFile = File(sampleDirectory)
            val sampleNames = sampleDirectoryFile.listFiles().orEmpty()
                .filter { it.extension == SampleFileExtension }
                .map { it.nameWithoutExtension }
                .sorted()

            val parser = labelerConf.parser
            val inputFile = if (inputLabelFile != "") {
                File(inputLabelFile)
            } else null

            val entriesBySample = if (inputFile != null) {
                parseRawLabels(inputFile.readLines(Charset.forName(encoding)), parser!!)
            } else {
                val start = labelerConf.defaultValues.first()
                val end = labelerConf.defaultValues.last()
                val fields = labelerConf.defaultValues.drop(1).dropLast(1)
                sampleNames.associateWith {
                    listOf(Entry(it, start, end, fields))
                }
            }

            return Project(
                sampleDirectory = sampleDirectory,
                workingDirectory = workingDirectory,
                projectName = projectName,
                entriesBySampleName = entriesBySample,
                labelerConf = labelerConf,
                currentSampleName = sampleNames.firstOrNull(),
                currentEntryIndex = 0
            )
        }
    }
}
