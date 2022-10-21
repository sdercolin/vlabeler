package com.sdercolin.vlabeler.util

object CommandLine {

    @JvmStatic
    fun execute(args: Array<String>): String {
        val process = ProcessBuilder(*args)
            .redirectErrorStream(true)
            .start()
        process.waitFor()
        return process.inputStream.bufferedReader().readText()
    }
}
