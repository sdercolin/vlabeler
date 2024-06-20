package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.AppDialogState
import com.sdercolin.vlabeler.ui.common.DoneIcon
import com.sdercolin.vlabeler.ui.common.DoneTriStateIcon
import com.sdercolin.vlabeler.ui.common.FreeSizedIconButton
import com.sdercolin.vlabeler.ui.common.NavigatorItemSummary
import com.sdercolin.vlabeler.ui.common.NavigatorListBody
import com.sdercolin.vlabeler.ui.common.NavigatorListItemNumber
import com.sdercolin.vlabeler.ui.common.NavigatorListState
import com.sdercolin.vlabeler.ui.common.SearchBar
import com.sdercolin.vlabeler.ui.common.StarIcon
import com.sdercolin.vlabeler.ui.common.StarTriStateIcon
import com.sdercolin.vlabeler.ui.common.WithTooltip
import com.sdercolin.vlabeler.ui.common.onPreviewKeyEvent
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.dialog.EntryFilterSetterDialogArgs
import com.sdercolin.vlabeler.ui.dialog.EntryFilterSetterDialogResult
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.ui.theme.LightGray
import com.sdercolin.vlabeler.ui.theme.White20
import kotlinx.coroutines.launch

class EntryListState(
    private val filterState: EntryListFilterState,
    project: Project,
    private val jumpToEntry: (Int) -> Unit,
    private val dialogState: AppDialogState?,
) : NavigatorListState<Entry> {
    var entries = project.currentModule.entries.withIndex().toList()
        private set
    override var currentIndex = 0
        private set

    override var searchResult: List<IndexedValue<Entry>> by mutableStateOf(calculateResult())
    override var selectedIndex: Int? by mutableStateOf(null)

    override var hasFocus: Boolean by mutableStateOf(false)
    var isFilterExpanded: Boolean by mutableStateOf(
        filterState.filter.isEmpty().not() || (filterState as? LinkableEntryListFilterState)?.linked == true,
    )

    override fun submit(index: Int) {
        jumpToEntry(index)
    }

    override fun calculateResult(): List<IndexedValue<Entry>> = entries.filter { filterState.filter.matches(it.value) }

    override fun updateProject(project: Project) {
        entries = project.currentModule.entries.withIndex().toList()
        currentIndex = project.currentModule.currentIndex
        updateSearch()
    }

    suspend fun editFilterInDialog() {
        val args = EntryFilterSetterDialogArgs(filterState.filter)
        val result = dialogState?.awaitEmbeddedDialog(args) ?: return
        val newValue = (result as? EntryFilterSetterDialogResult)?.value ?: return
        filterState.editFilter { newValue }
        updateSearch()
    }
}

