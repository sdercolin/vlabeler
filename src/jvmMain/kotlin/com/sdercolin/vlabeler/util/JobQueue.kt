package com.sdercolin.vlabeler.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private typealias JobFactory = (CoroutineScope) -> Job

/**
 * A queue of suspendable jobs to ensure they run sequentially.
 *
 * @param coroutineScope The scope of the jobs.
 * @param size The maximum size of the queue. If during the execution of a job, the queue is full, the oldest job except
 *     the current one will be removed.
 */
class JobQueue(
    private val coroutineScope: CoroutineScope,
    private val size: Int,
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
