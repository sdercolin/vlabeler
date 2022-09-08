@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)

package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.env.isReleased
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.common.DoneIcon
import com.sdercolin.vlabeler.ui.common.FreeSizedIconButton
import com.sdercolin.vlabeler.ui.common.SearchBar
import com.sdercolin.vlabeler.ui.common.StarIcon
import com.sdercolin.vlabeler.ui.common.Tooltip
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.DarkGreen
import com.sdercolin.vlabeler.ui.theme.DarkYellow
import com.sdercolin.vlabeler.ui.theme.LightGray
import com.sdercolin.vlabeler.ui.theme.White20
import com.sdercolin.vlabeler.ui.theme.White80
import com.sdercolin.vlabeler.util.animateScrollToShowItem
import com.sdercolin.vlabeler.util.runIf

class EntryListState(
    private val filterState: EntryListFilterState,
    project: Project,
    private val jumpToEntry: (Int) -> Unit,
) {
    var entries = project.entries.withIndex().toList()
        private set
    var currentIndex = project.currentIndex
        private set

    var searchResult: List<IndexedValue<Entry>> by mutableStateOf(calculateResult())
    var selectedIndex: Int? by mutableStateOf(
        searchResult.indexOfFirst { it.index == currentIndex }.takeIf { it >= 0 },
    )

    var hasFocus: Boolean by mutableStateOf(false)
    var isFilterExpanded: Boolean by mutableStateOf(filterState.filter.isEmpty().not())

    fun submit(index: Int) {
        jumpToEntry(index)
    }

    private fun calculateResult(): List<IndexedValue<Entry>> = entries.filter { filterState.filter.matches(it.value) }

    fun updateProject(project: Project) {
        entries = project.entries.withIndex().toList()
        currentIndex = project.currentIndex
        updateSearch()
    }

    fun updateSearch() {
        val newResults = calculateResult()
        searchResult = newResults
        selectedIndex = if (hasFocus) {
            if (newResults.isNotEmpty()) 0 else null
        } else {
            newResults.indexOfFirst { it.index == currentIndex }.takeIf { it >= 0 }
        }
    }
}

