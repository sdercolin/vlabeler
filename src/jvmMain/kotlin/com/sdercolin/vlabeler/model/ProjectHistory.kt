package com.sdercolin.vlabeler.model

data class ProjectHistory(
    val list: List<Project> = listOf(),
    val index: Int = -1,
) {
    val current get() = list[index]
    val canUndo get() = index > 0
    val canRedo get() = index >= 0 && index < list.lastIndex

    fun replaceTop(project: Project): ProjectHistory {
        val list = list.toMutableList()
        list[index] = project
        return copy(list = list)
    }

    fun push(project: Project): ProjectHistory {
        if (current.contentEquals(project)) return this
        return (list.subList(0, index + 1) + project)
            .takeLast(MaxStackSize)
            .let {
                ProjectHistory(
                    list = it,
                    index = it.lastIndex,
                )
            }
    }

    private fun Project.contentEquals(other: Project) = copy(
        currentIndex = other.currentIndex,
        entryFilter = other.entryFilter,
    ) == other

    fun undo() = copy(index = index.minus(1).coerceAtLeast(0))

    fun redo() = copy(index = index.plus(1).coerceAtMost(list.lastIndex))

    companion object {
        private const val MaxStackSize = 100

        fun new(project: Project) = ProjectHistory(list = listOf(project), 0)
    }
}
