package com.sdercolin.vlabeler.ui.dialog.prerender

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.MissingSampleDirectoryException
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.repository.SampleInfoRepository
import com.sdercolin.vlabeler.ui.editor.ChartStore
import com.sdercolin.vlabeler.util.toFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ChartPrerenderer(
    private val scope: CoroutineScope,
    private val chartStore: ChartStore,
    private val onError: (Throwable) -> Unit,
    private val onProgress: (Progress) -> Unit,
    private val onComplete: () -> Unit,
) {

    private val renderProgressMutex = Mutex()

    data class Progress(
        val finishedFiles: Int,
        val totalFiles: Int,
        val finishedCharts: Int,
        val totalCharts: Int,
    ) {
        val finishedInFile = finishedCharts == totalCharts
        val finished = finishedFiles == totalFiles && finishedCharts == totalCharts
    }

    fun render(
        project: Project,
        appConf: AppConf,
        density: Density,
        layoutDirection: LayoutDirection,
    ) = scope.launch(Dispatchers.IO) {
        val sampleDirectory = project.sampleDirectory.toFile()
        if (!sampleDirectory.exists()) {
            onError(MissingSampleDirectoryException())
            return@launch
        }

        val allSamples = project.entries.map { it.sample }.distinct().map { project.getSampleFile(it) }
        val totalFiles = allSamples.size
        for (sampleIndex in allSamples.indices) {
            val sample = allSamples[sampleIndex]

            val sampleInfo = SampleInfoRepository.load(sample, appConf).getOrElse {
                Log.error(it)
                onError(it)
                return@launch
            }

            val totalCharts = sampleInfo.chunkCount *
                (sampleInfo.channels + if (sampleInfo.hasSpectrogram) 1 else 0)

            if (chartStore.hasCachedSample(sampleInfo)) {
                onProgress(Progress(sampleIndex, totalFiles, totalCharts, totalCharts))
                continue
            }

            chartStore.prepareForNewLoading(project, appConf, sampleInfo.chunkCount, sampleInfo.channels)
            var finishedCharts = 0
            onProgress(Progress(sampleIndex, totalFiles, finishedCharts, totalCharts))

            val onRenderProgress = suspend {
                renderProgressMutex.withLock {
                    finishedCharts++
                    onProgress(Progress(sampleIndex, totalFiles, finishedCharts, totalCharts))
                }
            }

            chartStore.load(
                scope = scope,
                sampleInfo = sampleInfo,
                appConf = appConf,
                density = density,
                layoutDirection = layoutDirection,
                startingChunkIndex = 0,
                onRenderProgress = onRenderProgress,
            )
            chartStore.awaitLoad()
            chartStore.clear()
        }
        onComplete()
    }
}
