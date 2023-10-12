package com.sdercolin.vlabeler.repository

import androidx.compose.ui.res.useResource
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.appVersion
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.model.palette.ColorPaletteDefinition
import com.sdercolin.vlabeler.util.AppDir
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.getChildren
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.stringifyJson

/**
 * A repository for color palette definitions.
 */
object ColorPaletteRepository {

    private const val FOLDER_NAME = "color_palettes"
    private const val README_FILE_NAME = "readme.txt"
    val directory = AppDir.resolve(FOLDER_NAME)

    private val items = mutableMapOf<String, ColorPaletteDefinition>()

    fun initialize(appRecord: AppRecord) {
        if (directory.isFile) {
            directory.delete()
        }
        directory.mkdirs()
        if (directory.isDirectory.not()) return

        val isNewVersion = appRecord.appVersionLastLaunched < appVersion

        ColorPaletteDefinition.presets.forEach { definition ->
            val file = directory.resolve("${definition.name}.example.json")
            if (file.exists().not() || isNewVersion) {
                file.writeText(definition.stringifyJson())
            }
        }

        val readmeFile = directory.resolve(README_FILE_NAME)
        if (readmeFile.exists().not() || isNewVersion) {
            useResource(Resources.colorPaletteReadme) {
                val readme = it.bufferedReader().readText()
                readmeFile.writeText(readme)
            }
        }

        load()
    }

    fun load() {
        if (directory.isDirectory.not()) return
        val namesInThisLoad = mutableListOf<String>()
        ColorPaletteDefinition.presets.forEach {
            items[it.name] = it
            namesInThisLoad.add(it.name)
        }
        directory.getChildren().forEach { file ->
            if (file.name.endsWith(".json") && file.name.endsWith(".example.json").not()) {
                runCatching { file.readText().parseJson<ColorPaletteDefinition>().validate() }
                    .onSuccess { definition ->
                        if (definition.name in namesInThisLoad) {
                            Log.error(
                                "Cannot read {${file.absolutePath}} " +
                                    "because color palette ${definition.name} already exists.",
                            )
                        } else {
                            namesInThisLoad.add(definition.name)
                            items[definition.name] = definition
                        }
                    }
                    .onFailure {
                        Log.error(it)
                        Log.error("Cannot read color palette file {${file.absolutePath}}.")
                    }
            }
        }
    }

    fun getAll() = items.values.toList()

    fun has(name: String) = items.containsKey(name)

    fun get(name: String): ColorPaletteDefinition {
        val found = items[name]
        if (found == null) {
            Log.error("Cannot find color palette $name.")
            return ColorPaletteDefinition.presets.first()
        }
        return found
    }
}
