package com.sdercolin.vlabeler.ipc.request

import com.sdercolin.vlabeler.ipc.IpcMessageType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
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
        val parentFolderName: String, // "" for single-module project
        val entryName: String,
    )

    @Serializable
    class GotoEntryByIndex(
        val parentFolderName: String, // "" for single-module project
        val entryIndex: Int,
    )

    @Serializable
    class NewProjectArgs(
        val labelerName: String,
        val sampleDirectory: String? = null, // absolute path, defaults to project location
        val cacheDirectory: String? = null, // absolute path
        val labelerParams: Map<String, String>? = null,
        val pluginName: String? = null,
        val pluginParams: Map<String, String>? = null,
        val inputFile: String? = null, // absolute path
        val encoding: String = Charset.defaultCharset().name(), // defaults to UTF-8
        val autoExport: Boolean = false,
    )

    @Transient
    override val type: IpcMessageType = IpcMessageType.OpenOrCreate
}
