package com.sdercolin.vlabeler.ui.dialog.syncsample

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.exception.MissingSampleDirectoryException
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.repository.SampleInfoRepository
import com.sdercolin.vlabeler.ui.ProjectStore
import com.sdercolin.vlabeler.util.asNormalizedFileName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class EntrySampleSyncer(
    private val scope: CoroutineScope,
    private val projectStore: ProjectStore,
    private val onError: (Throwable) -> Unit,
    private val onProgress: (Progress) -> Unit,
) {

    data class Progress(
        val finishedModules: Int,
        val totalModules: Int,
        val finishedFiles: Int,
        val totalFiles: Int,
    ) {
        val moduleProgress = if (totalModules == 0) 0f else finishedModules.toFloat() / totalModules
        val fileProgress = if (totalFiles == 0) 0f else finishedFiles.toFloat() / totalFiles
        val finishedInModule = finishedFiles == totalFiles
        val finished = finishedModules == totalModules
    }

    fun sync(
        project: Project,
        appConf: AppConf,
    ) = scope.launch(Dispatchers.IO) {
        val totalModules = project.modules.size
        var progress = Progress(0, totalModules, 0, 0)
        for (moduleIndex in project.modules.indices) {
            val sampleDirectory = project.modules[moduleIndex].getSampleDirectory(project)
            if (!sampleDirectory.exists()) {
                onError(MissingSampleDirectoryException())
                return@launch
            }
            progress = progress.copy(finishedModules = moduleIndex)
            val module = project.modules[moduleIndex]
            val allSamples = module.entries.filter { it.needSyncCompatibly }
                .map { it.sample }
                .distinctBy { it.asNormalizedFileName() }
                .map { module.getSampleFile(project, it) }
            val totalFiles = allSamples.size
            progress = progress.copy(totalFiles = totalFiles)
            for (sampleIndex in allSamples.indices) {
                val sample = allSamples[sampleIndex]

                progress = progress.copy(finishedFiles = sampleIndex)
                onProgress(progress)

                val sampleInfo = SampleInfoRepository.load(project, sample, module.name, appConf).getOrElse {
                    Log.error(it)
                    onError(it)
                    return@launch
                }
                projectStore.updateProjectOnLoadedSample(sampleInfo, module.name)
            }
            progress = progress.copy(finishedFiles = totalFiles)
            onProgress(progress)
        }
        progress = progress.copy(finishedModules = totalModules)
        onProgress(progress)
    }
}
