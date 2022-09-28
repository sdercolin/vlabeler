package com.sdercolin.vlabeler.util

import androidx.compose.ui.res.useResource
import java.io.File

object Resources {
    val iconIco get() = "icon.ico"
    val iconPng get() = "icon.png"
    val licensesJson get() = "licenses.json"
    val classEditedEntryJs get() = "js/class_edited_entry.js"
    val classEntryJs get() = "js/class_entry.js"
    val expectedErrorJs get() = "js/expected_error.js"
    val fileJs get() = "js/file.js"
    val moduleDefinitionJs get() = "js/module_definition.js"
    val prepareBuildProjectJs get() = "js/prepare_build_project.js"
    val reportJs get() = "js/report.js"
    val transparencyGridPng get() = "img/transparency_grid.png"
}

fun JavaScript.execResource(path: String) {
    val code = useResource(path) { it.bufferedReader().readText() }
    File("", "")
    exec(path, code)
}
