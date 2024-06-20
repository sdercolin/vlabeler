package com.sdercolin.vlabeler.io

import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.EntryListDiff
import com.sdercolin.vlabeler.model.EntryListDiffItem
import com.sdercolin.vlabeler.model.EntryNotes
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.computeEntryListDiff
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.dialog.ReloadLabelConfigs
import com.sdercolin.vlabeler.ui.dialog.ReloadLabelDialogArgs
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
        val result = withContext(Dispatchers.IO) { reloadEntriesFromLabelFile(project, labelFile) }
            .getOrElse {
                showError(it, null)
                hideProgress()
                return@launch
            }
        hideProgress()
        if (skipConfirmation) {
            editProject { applyReloadedEntries(result.first, result.second) }
        } else {
            openReloadLabelDialog(ReloadLabelDialogArgs(project.currentModule.name, result.first, result.second))
        }
    }
}

private fun AppState.reloadEntriesFromLabelFile(
    project: Project,
    file: File,
): Result<Pair<List<Entry>, EntryListDiff>> = runCatching {
    val sampleFiles = project.currentModule.entries.map { it.sample }.distinct().sorted()
        .map { project.currentModule.getSampleFile(project, it) }

    val entries = moduleFromRawLabels(
        sources = file.readTextByEncoding(project.encoding).lines(),
        inputFile = file,
        labelerConf = project.labelerConf,
        labelerParams = null,
        labelerTypedParams = project.labelerParams,
        sampleFiles = sampleFiles,
        encoding = project.encoding,
    )
    val diff = computeEntryListDiff(
        list1 = project.currentModule.entries,
        list2 = entries,
        weights = project.labelerConf.entrySimilarityWeights,
    )
    entries to diff
}

fun mergeEntryLists(
    newList: List<Entry>,
    oldList: List<Entry>,
    diff: EntryListDiff,
    configs: ReloadLabelConfigs,
): List<Entry> {
    if (configs.inheritTag.not() && configs.inheritDone.not() && configs.inheritStar.not()) {
        return newList
    }
    val merged = newList.toMutableList()
    val indexPairs = diff.items.filterIsInstance<EntryListDiffItem.Edit>().map { it.oldIndex to it.newIndex } +
        diff.items.filterIsInstance<EntryListDiffItem.Unchanged>().map { it.oldIndex to it.newIndex }
    indexPairs.forEach { (oldIndex, newIndex) ->
        val entry = merged[newIndex]
        val oldEntry = oldList[oldIndex]
        val newTag = if (configs.inheritTag) {
            oldEntry.notes.tag
        } else {
            entry.notes.tag
        }
        val newStar = if (configs.inheritStar) {
            oldEntry.notes.star
        } else {
            entry.notes.star
        }
        val newDone = if (configs.inheritDone) {
            oldEntry.notes.done
        } else {
            entry.notes.done
        }
        val newNotes = EntryNotes(done = newDone, star = newStar, tag = newTag)
        merged[newIndex] = entry.copy(notes = newNotes)
    }
    return merged
}
