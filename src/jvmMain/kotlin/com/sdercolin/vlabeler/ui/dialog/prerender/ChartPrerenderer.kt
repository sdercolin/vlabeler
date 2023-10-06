package com.sdercolin.vlabeler.ui.dialog.prerender

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.MissingSampleDirectoryException
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.repository.SampleInfoRepository
import com.sdercolin.vlabeler.ui.editor.ChartStore
import com.sdercolin.vlabeler.util.asNormalizedFileName
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
) {

    private val renderProgressMutex = Mutex()

    data class Progress(
        val finishedModules: Int,
        val totalModules: Int,
        val finishedFiles: Int,
        val totalFiles: Int,
        val finishedCharts: Int,
        val totalCharts: Int,
    ) {
        val moduleProgress = if (totalModules == 0) 0f else finishedModules.toFloat() / totalModules
        val fileProgress = if (totalFiles == 0) 0f else finishedFiles.toFloat() / totalFiles
        val chartProgress = if (totalCharts == 0) 0f else finishedCharts.toFloat() / totalCharts
        val finishedInFile = finishedCharts == totalCharts
        val finishedInModule = finishedFiles == totalFiles && finishedCharts == totalCharts
        val finished = finishedModules == totalModules
    }

    fun render(
        project: Project,
        appConf: AppConf,
        density: Density,
        layoutDirection: LayoutDirection,
    ) = scope.launch(Dispatchers.IO) {
        val totalModules = project.modules.size
        var progress = Progress(0, totalModules, 0, 0, 0, 0)
        for (moduleIndex in project.modules.indices) {
            val sampleDirectory = project.modules[moduleIndex].getSampleDirectory(project)
            if (!sampleDirectory.exists()) {
                onError(MissingSampleDirectoryException())
                return@launch
            }
            progress = progress.copy(finishedModules = moduleIndex)
            val module = project.modules[moduleIndex]
            val allSamples = module.entries.map { it.sample }.distinctBy { it.asNormalizedFileName() }
                .map { module.getSampleFile(project, it) }
            val totalFiles = allSamples.size
            progress = progress.copy(totalFiles = totalFiles)
            for (sampleIndex in allSamples.indices) {
                val sample = allSamples[sampleIndex]

                progress = progress.copy(finishedFiles = sampleIndex)
                val sampleInfo = SampleInfoRepository.load(project, sample, module.name, appConf).getOrElse {
                    Log.error(it)
                    onError(it)
                    return@launch
                }

                val totalCharts = sampleInfo.totalChartCount
                progress = progress.copy(totalCharts = totalCharts)

                if (chartStore.hasCachedSample(sampleInfo)) {
                    progress = progress.copy(finishedCharts = totalCharts)
                    onProgress(progress)
                    continue
                }

                chartStore.prepareForNewLoading(project, appConf, sampleInfo)
                var finishedCharts = 0
                progress = progress.copy(finishedCharts = finishedCharts)
                onProgress(progress)

                val onRenderProgress = suspend {
                    renderProgressMutex.withLock {
                        finishedCharts++
                        progress = progress.copy(finishedCharts = finishedCharts)
                        onProgress(progress)
                    }
                }

                chartStore.load(
                    scope = scope,
                    project = project,
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
            progress = progress.copy(finishedFiles = totalFiles)
            onProgress(progress)
        }
        progress = progress.copy(finishedModules = totalModules)
        onProgress(progress)
    }
}
