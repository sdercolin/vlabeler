package com.sdercolin.vlabeler.util

import com.sdercolin.vlabeler.env.Log
import kotlinx.coroutines.delay

/**
 * A debug utility to monitor memory usage.
 */
class MemoryUsageMonitor {

    private fun convertBytesToMegaBytes(bytes: Long): Double {
        return bytes.toDouble() / 1024 / 1024
    }

    suspend fun run() {
        while (true) {
            delay(500)
            val totalMemory = convertBytesToMegaBytes(Runtime.getRuntime().totalMemory())
            val freeMemory = convertBytesToMegaBytes(Runtime.getRuntime().freeMemory())
            val usedMemory = totalMemory - freeMemory
            Log.info("used memory: ${usedMemory.roundToDecimalDigit(3)} MB")
        }
    }
}
