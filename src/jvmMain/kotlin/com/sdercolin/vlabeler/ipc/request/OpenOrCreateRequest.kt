@file:Suppress("MemberVisibilityCanBePrivate")

package com.sdercolin.vlabeler.ipc.request

import com.sdercolin.vlabeler.ipc.IpcMessageType
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.projectOf
import com.sdercolin.vlabeler.util.AvailableEncodings
import com.sdercolin.vlabeler.util.ParamTypedMap
import com.sdercolin.vlabeler.util.encodingNameEquals
import com.sdercolin.vlabeler.util.resolve
import com.sdercolin.vlabeler.util.toFile
import com.sdercolin.vlabeler.util.toFileOrNull
import com.sdercolin.vlabeler.util.toParamTypedMap
import io.ktor.utils.io.charsets.name
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import java.io.File
import java.nio.charset.Charset

@Serializable
@SerialName("OpenOrCreate")
class OpenOrCreateRequest(
    val projectFile: String, // absolute path
    val gotoEntryByName: GotoEntryByName? = null,
    val gotoEntryByIndex: GotoEntryByIndex? = null, // index is used in higher priority
    val newProjectArgs: NewProjectArgs,
    val sentAt: Long,
) : IpcRequest() {

    @Serializable
    class GotoEntryByName(
        val parentFolderName: String, // "" for single-module project, or root of multi-module project
        val entryName: String,
    )

    @Serializable
    class GotoEntryByIndex(
        val parentFolderName: String, // "" for single-module project, or root of multi-module project
        val entryIndex: Int,
    )

    @Serializable
    class NewProjectArgs(
        val labelerName: String,
        val sampleDirectory: String? = null, // absolute path, defaults to project location
        val cacheDirectory: String? = null, // absolute path
        val labelerParams: Map<String, ParamTypedMap.TypedValue>? = null,
        val pluginName: String? = null,
        val pluginParams: Map<String, ParamTypedMap.TypedValue>? = null,
        val inputFile: String? = null, // absolute path
        val encoding: String = Charset.defaultCharset().name(), // defaults to UTF-8
        val autoExport: Boolean = false,
    ) {

        suspend fun create(
            projectFile: File,
            availableLabelers: List<LabelerConf>,
            availableTemplatePlugins: List<Plugin>,
        ): Result<Project> {
            val workingDirectory = projectFile.parent
            val projectName = projectFile.nameWithoutExtension
            val sampleDirectory = this.sampleDirectory?.toFileOrNull(ensureIsDirectory = true, ensureExists = true)
                ?.absolutePath
                ?: workingDirectory
            val cacheDirectory = this.cacheDirectory?.toFile()
                ?.takeIf { it.parentFile?.exists() == true && it.parentFile?.isDirectory == true }
                ?.absolutePath
                ?: Project.getDefaultCacheDirectory(workingDirectory, projectName)
            val labelerConf = availableLabelers.find { it.name == labelerName }
                ?: return Result.failure(IllegalArgumentException("Labeler $labelerName not found"))
            val plugin = if (pluginName != null) {
                availableTemplatePlugins.find { it.name == pluginName }
                    ?: return Result.failure(IllegalArgumentException("Template plugin $pluginName not found"))
            } else null
            return projectOf(
                sampleDirectory = sampleDirectory,
                workingDirectory = workingDirectory,
                projectName = projectName,
                cacheDirectory = cacheDirectory,
                rawLabelerConf = labelerConf,
                labelerParams = labelerParams?.toParamTypedMap().resolve(labelerConf),
                plugin = plugin,
                pluginParams = plugin?.let { pluginParams?.toParamTypedMap().resolve(it) },
                inputFilePath = inputFile,
                encoding = AvailableEncodings.find { encodingNameEquals(it, encoding) }
                    ?: Charset.defaultCharset().name,
                autoExport = autoExport,
            )
        }
    }

    @Transient
    override val type: IpcMessageType = IpcMessageType.OpenOrCreate
}
