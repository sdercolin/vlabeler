package com.sdercolin.vlabeler.util

import androidx.compose.ui.res.useResource
import java.io.File

/**
 * A collection of resource paths used in the project.
 */
object Resources {
    val iconIco get() = "icon.ico"
    val iconPng get() = "icon.png"
    val licensesJson get() = "licenses.json"
    val classEntryJs get() = "js/class_entry.js"
    val classModuleJs get() = "js/class_module.js"
    val expectedErrorJs get() = "js/expected_error.js"
    val envJs get() = "js/env.js"
    val fileJs get() = "js/file.js"
    val moduleDefinitionJs get() = "js/module_definition.js"
    val commandLineJs get() = "js/command_line.js"
    val requestAudioPlaybackJs get() = "js/request_audio_playback.js"
    val reportJs get() = "js/report.js"
    val transparencyGridPng get() = "img/transparency_grid.png"
    val colorPaletteReadme get() = "readme/color_palette.md"
    val customFontReadme get() = "readme/custom_font.md"
}

/**
 * Execute a JavaScript code in a file.
 *
 * @param path the path of the file.
 */
fun JavaScript.execResource(path: String) {
    val code = useResource(path) { it.bufferedReader().readText() }
    File("", "")
    exec(path, code)
}
