package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.util.readTextByEncoding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Reloads the label file and updates the project with the new entries.
 *
 * @param file The new label file. If null, use the defined raw label file in the module.
 * @param skipConfirmation If true, skip the confirmation dialog and apply the new entries directly.
 */
fun AppState.reloadLabelFile(file: File?, skipConfirmation: Boolean) {
    val project = project ?: return
    val labelFile = file ?: project.currentModule.getRawFile(project) ?: return
    mainScope.launch {
        showProgress()
        val result = withContext(Dispatchers.IO) { project.reloadEntriesFromLabelFile(labelFile) }
            .getOrElse {
                showError(it, null)
                hideProgress()
                return@launch
            }
        // TODO: use skipConfirmation
        if (skipConfirmation) {
            editProject { applyReloadedEntries(result) }
        }
        hideProgress()
    }
}

private fun Project.reloadEntriesFromLabelFile(file: File): Result<List<Entry>> = runCatching {
    val sampleFiles = currentModule.entries.map { it.sample }.distinct().sorted()
        .map { currentModule.getSampleFile(this, it) }

    val entries = moduleFromRawLabels(
        sources = file.readTextByEncoding(encoding).lines(),
        inputFile = file,
        labelerConf = labelerConf,
        labelerParams = null,
        labelerTypedParams = labelerParams,
        sampleFiles = sampleFiles,
        encoding = encoding,
    )
    // TODO: match entries and copy notes
    entries
}

private fun Project.applyReloadedEntries(entries: List<Entry>): Project {
    val newModule = currentModule.copy(entries = entries, currentIndex = 0, entryFilter = null)
    return copy(modules = modules.map { if (it == currentModule) newModule else it })
}
