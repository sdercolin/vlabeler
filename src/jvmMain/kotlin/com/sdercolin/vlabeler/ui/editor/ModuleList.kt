package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Module
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.common.NavigatorItemSummary
import com.sdercolin.vlabeler.ui.common.NavigatorListBody
import com.sdercolin.vlabeler.ui.common.NavigatorListItemNumber
import com.sdercolin.vlabeler.ui.common.NavigatorListState
import com.sdercolin.vlabeler.ui.common.SearchBar
import com.sdercolin.vlabeler.ui.common.onPreviewKeyEvent
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.util.toFile

class ModuleListState(
    project: Project,
    private val jumpToModule: (Int) -> Unit,
) : NavigatorListState<Module> {
    var modules = project.modules.withIndex().toList()
        private set
    override var currentIndex = 0
        private set

    var searchText: String by mutableStateOf("")
    override var searchResult: List<IndexedValue<Module>> by mutableStateOf(calculateResult())
    override var selectedIndex: Int? by mutableStateOf(null)

    override var hasFocus: Boolean by mutableStateOf(false)

    override fun submit(index: Int) {
        jumpToModule(index)
    }

    override fun calculateResult(): List<IndexedValue<Module>> =
        modules.filter { it.value.name.contains(searchText, true) }

    override fun updateProject(project: Project) {
        modules = project.modules.withIndex().toList()
        currentIndex = project.currentModuleIndex
        updateSearch()
    }
}

@Composable
fun ModuleList(
    viewConf: AppConf.View,
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
                state.updateSearch()
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
            itemContent = { ItemContent(viewConf, it) },
        )
    }
}

@Composable
private fun ItemContent(viewConf: AppConf.View, item: IndexedValue<Module>) {
    val subtext = item.value.sampleDirectoryPath.let {
        if (it.toFile().isAbsolute) {
            it
        } else {
            "./$it"
        }
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        NavigatorListItemNumber(item.index)
        NavigatorItemSummary(item.value.name, subtext, viewConf.hideSampleExtension)
    }
}
