@file:OptIn(ExperimentalComposeUiApi::class)

package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.ScrollbarAdapter
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.env.isReleased
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.ui.theme.LightGray
import com.sdercolin.vlabeler.util.alpha
import com.sdercolin.vlabeler.util.animateScrollToShowItem
import com.sdercolin.vlabeler.util.runIfHave

interface NavigatorListState<S : ContextMenuSubject<A>, A : ContextMenuAction<A>> {

    val currentIndex: Int
    var selectedIndex: Int?
    var isFiltered: Boolean
    var searchResult: List<S>
    var hasFocus: Boolean

    val labelerConf: LabelerConf

    val allowMultiSelection: Boolean get() = false
    val multiSelectedIndexes: Set<Int> get() = emptySet()
    fun multiSelectToggle(index: Int) {}
    fun multiSelectRange(index: Int) {}
    fun multiSelectClear() {}

    fun submit(index: Int)
    fun updateProject(project: Project)
    fun calculateResult(): Pair<Boolean, List<S>>

    /**
     * Recalculates the displayed results and updates the highlighted item.
     *
     * @param selectFirst when true (the user is actively typing a search query), the first result is highlighted so
     *   that pressing Enter jumps to the top match. When false (the list is being opened or the current entry/module
     *   changed via navigation), the current entry/module is highlighted instead, giving a useful pre-selection.
     */
    fun updateSearch(selectFirst: Boolean = false) {
        val (active, newResults) = calculateResult()
        searchResult = newResults
        selectedIndex = when {
            newResults.isEmpty() -> null
            selectFirst -> 0
            else -> newResults.indexOfFirst { it.index == currentIndex }.takeIf { it >= 0 }
        }
        isFiltered = active
        multiSelectClear()
    }

    fun submitCurrent() {
        val index = selectedIndex ?: return
        searchResult[index].index.let(::submit)
    }
}

fun <S : ContextMenuSubject<A>, A : ContextMenuAction<A>> NavigatorListState<S, A>.onPreviewKeyEvent(
    event: KeyEvent,
): Boolean {
    if (searchResult.isEmpty()) return false
    val index = selectedIndex ?: return false
    return when {
        event.isReleased(Key.DirectionDown) -> {
            selectedIndex = index.plus(1).coerceAtMost(searchResult.lastIndex)
            true
        }
        event.isReleased(Key.DirectionUp) -> {
            selectedIndex = index.minus(1).coerceAtLeast(0)
            true
        }
        else -> false
    }
}

@Composable
fun <S : ContextMenuSubject<A>, A : ContextMenuAction<A>> ColumnScope.NavigatorListBody(
    state: NavigatorListState<S, A>,
    itemContent: @Composable RowScope.(item: S) -> Unit,
    contextMenuActionConsumer: ((A) -> Unit)?,
) {
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
                WithContextMenu(items = { item.getContextMenuActions() }, consumer = contextMenuActionConsumer) {
                    val hoverInteractionSource = remember { MutableInteractionSource() }
                    val isHovered by hoverInteractionSource.collectIsHoveredAsState()
                    val backgroundColor = when {
                        index == state.selectedIndex -> MaterialTheme.colors.primaryVariant
                        index in state.multiSelectedIndexes -> MaterialTheme.colors.primaryVariant.alpha(0.7f)
                        isHovered -> MaterialTheme.colors.primaryVariant.alpha(0.5f)
                        else -> null
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .height(30.dp)
                            .runIfHave(backgroundColor) {
                                background(color = it)
                            }
                            .hoverable(hoverInteractionSource)
                            .padding(end = 20.dp)
                            .onPointerEvent(PointerEventType.Press) {
                                if (it.buttons.isSecondaryPressed) {
                                    // Right-clicking outside the multi-selection dismisses it, so that
                                    // the context menu targets the item under the cursor.
                                    if (index !in state.multiSelectedIndexes) {
                                        state.multiSelectClear()
                                    }
                                    return@onPointerEvent
                                }
                                if (it.buttons.isPrimaryPressed.not()) {
                                    return@onPointerEvent
                                }
                                if (state.allowMultiSelection) {
                                    val modifiers = it.keyboardModifiers
                                    when {
                                        modifiers.isCtrlPressed || modifiers.isMetaPressed -> {
                                            state.multiSelectToggle(index)
                                            return@onPointerEvent
                                        }
                                        modifiers.isShiftPressed -> {
                                            state.multiSelectRange(index)
                                            return@onPointerEvent
                                        }
                                        else -> state.multiSelectClear()
                                    }
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
                        itemContent(item)
                    }
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
fun NavigatorListItemNumber(index: Int) {
    BasicText(
        text = "${index + 1}",
        modifier = Modifier.padding(start = 20.dp, end = 15.dp, top = 3.dp).widthIn(20.dp),
        maxLines = 1,
        style = MaterialTheme.typography.caption.copy(color = LightGray.copy(alpha = 0.5f)),
    )
}

@Composable
fun NavigatorItemSummary(name: String, subtext: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            text = name,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onBackground),
        )
        BasicText(
            text = subtext,
            modifier = Modifier.padding(start = 10.dp, top = 3.dp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.caption.copy(color = LightGray.copy(alpha = 0.5f)),
        )
    }
}
