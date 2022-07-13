@file:OptIn(DelicateCoroutinesApi::class)

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun launchGcDelayed() {
    GlobalScope.launch(Dispatchers.Default) {
        delay(1000)
        System.gc()
    }
}
