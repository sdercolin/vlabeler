package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.useResource
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.util.Python
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.python.core.PyObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset

@Serializable
@Immutable
data class Plugin(
    val name: String,
    val version: Int = 1,
    val type: Type,
    val displayedName: String = name,
    val author: String,
    val description: String = "",
    val supportedLabelFileExtension: String,
    val inputFileExtension: String? = null,
    val requireInputFile: Boolean = false,
    val allowMultipleInputFiles: Boolean = false,
    val scriptFiles: List<String>,
    @Transient val directory: File? = null
) {

    fun readScriptTexts() = scriptFiles.map { requireNotNull(directory).resolve(it).readText() }

    @Serializable
    enum class Type(val directoryName: String) {
        @SerialName("template")
        Template("template"),

        @SerialName("macro")
        Macro("macro")
    }
}

fun runTemplatePlugin(plugin: Plugin, inputFiles: List<File>, encoding: String): List<FlatEntry> {
    val python = Python()
    val inputTexts = inputFiles.map { it.readText(Charset.forName(encoding)) }
    python.setCurrentWorkingDirectory(requireNotNull(plugin.directory).absolutePath)

    val outputStream = ByteArrayOutputStream()
    python.setOutputStream(outputStream)

    python.set("inputs", inputTexts)

    val entryDefCode = useResource("template_entry.py") { String(it.readAllBytes()) }
    python.exec(entryDefCode)

    plugin.readScriptTexts().forEach {
        python.exec(it)
    }

    val output = python.get<List<PyObject>>("output")
        .map { obj ->
            FlatEntry(
                sample = obj.__getattr__("sample").asStringOrNull(),
                name = obj.__getattr__("name").asString(),
                start = obj.__getattr__("start").asDouble().toFloat(),
                end = obj.__getattr__("end").asDouble().toFloat(),
                points = obj.__getattr__("points").asIterable().map { it.asDouble().toFloat() },
                extra = obj.__getattr__("extras").asIterable().map { it.asString() }
            )
        }
    Log.info("Plugin execution got entries:\n" + output.joinToString("\n"))
    val printed = outputStream.toByteArray().decodeToString()
    if (printed.isNotBlank()) {
        Log.debug("Plugin execution output:\n$printed")
    }
    outputStream.close()
    python.close()
    return output
}
