package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.model.EmbeddedScripts
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.util.CustomLabelerDir
import com.sdercolin.vlabeler.util.DefaultLabelerDir
import com.sdercolin.vlabeler.util.getChildren
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FilenameFilter

private val labelerFileFilter = FilenameFilter { _, name -> name.endsWith(LabelerConf.LABELER_FILE_EXTENSION) }
private fun File.getLabelers(): List<File> {
    val singleFileLabelers = getChildren(labelerFileFilter)
    val directoryLabelers = getChildren().filter { it.isDirectory }
        .flatMap { it.getChildren(labelerFileFilter) }
    return singleFileLabelers + directoryLabelers
}

fun getDefaultLabelers() = DefaultLabelerDir.getLabelers()
fun getCustomLabelers() = CustomLabelerDir.getLabelers()

suspend fun loadAvailableLabelerConfs(): List<LabelerConf> = withContext(Dispatchers.IO) {
    val defaultLabelers = getDefaultLabelers()
        .mapNotNull {
            it.asLabelerConf(isBuiltIn = true).getOrElse { t ->
                if (isDebug) {
                    throw t
                } else {
                    null
                }
            }
        }
        .groupBy { it.name }
        .mapNotNull { (_, labelers) -> labelers.maxByOrNull { it.version } }
    val defaultLabelerNames = defaultLabelers.map { it.name }
    val customLabelers = getCustomLabelers().mapNotNull {
        it.asLabelerConf(isBuiltIn = false).getOrNull()
    }.toList()
    val validCustomLabelers = customLabelers.filterNot { it.name in defaultLabelerNames }
        .groupBy { it.name }
        .mapNotNull { (_, labelers) -> labelers.maxByOrNull { it.version } }

    val availableLabelers = defaultLabelers + validCustomLabelers
    availableLabelers.forEach {
        Log.info("Loaded labeler: ${it.name}")
    }
    availableLabelers.sortedBy { it.name }
}

fun File.asLabelerConf(isBuiltIn: Boolean): Result<LabelerConf> {
    val text = readText()
    val result = runCatching {
        text.parseJson<LabelerConf>()
            .copy(builtIn = isBuiltIn)
            .run { if (singleFile) this else copy(directory = parentFile) }
            .preloadScripts()
            .validate()
            .migrate()
    }
    result.exceptionOrNull()?.let {
        Log.debug("Failed to parse labeler conf: $text. Error message: {${it.message}}.")
    }
    return result
}

private fun EmbeddedScripts.writeTo(location: File, fallbackPath: String): EmbeddedScripts {
    require(lines.isNullOrEmpty().not())
    val file = location.resolve(path ?: fallbackPath)
    file.writeText(lines.orEmpty().joinToString("\n"))
    return copy(path = file.relativeTo(location).path, lines = null, writeAsPath = true)
}

fun LabelerConf.install(location: File): Result<File> = runCatching {
    if (singleFile) {
        val text = stringifyJson()
        val fileName = "$name.${LabelerConf.LABELER_FILE_EXTENSION}"
        val file = File(location, fileName)
        file.writeText(text)
        Log.debug("Installed labeler $name (version $version) to ${file.absolutePath}")
        file
    } else {
        val folderName = name.removeSuffix(".default") + "-labeler"
        val folder = File(location, folderName)
        folder.mkdirs()
        if (folder.isDirectory.not()) {
            throw IllegalStateException("Failed to create labeler folder: ${folder.absolutePath}")
        }
        val properties = this.properties.map {
            it.copy(
                valueGetter = it.valueGetter.writeTo(folder, "${it.name}-getter.js"),
                valueSetter = it.valueSetter?.writeTo(folder, "${it.name}-setter.js"),
            )
        }
        val parser = this.parser.copy(scripts = this.parser.scripts.writeTo(folder, "parser.js"))
        val writer = this.writer.copy(scripts = this.writer.scripts?.writeTo(folder, "writer.js"))
        val parameters = this.parameters.map { holder ->
            holder.copy(
                injector = holder.injector?.writeTo(folder, "${holder.parameter.name}-injector.js"),
            )
        }
        val projectConstructor = this.projectConstructor?.let {
            it.copy(scripts = it.scripts.writeTo(folder, "project-constructor.js"))
        }
        val quickProjectBuilders = this.quickProjectBuilders.map { def ->
            def.copy(scripts = def.scripts.writeTo(folder, "${def.name}-quick-project-builder.js"))
        }
        val text = copy(
            properties = properties,
            parser = parser,
            writer = writer,
            parameters = parameters,
            projectConstructor = projectConstructor,
            quickProjectBuilders = quickProjectBuilders,
        ).stringifyJson()
        val file = File(folder, LabelerConf.LABELER_FILE_EXTENSION)
        file.writeText(text)
        Log.debug("Installed labeler $name (version $version) to ${folder.absolutePath}")
        file
    }
}
