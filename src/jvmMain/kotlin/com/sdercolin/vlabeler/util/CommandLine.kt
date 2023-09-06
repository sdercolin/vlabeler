@file:OptIn(DelicateCoroutinesApi::class)

package com.sdercolin.vlabeler.util

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isWindows
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Command line utility, especially to be called from a JavaScript engine during plugin execution.
 * See resources/js/command_line.js for usages.
 */
object CommandLine {

    @JvmStatic
    fun execute(args: List<String>): Int {
        val command = if (isWindows) {
            listOf(args.joinToString(" ") { "\"$it\"" })
        } else {
            args
        }

        Log.info("Execute command: ${args.joinToString(" ") { "\"$it\"" }}")
        val process = ProcessBuilder(command)
            .redirectErrorStream(true)
            .start()
        val reader = process.inputStream.bufferedReader()
        val job = GlobalScope.launch(Dispatchers.IO) {
            var line = reader.readLine()
            while (line != null) {
                Log.info(line)
                line = reader.readLine()
            }
        }
        val exitValue = process.waitFor()
        Log.info("Exit value: $exitValue")
        job.cancel()
        return exitValue
    }
}
