package com.sdercolin.vlabeler.util

import com.sdercolin.vlabeler.env.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY

class FileChangeDetection(
    private val directory: File,
    private val filter: (File) -> Boolean,
    private val callback: Callback,
) {
    private var job: Job? = null
    private var callbackJob: Job? = null

    fun startIn(coroutineScope: CoroutineScope) {
        if (job?.isActive == true) {
            Log.info("File change detection on $directory is already running, skipping")
            return
        }
        job = coroutineScope.launch(Dispatchers.IO) {
            val watchService = FileSystems.getDefault().newWatchService()
            val directoryPath = Paths.get(directory.absolutePath)
            directoryPath.register(watchService, ENTRY_CREATE, ENTRY_MODIFY)
            while (isActive) {
                val key = watchService.take()
                yield()
                val new = mutableListOf<File>()
                val changed = mutableListOf<File>()
                key.pollEvents().forEach { event ->
                    val kind = event.kind()
                    val path = event.context() as Path
                    val file = directory.resolve(path.toString())
                    if (filter(file).not()) {
                        return@forEach
                    }
                    when (kind) {
                        ENTRY_CREATE -> new.add(file)
                        ENTRY_MODIFY -> changed.add(file)
                    }
                    Log.debug("File change detected: new=$new, changed=$changed")
                    if (callbackJob?.isActive == true) {
                        Log.debug("File change detection callback is already running, skipping")
                    } else {
                        callbackJob = coroutineScope.launch {
                            callback.onFileChanged(changed, new)
                        }
                    }
                }
                val valid = key.reset()
                if (!valid) {
                    break
                }
            }
        }
    }

    fun dispose() {
        callbackJob?.cancel()
        callbackJob = null
        job?.cancel()
        job = null
    }

    fun interface Callback {
        suspend fun onFileChanged(changed: List<File>, new: List<File>)
    }
}
