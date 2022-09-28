package com.sdercolin.vlabeler.model

import com.sdercolin.vlabeler.util.toFile
import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class RawModuleDefinition(
    val name: String,
    val sampleDirectoryPath: String,
    val sampleFileNames: List<String>,
    val inputFilePaths: List<String>? = null,
    val labelFilePath: String? = null,
) {
    fun toModuleDefinition(): ModuleDefinition {
        val sampleDirectory = File(sampleDirectoryPath)
        val sampleFiles = sampleFileNames.map { sampleDirectory.resolve(it) }
        val sampleNames = sampleFiles.map { it.nameWithoutExtension }
        val inputFiles = inputFilePaths?.map { File(it) }
        val labelFile = labelFilePath?.toFile()
        return ModuleDefinition(name, sampleDirectory, sampleFiles, sampleNames, inputFiles, labelFile)
    }
}

class ModuleDefinition(
    val name: String,
    val sampleDirectory: File,
    val sampleFiles: List<File>,
    val sampleNames: List<String>,
    val inputFiles: List<File>?,
    val labelFile: File?,
)
