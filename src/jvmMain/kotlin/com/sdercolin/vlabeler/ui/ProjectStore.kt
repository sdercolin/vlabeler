package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.ProjectHistory
import com.sdercolin.vlabeler.ui.editor.EditedEntry
import com.sdercolin.vlabeler.ui.editor.ScrollFitViewModel

interface ProjectStore {
    val project: Project?
    val hasProject: Boolean
    fun requireProject(): Project
    val history: ProjectHistory
    fun newProject(newProject: Project)
    fun clearProject()
    fun editProject(editor: Project.() -> Project)
    fun editEntry(editedEntry: EditedEntry)
    val canUndo: Boolean
    fun undo()
    val canRedo: Boolean
    fun redo()
    val canGoNextEntryOrSample: Boolean
    val canGoPreviousEntryOrSample: Boolean
    fun nextEntry()
    fun previousEntry()
    fun nextSample()
    fun previousSample()
    fun jumpToEntry(sampleName: String, entryIndex: Int)
    fun renameEntry(sampleName: String, index: Int, newName: String)
    fun duplicateEntry(sampleName: String, index: Int, newName: String)
    val canRemoveCurrentEntry: Boolean
    fun removeCurrentEntry()
}

class ProjectStoreImpl(
    private val screenState: AppScreenState,
    private val scrollFitViewModel: ScrollFitViewModel
) : ProjectStore {

    private val projectState: MutableState<Project?> = mutableStateOf(null)

    override var project: Project?
        get() = projectState.value
        set(value) {
            if (value != null) screenState.editor?.updateProject(value)
            projectState.value = value
        }

    override val hasProject get() = project != null
    override fun requireProject(): Project = requireNotNull(project)

    override var history: ProjectHistory by mutableStateOf(ProjectHistory())
        private set

    override fun newProject(newProject: Project) {
        project = newProject
        history = ProjectHistory.new(newProject)
    }

    override fun clearProject() {
        project = null
        history = ProjectHistory()
    }

    override fun editProject(editor: Project.() -> Project) {
        val edited = requireProject().editor()
        project = edited
        history = history.push(edited)
    }

    private fun editNonNullProject(editor: Project.() -> Project?) {
        val edited = requireProject().editor() ?: return
        project = edited
        history = history.push(edited)
    }

    override fun editEntry(editedEntry: EditedEntry) = editProject { updateEntry(editedEntry) }

    override val canUndo get() = history.canUndo
    override fun undo() {
        history = history.undo()
        project = history.current
    }

    override val canRedo get() = history.canRedo
    override fun redo() {
        history = history.redo()
        project = history.current
    }

    override val canGoNextEntryOrSample get() = project?.run { currentEntryIndexInTotal < totalEntryCount - 1 } == true
    override val canGoPreviousEntryOrSample get() = project?.run { currentEntryIndexInTotal > 0 } == true

    override fun nextEntry() {
        val previousProject = project
        editNonNullProject { nextEntry() }
        if (requireProject().hasSwitchedSample(previousProject)) scrollFitViewModel.emitNext()
    }

    override fun previousEntry() {
        val previous = project
        editNonNullProject { previousEntry() }
        if (requireProject().hasSwitchedSample(previous)) scrollFitViewModel.emitNext()
    }

    override fun nextSample() {
        editNonNullProject { nextSample() }
        scrollFitViewModel.emitNext()
    }

    override fun previousSample() {
        editNonNullProject { previousSample() }
        scrollFitViewModel.emitNext()
    }

    override fun jumpToEntry(sampleName: String, entryIndex: Int) {
        editProject {
            requireProject().copy(
                currentSampleName = sampleName,
                currentEntryIndex = entryIndex
            )
        }
        scrollFitViewModel.emitNext()
    }

    override fun renameEntry(sampleName: String, index: Int, newName: String) = editProject {
        renameEntry(sampleName, index, newName)
    }

    override fun duplicateEntry(sampleName: String, index: Int, newName: String) = editProject {
        duplicateEntry(sampleName, index, newName).copy(currentEntryIndex = index + 1)
    }

    override val canRemoveCurrentEntry
        get() = project?.let {
            it.entriesBySampleName.getValue(it.currentSampleName).size > 1
        } == true

    override fun removeCurrentEntry() = editProject { removeCurrentEntry() }
}
