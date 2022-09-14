package com.sdercolin.vlabeler.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class ProjectHistory {

    private val list = mutableStateListOf<Project>()
    private var index by mutableStateOf(-1)

    val current get() = list[index]
    val canUndo get() = index > 0
    val canRedo get() = index >= 0 && index < list.lastIndex

    fun clear() {
        list.clear()
        index = -1
    }

    fun new(project: Project) {
        list.clear()
        list.add(project)
        index = 0
    }

    fun replaceTop(project: Project) {
        list[index] = project
    }

    fun push(project: Project) {
        if (current.contentEquals(project)) return
        list.removeRange(index + 1, list.size)
        list.add(project)
        if (list.size > MaxStackSize) list.removeAt(0)
        index = list.lastIndex
    }

    private fun Project.contentEquals(other: Project) = copy(
        currentIndex = other.currentIndex,
        entryFilter = other.entryFilter,
    ) == other

    fun undo() {
        if (canUndo) index--
    }

    fun redo() {
        if (canRedo) index++
    }

    companion object {
        private const val MaxStackSize = 100
    }
}