@Composable
fun EntryList(
    pinned: Boolean,
    filterState: EntryListFilterState,
    project: Project,
    jumpToEntry: (Int) -> Unit,
    onFocusedChanged: (Boolean) -> Unit,
    state: EntryListState = remember(filterState, jumpToEntry) { EntryListState(filterState, project, jumpToEntry) },
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
            onPreviewKeyEvent = {
                if (state.searchResult.isEmpty()) return@SearchBar false
                val index = state.selectedIndex ?: return@SearchBar false
                when {
                    it.isReleased(Key.DirectionDown) -> {
                        state.selectedIndex = index.plus(1).coerceAtMost(state.searchResult.lastIndex)
                        true
                    }
                    it.isReleased(Key.DirectionUp) -> {
                        state.selectedIndex = index.minus(1).coerceAtLeast(0)
                        true
                    }
                    it.isReleased(Key.Enter) -> {
                        state.searchResult[index].index.let(state::submit)
                        true
                    }
                    else -> false
                }
            },
            trailingContent = {
                if (pinned) {
                    FreeSizedIconButton(
                        onClick = { state.isFilterExpanded = !state.isFilterExpanded },
                        modifier = Modifier.padding(8.dp),
                    ) {
                        val icon = if (state.isFilterExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore
                        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
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
            FilterRow(filterState as LinkableEntryListFilterState, state::updateSearch)
        }

        List(state)
    }
}

@Composable
private fun FilterRow(filterState: LinkableEntryListFilterState, updateSearch: () -> Unit) {
    Row(modifier = Modifier.padding(horizontal = 5.dp)) {
        TooltipArea(
            tooltip = {
                val strings = when (filterState.filter.done) {
                    true -> Strings.FilterDone
                    false -> Strings.FilterUndone
                    null -> Strings.FilterDoneIgnored
                }
                Tooltip(string(strings))
            },
        ) {
            FreeSizedIconButton(
                onClick = {
                    filterState.editFilter { doneNexted() }
                    updateSearch()
                },
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            ) {
                val tint = when (filterState.filter.done) {
                    true -> DarkGreen
                    false -> White80
                    null -> White20
                }
                Icon(
                    imageVector = Icons.Default.Done,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = tint,
                )
            }
        }
        TooltipArea(
            tooltip = {
                val strings = when (filterState.filter.star) {
                    true -> Strings.FilterStarred
                    false -> Strings.FilterUnstarred
                    null -> Strings.FilterStarIgnored
                }
                Tooltip(string(strings))
            },
        ) {
            FreeSizedIconButton(
                onClick = {
                    filterState.editFilter { starNexted() }
                    updateSearch()
                },
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp),
            ) {
                val icon = when (filterState.filter.star) {
                    true -> Icons.Default.Star
                    else -> Icons.Default.StarOutline
                }
                val tint = when (filterState.filter.star) {
                    true -> DarkYellow
                    false -> White80
                    null -> White20
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = tint,
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        TooltipArea(
            tooltip = {
                val strings = if (filterState.linked) {
                    Strings.FilterLinked
                } else {
                    Strings.FilterLink
                }
                Tooltip(string(strings))
            },
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
    }
}

@Composable
private fun ColumnScope.List(state: EntryListState) {
    var pressedIndex by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberLazyListState(initialFirstVisibleItemIndex = state.currentIndex)
    val scrollbarAdapter = remember { ScrollbarAdapter(scrollState) }

    LaunchedEffect(state.selectedIndex, state.searchResult) {
        val index = state.selectedIndex
        if (index != null) {
            scrollState.animateScrollToShowItem(index)
        }
    }
    Box(
        Modifier.weight(1f)
            .fillMaxWidth()
            .background(color = MaterialTheme.colors.background.copy(alpha = 0.35f)),
    ) {
        LazyColumn(state = scrollState) {
            itemsIndexed(state.searchResult) { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .height(30.dp)
                        .runIf(index == state.selectedIndex) {
                            background(color = MaterialTheme.colors.primaryVariant)
                        }
                        .padding(end = 20.dp)
                        .onPointerEvent(PointerEventType.Press) {
                            if (it.buttons.isPrimaryPressed.not() || it.buttons.isSecondaryPressed) {
                                return@onPointerEvent
                            }
                            pressedIndex = index
                            state.selectedIndex = index
                        }
                        .onPointerEvent(PointerEventType.Exit) {
                            if (it.buttons.isPrimaryPressed.not()) return@onPointerEvent
                            if (pressedIndex == index) {
                                pressedIndex = null
                            }
                        }
                        .onPointerEvent(PointerEventType.Release) {
                            if (pressedIndex == index) {
                                state.submit(state.searchResult[index].index)
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ItemContent(item)
                }
            }
        }
        VerticalScrollbar(
            adapter = scrollbarAdapter,
            modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().width(15.dp),
        )
    }
}

@Composable
private fun ItemContent(item: IndexedValue<Entry>) {
    Layout(
        content = {
            BasicText(
                text = "${item.index + 1}",
                modifier = Modifier.padding(start = 20.dp, end = 15.dp, top = 3.dp).widthIn(20.dp),
                maxLines = 1,
                style = MaterialTheme.typography.caption.copy(color = LightGray.copy(alpha = 0.5f)),
            )
            Row {
                BasicText(
                    text = item.value.name,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onBackground),
                )
                BasicText(
                    text = item.value.sample,
                    modifier = Modifier.padding(start = 10.dp, top = 3.dp),
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1,
                    style = MaterialTheme.typography.caption.copy(color = LightGray.copy(alpha = 0.5f)),
                )
            }

            Row {
                if (item.value.meta.done) {
                    DoneIcon(true, modifier = Modifier.padding(start = 8.dp).requiredSize(16.dp))
                }
                if (item.value.meta.star) {
                    StarIcon(true, modifier = Modifier.padding(start = 8.dp).requiredSize(16.dp))
                }
            }
        },
    ) { measurables, constraints ->
        val (head, middle, tail) = measurables
        val placeables = mutableListOf<Placeable>()
        head.measure(constraints).let { placeables.add(it) }
        tail.measure(constraints.copy(maxWidth = constraints.maxWidth - placeables.first().measuredWidth))
            .let { placeables.add(it) }
        middle.measure(constraints.copy(maxWidth = constraints.maxWidth - placeables.sumOf { it.measuredWidth }))
            .let { placeables.add(1, it) }

        layout(constraints.maxWidth, constraints.maxHeight) {
            var xPosition = 0
            placeables.forEachIndexed { index, placeable ->
                val x = if (index <= 1) xPosition else constraints.maxWidth - placeable.width
                val y = (constraints.maxHeight - placeable.height) / 2
                placeable.placeRelative(x, y)
                xPosition += placeable.width
            }
        }
    }
}
