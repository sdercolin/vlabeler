package com.sdercolin.vlabeler.ui.dialog.sample

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.io.Sample
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import com.sdercolin.vlabeler.util.toFile
import java.awt.Desktop
import java.io.File

class SampleListDialogState(
    private val editorState: EditorState,
) {
    var selectedSampleName: String? by mutableStateOf(null)
        private set

    var selectedEntryIndex: Int? by mutableStateOf(null)
        private set

    var includedSampleItems: List<SampleListDialogItem.IncludedSample> by mutableStateOf(listOf())
        private set

    var excludedSampleItems: List<SampleListDialogItem.ExcludedSample> by mutableStateOf(listOf())
        private set

    var entryItems: List<SampleListDialogItem.Entry> by mutableStateOf(listOf())
        private set

    init {
        fetch()
    }

    private fun getExistingSampleFiles() = editorState.project.sampleFileNameMap.map { it.toPair() }
        .plus(
            Sample.listSampleFiles(editorState.project.sampleDirectory.toFile())
                .map { it.nameWithoutExtension to it.name },
        )
        .distinctBy { it.first }

    private fun getProjectSampleFilesWithEntries() = editorState.project.entries
        .groupBy { it.sample }
        .map { (sample, entries) ->
            (sample to editorState.project.getSampleFile(sample).name) to entries
        }
        .toMap()

    private fun fetch() {
        val existing = getExistingSampleFiles()
        val projectSamplesWithEntries = getProjectSampleFilesWithEntries()
        val projectSamples = projectSamplesWithEntries.map { it.key }
        includedSampleItems = projectSamplesWithEntries.map {
            SampleListDialogItem.IncludedSample(
                name = it.key.first,
                fileName = it.key.second,
                valid = existing.any { (name, _) -> name == it.key.first },
                entryCount = it.value.size,
            )
        }
        excludedSampleItems = (existing - projectSamples.toSet())
            .map { SampleListDialogItem.ExcludedSample(it.first, it.second) }
        selectedSampleName?.let { selectSample(it) }
    }

    fun selectSample(name: String) {
        selectedSampleName = name
        val entries = editorState.project.entries
        entryItems = entries.indices.filter { entries[it].sample == name }.map {
            val indexedEntry = IndexedEntry(entries[it], it)
            SampleListDialogItem.Entry(name = indexedEntry.name, entry = indexedEntry)
        }
        if (selectedEntryIndex !in entryItems.map { it.entry.index }) {
            selectedEntryIndex = null
        }
    }

    fun selectEntry(index: Int) {
        selectedEntryIndex = index
    }

    fun createDefaultEntry() {
        val sampleName = requireNotNull(selectedSampleName)
        editorState.createDefaultEntry(sampleName)
        fetch()
    }

    fun jumpToSelectedEntry() {
        val index = requireNotNull(selectedEntryIndex)
        editorState.jumpToEntry(index)
    }

    val sampleDirectory: File get() = editorState.project.sampleDirectory.toFile()

    fun isSampleDirectoryExisting() = sampleDirectory.let {
        it.exists() && it.isDirectory
    }

    fun openSampleDirectory() {
        Desktop.getDesktop().open(sampleDirectory)
    }

    fun requestRedirectSampleDirectory() {
        editorState.requestRedirectSampleDirectory()
    }
}
