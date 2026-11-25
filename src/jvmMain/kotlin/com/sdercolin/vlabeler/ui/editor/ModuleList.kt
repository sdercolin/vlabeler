package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Module
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.common.ContextMenuSubject
import com.sdercolin.vlabeler.ui.common.NavigatorItemSummary
import com.sdercolin.vlabeler.ui.common.NavigatorListBody
import com.sdercolin.vlabeler.ui.common.NavigatorListItemNumber
import com.sdercolin.vlabeler.ui.common.NavigatorListState
import com.sdercolin.vlabeler.ui.common.NoOpContextMenuAction
import com.sdercolin.vlabeler.ui.common.SearchBar
import com.sdercolin.vlabeler.ui.common.onPreviewKeyEvent
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.util.toFile

@Stable
class ModuleListStateItem(override val index: Int, val module: Module) : ContextMenuSubject<NoOpContextMenuAction> {
    @Composable
    override fun getContextMenuActions(): List<NoOpContextMenuAction> = emptyList()
}

class ModuleListState(
    project: Project,
    private val jumpToModule: (Int) -> Unit,
) : NavigatorListState<ModuleListStateItem, NoOpContextMenuAction> {
    var modules = project.modules.withIndex().toList()
        private set
    override var currentIndex = 0
        private set
    override val labelerConf: LabelerConf = project.labelerConf

    var searchText: String by mutableStateOf("")
    private val initialResult = calculateResult()
    override var isFiltered: Boolean by mutableStateOf(initialResult.first)
    override var searchResult: List<ModuleListStateItem> by mutableStateOf(initialResult.second)
    override var selectedIndex: Int? by mutableStateOf(null)

    override var hasFocus: Boolean by mutableStateOf(false)

    override fun submit(index: Int) {
        jumpToModule(index)
    }

    override fun calculateResult(): Pair<Boolean, List<ModuleListStateItem>> {
        val filteredModules = modules.filter { it.value.name.contains(searchText, true) }
        return searchText.isNotEmpty() to filteredModules.map {
            ModuleListStateItem(index = it.index, it.value)
        }
    }

    override fun updateProject(project: Project) {
        modules = project.modules.withIndex().toList()
        currentIndex = project.currentModuleIndex
        updateSearch()
    }
}

@Composable
fun ModuleList(
    project: Project,
    jumpToModule: (Int) -> Unit,
    state: ModuleListState = remember(jumpToModule) {
        ModuleListState(
            project,
            jumpToModule,
        )
    },
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    LaunchedEffect(project) { state.updateProject(project) }

    Column(
        modifier = Modifier.fillMaxWidth(0.4f).fillMaxHeight(0.7f).plainClickable(),
    ) {
        SearchBar(
            text = state.searchText,
            onTextChange = {
                state.searchText = it
                state.updateSearch(selectFirst = true)
            },
            focusRequester = focusRequester,
            onFocusedChanged = {
                state.hasFocus = it
            },
            onPreviewKeyEvent = state::onPreviewKeyEvent,
            onSubmit = state::submitCurrent,
        )

        NavigatorListBody(
            state = state,
            itemContent = { ItemContent(it) },
            contextMenuActionConsumer = null,
        )
    }
}

@Composable
private fun ItemContent(item: ModuleListStateItem) {
    val subtext = item.module.sampleDirectoryPath.let {
        if (it.toFile().isAbsolute) {
            it
        } else {
            "./$it"
        }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        NavigatorListItemNumber(item.index)
        NavigatorItemSummary(item.module.name, subtext)
    }
}
