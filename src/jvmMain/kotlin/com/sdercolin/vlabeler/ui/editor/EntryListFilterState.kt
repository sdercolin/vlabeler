package com.sdercolin.vlabeler.ui.editor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.filter.EntryFilter

open class EntryListFilterState {

    var filter: EntryFilter by mutableStateOf(EntryFilter())
        protected set

    open fun editFilter(editor: EntryFilter.() -> EntryFilter) {
        filter = editor.invoke(filter)
    }

    open fun clear() {
        filter = EntryFilter()
    }
}

class LinkableEntryListFilterState(project: Project, private val submitFilter: (EntryFilter?) -> Unit) :
    EntryListFilterState() {

    private var projectFiler: EntryFilter? = project.entryFilter

    var linked: Boolean by mutableStateOf(project.entryFilter != null)

    init {
        filter = projectFiler ?: EntryFilter()
    }

    override fun editFilter(editor: EntryFilter.() -> EntryFilter) {
        super.editFilter(editor)
        if (linked) {
            projectFiler = filter
            submitFilter(projectFiler)
        }
    }

    fun updateProject(project: Project) {
        projectFiler = project.entryFilter
        linked = projectFiler != null
        if (project.entryFilter != null) {
            filter = project.entryFilter
        }
    }

    fun toggleLinked() {
        if (linked) {
            unlink()
        } else {
            link()
        }
    }

    private fun link() {
        linked = true
        projectFiler = filter
        submitFilter(projectFiler)
    }

    private fun unlink() {
        linked = false
        projectFiler = null
        submitFilter(null)
    }

    override fun clear() {
        super.clear()
        unlink()
    }
}
