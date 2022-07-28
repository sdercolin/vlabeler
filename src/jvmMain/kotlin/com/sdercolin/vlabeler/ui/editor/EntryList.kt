@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.editor

import androidx.compose.foundation.ScrollbarAdapter
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.env.isReleased
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.theme.LightGray
import com.sdercolin.vlabeler.ui.theme.White20
import com.sdercolin.vlabeler.util.animateScrollToShowItem
import com.sdercolin.vlabeler.util.runIf

@Composable
fun EntryList(pinned: Boolean, project: Project, jumpToEntry: (Int) -> Unit) {
    val entries = project.entries.withIndex().toList()
    val currentIndex = project.currentIndex

    val focusRequester = remember { FocusRequester() }
    val submit = { index: Int -> jumpToEntry(index) }

    fun getSearchResult(searchText: String?): List<IndexedValue<Entry>> {
        return if (searchText == null) entries else entries.filter { it.value.name.contains(searchText) }
    }

    var searchText by remember { mutableStateOf<String?>(null) }
    var searchResult by remember(project) { mutableStateOf(getSearchResult(searchText)) }
    var selectedIndex by remember(project.currentIndex) {
        mutableStateOf(searchResult.indexOfFirst { it.index == project.currentIndex }.takeIf { it >= 0 })
    }

    fun search(text: String?) {
        searchText = text
        val newResults = getSearchResult(text)
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

    Column(sizeModifier.plainClickable()) {
        Row(
            modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colors.onSurface)
            Spacer(Modifier.width(15.dp))
            BasicTextField(
                value = searchText.orEmpty(),
                modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 10.dp)
                    .focusRequester(focusRequester)
                    .onPreviewKeyEvent {
                        if (searchResult.isEmpty()) return@onPreviewKeyEvent false
                        val index = selectedIndex ?: return@onPreviewKeyEvent false
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
                onValueChange = { text -> search(text.takeIf { it.isNotEmpty() }) },
                textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onBackground),
                cursorBrush = SolidColor(MaterialTheme.colors.onBackground),
                singleLine = true
            )
        }
        Divider(color = White20)
        if (searchResult.isNotEmpty()) {
            List(
                searchResult = searchResult,
                currentIndex = currentIndex,
                selectedIndex = selectedIndex,
                select = { selectedIndex = it }
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
    submit: (Int) -> Unit
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicText(
                        text = "${item.index + 1}",
                        modifier = Modifier.padding(start = 20.dp, end = 15.dp, top = 3.dp).widthIn(20.dp),
                        maxLines = 1,
                        style = MaterialTheme.typography.caption.copy(color = LightGray.copy(alpha = 0.5f))
                    )
                    BasicText(
                        text = item.value.name,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onBackground)
                    )
                    BasicText(
                        text = item.value.sample,
                        modifier = Modifier.padding(start = 10.dp, top = 3.dp),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.caption.copy(color = LightGray.copy(alpha = 0.5f))
                    )
                }
            }
        }
        VerticalScrollbar(
            adapter = scrollbarAdapter,
            modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().width(15.dp)
        )
    }
}
