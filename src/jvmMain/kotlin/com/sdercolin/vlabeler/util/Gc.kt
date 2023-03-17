@file:OptIn(DelicateCoroutinesApi::class)

package com.sdercolin.vlabeler.util

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Force garbage collection with a delay.
 */
fun launchGcDelayed() {
    GlobalScope.launch(Dispatchers.Default) {
        delay(1000)
        System.gc()
    }
}