@Composable
fun EntryList(
    editorConf: AppConf.Editor,
    viewConf: AppConf.View,
    pinned: Boolean,
    filterState: EntryListFilterState,
    project: Project,
    jumpToEntry: (Int) -> Unit,
    onFocusedChanged: (Boolean) -> Unit,
    dialogState: AppDialogState?,
    state: EntryListState = remember(editorConf, filterState, jumpToEntry) {
        EntryListState(
            filterState,
            project,
            jumpToEntry,
            dialogState,
        )
    },
) {
    val focusRequester = remember { FocusRequester() }

    if (!pinned) LaunchedEffect(Unit) { focusRequester.requestFocus() }

    LaunchedEffect(project) { state.updateProject(project) }

    val sizeModifier = if (pinned) {
        Modifier.fillMaxSize()
    } else {
        Modifier.fillMaxWidth(0.5f).fillMaxHeight(0.7f)
    }

    Column(
        modifier = sizeModifier.plainClickable(),
    ) {
        SearchBar(
            text = filterState.filter.searchText,
            onTextChange = {
                filterState.editFilter { copy(searchText = it) }
                state.updateSearch()
            },
            focusRequester = focusRequester,
            onFocusedChanged = {
                state.hasFocus = it
                onFocusedChanged(it)
            },
            onPreviewKeyEvent = state::onPreviewKeyEvent,
            onSubmit = state::submitCurrent,
            trailingContent = {
                if (pinned) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val countText = "${state.searchResult.size} / ${state.entries.size}"
                        val isFiltered = state.searchResult.size != state.entries.size
                        val alpha = if (isFiltered) 0.8f else 0.4f
                        val color = MaterialTheme.colors.onSurface.copy(alpha)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = countText,
                            modifier = Modifier.background(color = White20, shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            color = color,
                            style = MaterialTheme.typography.caption,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FreeSizedIconButton(
                            onClick = { state.isFilterExpanded = !state.isFilterExpanded },
                            modifier = Modifier.padding(8.dp),
                        ) {
                            val icon =
                                if (state.isFilterExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore
                            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            },
        )
        val filterShown = pinned && state.isFilterExpanded
        Divider(
            color = when {
                state.hasFocus -> MaterialTheme.colors.primaryVariant
                filterShown -> White20
                else -> Color.Transparent
            },
            thickness = if (state.hasFocus) 2.dp else 1.dp,
            modifier = Modifier.padding(top = if (state.hasFocus) 0.dp else 1.dp),
        )
        if (filterShown) {
            FilterRow(
                filterState = filterState as LinkableEntryListFilterState,
                updateSearch = state::updateSearch,
                editInDialog = state::editFilterInDialog,
            )
        }

        NavigatorListBody(
            state = state,
            itemContent = { ItemContent(editorConf, viewConf, it) },
        )
    }
}

@Composable
private fun FilterRow(
    filterState: LinkableEntryListFilterState,
    updateSearch: () -> Unit,
    editInDialog: suspend () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    Row(modifier = Modifier.padding(horizontal = 5.dp)) {
        WithTooltip(
            tooltip = string(
                when (filterState.filter.done) {
                    true -> Strings.FilterDone
                    false -> Strings.FilterUndone
                    null -> Strings.FilterDoneIgnored
                },
            ),
        ) {
            FreeSizedIconButton(
                onClick = {
                    filterState.editFilter { doneNexted() }
                    updateSearch()
                },
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            ) {
                DoneTriStateIcon(filterState.filter.done, modifier = Modifier.size(20.dp))
            }
        }
        WithTooltip(
            tooltip = string(
                when (filterState.filter.star) {
                    true -> Strings.FilterStarred
                    false -> Strings.FilterUnstarred
                    null -> Strings.FilterStarIgnored
                },
            ),
        ) {
            FreeSizedIconButton(
                onClick = {
                    filterState.editFilter { starNexted() }
                    updateSearch()
                },
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            ) {
                StarTriStateIcon(filterState.filter.star, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        WithTooltip(
            tooltip = string(
                if (filterState.linked) {
                    Strings.FilterLinked
                } else {
                    Strings.FilterLink
                },
            ),
        ) {
            FreeSizedIconButton(
                onClick = { filterState.toggleLinked() },
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            ) {
                val icon = if (filterState.linked) Icons.Default.Link else Icons.Default.LinkOff
                val tint = if (filterState.linked) LightGray else White20
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = tint,
                )
            }
        }
        FreeSizedIconButton(
            onClick = {
                filterState.clear()
                updateSearch()
            },
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
        FreeSizedIconButton(
            onClick = {
                coroutineScope.launch {
                    editInDialog()
                }
            },
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun ItemContent(editorConf: AppConf.Editor, viewConf: AppConf.View, item: IndexedValue<Entry>) {
    Layout(
        content = {
            NavigatorListItemNumber(item.index)
            NavigatorItemSummary(item.value.name, item.value.sample, viewConf.hideSampleExtension, isEntry = true)
            Row {
                if (item.value.notes.tag.isNotEmpty() && editorConf.showTag) {
                    Spacer(modifier = Modifier.width(12.dp))
                    BasicText(
                        text = item.value.notes.tag,
                        modifier = Modifier
                            .offset(y = 1.dp)
                            .background(color = White20, shape = RoundedCornerShape(5.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.caption.copy(color = LightGray.copy(alpha = 0.8f)),
                    )
                }
            }

            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (item.value.notes.done && editorConf.showDone) {
                    DoneIcon(true, modifier = Modifier.requiredSize(16.dp))
                }
                if (item.value.notes.star && editorConf.showStar) {
                    StarIcon(true, modifier = Modifier.requiredSize(16.dp))
                }
            }
        },
    ) { measurables, constraints ->
        val (head, middle, tag, tail) = measurables
        val placeables = mutableListOf<Placeable>()
        head.measure(constraints).let { placeables.add(it) }
        tail.measure(constraints.copy(maxWidth = constraints.maxWidth - placeables.first().measuredWidth))
            .let { placeables.add(it) }
        tag.measure(constraints.copy(maxWidth = constraints.maxWidth - placeables.sumOf { it.measuredWidth }))
            .let { placeables.add(1, it) }
        middle.measure(constraints.copy(maxWidth = constraints.maxWidth - placeables.sumOf { it.measuredWidth }))
            .let { placeables.add(1, it) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            var xPosition = 0
            placeables.forEachIndexed { index, placeable ->
                val x = if (index <= 2) xPosition else constraints.maxWidth - placeable.width
                val y = (constraints.maxHeight - placeable.height) / 2
                placeable.placeRelative(x, y)
                xPosition += placeable.width
            }
        }
    }
}
