package com.sdercolin.vlabeler.util

import com.sdercolin.vlabeler.env.isWindows

object CommandLine {

    @JvmStatic
    fun execute(args: List<String>): String {
        val command = if (isWindows) {
            listOf(args.joinToString(" ") { "\"$it\"" })
        } else {
            args
        }
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        process.waitFor()
        return process.inputStream.bufferedReader().readText()
    }
}
