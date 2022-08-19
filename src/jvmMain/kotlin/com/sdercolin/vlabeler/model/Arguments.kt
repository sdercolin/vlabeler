package com.sdercolin.vlabeler.model

typealias ArgumentMap = Map<String, String>

object Arguments {
    const val OpenOrCreate = "open-or-create"
    const val Open = "open"
    const val SampleDirectory = "sample-dir"
    const val CacheDirectory = "cache-dir"
    const val InputFile = "input"
    const val LabelerName = "labeler"
    const val Encoding = "encoding"
    const val AutoExport = "auto-export"
}

fun parseArguments(args: List<String>): ArgumentMap? {
    val map = mutableMapOf<String, String>()
    for (i in args.indices) {
        if (args[i].startsWith("--")) {
            val arg = args[i].removePrefix("--")
            val value = if (i + 1 < args.size && !args[i + 1].startsWith("--")) args[i + 1] else ""
            map[arg] = value.trim('"')
        }
    }
    return map.ifEmpty { null }
}
