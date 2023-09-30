package com.sdercolin.vlabeler.model

import java.io.File

/**
 * Arguments for the program.
 *
 * @property file The file to open or create. If the file is a project file, open it or create a new project with the
 *     same name. If the file is a directory, create a new project in the directory with the same name.
 */
data class Arguments(
    val file: File? = null,
)

fun parseArgs(args: List<String>): Arguments {
    val file = args.firstOrNull()?.let { File(it) }
    return Arguments(file)
}
