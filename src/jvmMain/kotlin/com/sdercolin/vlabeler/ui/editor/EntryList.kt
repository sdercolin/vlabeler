@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.focus.onFocusChanged
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
import com.sdercolin.vlabeler.model.filter.EntryFilter
import com.sdercolin.vlabeler.ui.common.DoneIcon
import com.sdercolin.vlabeler.ui.common.SearchBar
import com.sdercolin.vlabeler.ui.common.StarIcon
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.theme.LightGray
import com.sdercolin.vlabeler.ui.theme.White20
import com.sdercolin.vlabeler.util.animateScrollToShowItem
import com.sdercolin.vlabeler.util.runIf

@Composable
fun EntryList(pinned: Boolean, project: Project, jumpToEntry: (Int) -> Unit, onFocusedChanged: (Boolean) -> Unit) {
    val entries = project.entries.withIndex().toList()
    val currentIndex = project.currentIndex

    val focusRequester = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    val submit = { index: Int -> jumpToEntry(index) }

    var searchText by remember { mutableStateOf("") }
    var filter: EntryFilter? by remember { mutableStateOf(EntryFilter(searchText).validated()) }

    fun getSearchResult(): List<IndexedValue<Entry>> = entries.filter { filter?.matches(it.value) != false }

    var searchResult by remember(project) { mutableStateOf(getSearchResult()) }
    var selectedIndex by remember(project.currentIndex) {
        mutableStateOf(searchResult.indexOfFirst { it.index == project.currentIndex }.takeIf { it >= 0 })
    }

    fun updateSearch() {
        filter = EntryFilter(searchText).validated()
        val newResults = getSearchResult()
        if (searchResult == newResults) return
        searchResult = newResults
        selectedIndex = 0
    }

    if (!pinned) LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val sizeModifier = if (pinned) {
        Modifier.fillMaxSize()
    } else {
        Modifier.fillMaxWidth(0.5f).fillMaxHeight(0.7f)
    }

    Column(
        modifier = sizeModifier.plainClickable()
            .onFocusChanged {
                hasFocus = it.hasFocus
                onFocusedChanged(hasFocus)
            },
    ) {
        SearchBar(
            text = searchText,
            onTextChange = {
                searchText = it
                updateSearch()
            },
            focusRequester = focusRequester,
            onPreviewKeyEvent = {
                if (searchResult.isEmpty()) return@SearchBar false
                val index = selectedIndex ?: return@SearchBar false
                when {
                    it.isReleased(Key.DirectionDown) -> {
                        selectedIndex = index.plus(1).coerceAtMost(searchResult.lastIndex)
                        true
                    }
                    it.isReleased(Key.DirectionUp) -> {
                        selectedIndex = index.minus(1).coerceAtLeast(0)
                        true
                    }
                    it.isReleased(Key.Enter) -> {
                        searchResult[index].index.let(submit)
                        true
                    }
                    else -> false
                }
            },
        )
        Divider(
            color = if (hasFocus) MaterialTheme.colors.primaryVariant else White20,
            thickness = if (hasFocus) 2.dp else 1.dp,
            modifier = Modifier.padding(top = if (hasFocus) 0.dp else 1.dp),
        )

        if (searchResult.isNotEmpty()) {
            List(
                searchResult = searchResult,
                currentIndex = currentIndex,
                selectedIndex = selectedIndex,
                select = { selectedIndex = it },
            ) { searchResult[it].index.let(submit) }
        }
    }
}

@Composable
private fun ColumnScope.List(
    searchResult: List<IndexedValue<Entry>>,
    currentIndex: Int,
    selectedIndex: Int?,
    select: (Int) -> Unit,
    submit: (Int) -> Unit,
) {
    var pressedIndex by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberLazyListState(initialFirstVisibleItemIndex = currentIndex)
    val scrollbarAdapter = remember { ScrollbarAdapter(scrollState) }

    LaunchedEffect(selectedIndex, searchResult) {
        if (selectedIndex != null) {
            scrollState.animateScrollToShowItem(selectedIndex)
        }
    }
    Box(Modifier.weight(1f)) {
        LazyColumn(state = scrollState) {
            itemsIndexed(searchResult) { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .height(30.dp)
                        .runIf(index == selectedIndex) {
                            background(color = MaterialTheme.colors.primaryVariant)
                        }
                        .padding(end = 20.dp)
                        .onPointerEvent(PointerEventType.Press) {
                            if (it.buttons.isPrimaryPressed.not() || it.buttons.isSecondaryPressed) {
                                return@onPointerEvent
                            }
                            pressedIndex = index
                            select(index)
                        }
                        .onPointerEvent(PointerEventType.Exit) {
                            if (it.buttons.isPrimaryPressed.not()) return@onPointerEvent
                            if (pressedIndex == index) {
                                pressedIndex = null
                            }
                        }
                        .onPointerEvent(PointerEventType.Release) {
                            if (pressedIndex == index) {
                                submit(index)
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
