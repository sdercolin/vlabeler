package com.sdercolin.vlabeler.ui.dialog.sample

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.io.Sample
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.editor.IndexedEntry
import com.sdercolin.vlabeler.util.asNormalizedFileName
import com.sdercolin.vlabeler.util.equalsAsFileName
import com.sdercolin.vlabeler.util.getDirectory
import java.awt.Desktop
import java.io.File

class SampleListDialogState(
    private val editorState: EditorState,
) {
    val allModuleNames get() = editorState.project.modules.map { it.name }
    var currentModuleName: String by mutableStateOf(editorState.project.currentModule.name)
        private set

    private val currentModule get() = editorState.project.modules.first { it.name == currentModuleName }

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

    var isShowingSampleDirectoryRedirectDialog: Boolean by mutableStateOf(false)
        private set

    init {
        fetch()
    }

    fun selectModule(name: String) {
        currentModuleName = name
        fetch()
        cleanSelections()
    }

    private fun getExistingSampleFileNames() =
        Sample.listSampleFiles(
            currentModule.getSampleDirectory(editorState.project),
        )
            .map { it.name }

    private fun getProjectSampleFilesWithEntries(existingSampleFileNames: List<String>) = currentModule.entries
        .groupBy { it.sample.asNormalizedFileName() }
        .map { (sampleName, entries) ->
            val actualSampleName = existingSampleFileNames.find { it.equalsAsFileName(sampleName) } ?: sampleName
            actualSampleName to entries.map { IndexedEntry(it, currentModule.entries.indexOf(it)) }
        }

    private fun fetch() {
        val existing = getExistingSampleFileNames()
        val projectSamplesWithEntries = getProjectSampleFilesWithEntries(existing)
        val projectSamples = projectSamplesWithEntries.map { it.first }
        includedSampleItems = projectSamplesWithEntries.map {
            SampleListDialogItem.IncludedSample(
                name = it.first,
                valid = it.first in existing,
                entryCount = it.second.size,
            )
        }
        excludedSampleItems = (existing - projectSamples.toSet())
            .map { SampleListDialogItem.ExcludedSample(it) }
        selectedSampleName?.let { selectSample(it) }
    }

    private fun cleanSelections() {
        selectedSampleName = null
        selectedEntryIndex = null
        entryItems = listOf()
    }

    fun selectSample(name: String) {
        val entries = currentModule.entries
        entryItems = entries.indices.filter { entries[it].sample.equalsAsFileName(name) }.map {
            val indexedEntry = IndexedEntry(entries[it], it)
            SampleListDialogItem.Entry(name = indexedEntry.name, entry = indexedEntry)
        }
        selectedSampleName = name
        if (selectedEntryIndex !in entryItems.map { it.entry.index }) {
            selectedEntryIndex = null
        }
    }

    fun selectEntry(index: Int) {
        selectedEntryIndex = index
    }

    fun createDefaultEntry() {
        val sampleName = requireNotNull(selectedSampleName)
        editorState.createDefaultEntry(currentModuleName, sampleName)
        fetch()
    }

    fun createDefaultEntriesForAllExcludedSamples() {
        val excludedSamples = excludedSampleItems.map { it.name }
        editorState.createDefaultEntries(currentModuleName, excludedSamples)
        fetch()
    }

    fun jumpToSelectedEntry() {
        val index = requireNotNull(selectedEntryIndex)
        editorState.jumpToEntry(currentModuleName, index)
    }

    val sampleDirectory: File
        get() = currentModule.getSampleDirectory(editorState.project)

    fun isSampleDirectoryExisting() = sampleDirectory.let {
        it.exists() && it.isDirectory
    }

    fun openSampleDirectory() {
        Desktop.getDesktop().open(sampleDirectory)
    }

    fun requestRedirectSampleDirectory() {
        isShowingSampleDirectoryRedirectDialog = true
    }

    fun getInitialSampleDirectoryForRedirection() = sampleDirectory
        .takeIf { it.isDirectory }?.absolutePath

    fun handleRedirectionDialogResult(parent: String?, name: String?) {
        isShowingSampleDirectoryRedirectDialog = false
        if (parent != null && name != null) {
            val newDirectory = File(parent, name).getDirectory()
            if (newDirectory.exists() && newDirectory.isDirectory) {
                editorState.changeSampleDirectory(currentModuleName, newDirectory)
                fetch()
                cleanSelections()
            }
        }
    }
}
