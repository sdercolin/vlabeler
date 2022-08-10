package com.sdercolin.vlabeler.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private typealias JobFactory = (CoroutineScope) -> Job

class JobQueue(
    private val coroutineScope: CoroutineScope,
    private val size: Int
) {

    private val list = mutableListOf<JobFactory>()
    private var currentJob: Job? = null

    fun post(factory: JobFactory) {
        list.add(factory)
        if (list.size > size) {
            list.removeAt(0)
        }
        if (currentJob == null) {
            currentJob = coroutineScope.launch {
                while (list.isNotEmpty()) {
                    list.removeAt(0)(this).join()
                }
                currentJob = null
            }
        }
    }
}
