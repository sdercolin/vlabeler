package com.sdercolin.vlabeler.ui.dialog.sample

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.LargeDialogContainer
import com.sdercolin.vlabeler.ui.common.SelectionBox
import com.sdercolin.vlabeler.ui.common.SingleClickableText
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.dialog.OpenFileDialog
import com.sdercolin.vlabeler.ui.dialog.sample.SampleListDialogItem.Entry
import com.sdercolin.vlabeler.ui.dialog.sample.SampleListDialogItem.IncludedSample
import com.sdercolin.vlabeler.ui.dialog.sample.SampleListDialogItem.Sample
import com.sdercolin.vlabeler.ui.editor.EditorState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.LightGray
import com.sdercolin.vlabeler.ui.theme.White20
import com.sdercolin.vlabeler.util.animateScrollToShowItem
import com.sdercolin.vlabeler.util.runIfHave
import java.io.File

@Composable
private fun rememberState(editorState: EditorState) = remember(
    editorState,
    editorState.project.rootSampleDirectory,
    editorState.project.currentModule.getSampleDirectory(editorState.project),
) {
    SampleListDialogState(editorState)
}

@Composable
fun SampleListDialog(
    editorState: EditorState,
    finish: () -> Unit,
    state: SampleListDialogState = rememberState(editorState),
) {
    LargeDialogContainer {
        Column(modifier = Modifier.fillMaxSize().padding(vertical = 20.dp, horizontal = 30.dp)) {
            Spacer(Modifier.height(5.dp))
            SampleDirectoryBar(
                currentModuleName = state.currentModuleName,
                allModuleNames = state.allModuleNames,
                onSelectModuleName = { state.selectModule(it) },
                directory = state.sampleDirectory,
                valid = state.isSampleDirectoryExisting(),
                requestRedirectSampleDirectory = {
                    state.requestRedirectSampleDirectory()
                },
            )
            Spacer(Modifier.height(15.dp))
            Content(state)
            Spacer(Modifier.height(20.dp))
            ButtonBar(
                finish = finish,
                hasSelectedEntry = state.selectedEntryIndex != null,
                jumpToSelectedEntry = {
                    state.jumpToSelectedEntry()
                    finish()
                },
                canOpenSampleDirectory = state.isSampleDirectoryExisting(),
                openSampleDirectory = { state.openSampleDirectory() },
                canCreateDefault = state.excludedSampleItems.isNotEmpty(),
                createDefault = { state.createDefaultEntriesForAllExcludedSamples() },
            )
        }
    }

    if (state.isShowingSampleDirectoryRedirectDialog) {
        OpenFileDialog(
            title = string(Strings.ChooseSampleDirectoryDialogTitle),
            initialDirectory = state.getInitialSampleDirectoryForRedirection(),
            extensions = null,
            directoryMode = true,
        ) { parent, name ->
            state.handleRedirectionDialogResult(parent, name)
        }
    }
}

@Composable
private fun ColumnScope.Content(state: SampleListDialogState) {
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
        Column(modifier = Modifier.fillMaxHeight().weight(1f)) { Samples(state) }
        Spacer(Modifier.width(30.dp))
        Column(modifier = Modifier.fillMaxHeight().weight(1f)) { Entries(state) }
    }
}

@Composable
private fun ColumnScope.Samples(state: SampleListDialogState) {
    GroupTitle(Strings.SampleListIncludedHeader)
    Spacer(Modifier.height(7.dp))
    val items = state.includedSampleItems
    val selectedIndex = items.indexOfFirst { it.isSelected(state.selectedSampleName) }.takeIf { it >= 0 }
    GroupLazyColumn(weight = 0.65f, selectedIndex = selectedIndex) {
        items(state.includedSampleItems, key = { it.name }) { item ->
            val isSelected = item.isSelected(state.selectedSampleName)
            val textColor = if (item.valid) {
                MaterialTheme.colors.onBackground
            } else {
                MaterialTheme.colors.error
            }
            ItemRow(
                item,
                isSelected,
                textColor,
                onClick = { state.selectSample(item.name) },
            )
        }
    }
    Spacer(Modifier.height(20.dp))
    GroupTitle(Strings.SampleListExcludedHeader)
    Spacer(Modifier.height(7.dp))
    GroupLazyColumn(
        weight = 0.35f,
        placeholder = {
            PlaceholderText(Strings.SampleListExcludedPlaceholder)
        },
    ) {
        items(state.excludedSampleItems, key = { it.name }) { item ->
            val isSelected = item.isSelected(state.selectedSampleName)
            val textColor = MaterialTheme.colors.onBackground.copy(alpha = 0.6f)
            ItemRow(
                item,
                isSelected,
                textColor,
                onClick = { state.selectSample(item.name) },
            )
        }
    }
}

@Composable
private fun ColumnScope.Entries(state: SampleListDialogState) {
    GroupTitle(Strings.SampleListEntryHeader)
    Spacer(Modifier.height(7.dp))
    GroupLazyColumn(
        weight = 1f,
        placeholder = {
            if (state.selectedSampleName == null || state.entryItems.isNotEmpty()) {
                PlaceholderText(Strings.SampleListEntriesPlaceholderUnselected)
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PlaceholderText(Strings.SampleListEntriesPlaceholderNoEntry)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { state.createDefaultEntry() },
                    ) {
                        Text(string(Strings.SampleListEntriesPlaceholderNoEntryButton))
                    }
                }
            }
        },
    ) {
        items(state.entryItems, key = { it.entry.index }) { item ->
            val isSelected = item.isSelected(state.selectedEntryIndex)
            val textColor = MaterialTheme.colors.onBackground
            ItemRow(
                item,
                isSelected,
                textColor,
                onClick = { state.selectEntry(item.entry.index) },
            )
        }
    }
}

@Composable
private fun ItemRow(
    item: SampleListDialogItem,
    isSelected: Boolean,
    textColor: Color,
    onClick: (() -> Unit)? = null,
) {
    val backgroundModifier = if (isSelected) {
        Modifier.background(MaterialTheme.colors.primaryVariant)
    } else Modifier

    Row(
        modifier = backgroundModifier
            .fillMaxWidth()
            .height(35.dp)
            .runIfHave(onClick) { plainClickable(it) }
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val mainStyle = MaterialTheme.typography.body2.copy(color = textColor)
        val subStyle = MaterialTheme.typography.caption.copy(color = LightGray.copy(alpha = 0.5f))
        when (item) {
            is Entry -> {
                BasicText(
                    text = "${item.entry.index + 1}",
                    modifier = Modifier.padding(end = 15.dp, top = 3.dp).widthIn(20.dp),
                    maxLines = 1,
                    style = subStyle,
                )
                BasicText(
                    text = item.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = mainStyle,
                )
                BasicText(
                    text = "${item.entry.start}...${item.entry.end}",
                    modifier = Modifier.padding(start = 10.dp, top = 3.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = subStyle,
                )
            }
            is Sample -> {
                BasicText(
                    text = item.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = mainStyle,
                )
                if (item is IncludedSample) {
                    val text = if (item.entryCount > 1) {
                        string(Strings.SampleListIncludedItemEntryCountPlural, item.entryCount)
                    } else {
                        string(Strings.SampleListIncludedItemEntryCountSingle, item.entryCount)
                    }
                    BasicText(
                        text = text,
                        modifier = Modifier.padding(start = 10.dp, top = 3.dp),
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = subStyle,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderText(stringKey: Strings) {
    Text(
        text = string(stringKey),
        style = MaterialTheme.typography.caption,
        color = MaterialTheme.colors.onBackground,
    )
}

@Composable
private fun GroupTitle(stringKey: Strings) {
    BasicText(
        modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp),
        text = string(stringKey),
        style = MaterialTheme.typography.body2.copy(
            color = MaterialTheme.colors.onBackground,
            fontWeight = FontWeight.Bold,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun ColumnScope.GroupLazyColumn(
    weight: Float,
    placeholder: @Composable BoxScope.() -> Unit = {},
    selectedIndex: Int? = null,
    content: LazyListScope.() -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxHeight()
            .weight(weight)
            .background(color = MaterialTheme.colors.background),
    ) {
        val lazyListState = rememberLazyListState()
        LaunchedEffect(selectedIndex) {
            if (selectedIndex != null) {
                lazyListState.animateScrollToShowItem(selectedIndex)
            }
        }
        LazyColumn(modifier = Modifier.fillMaxSize(), state = lazyListState, content = content)
        if (lazyListState.layoutInfo.totalItemsCount == 0) {
            Box(
                modifier = Modifier.fillMaxSize(0.8f).align(Alignment.Center),
                contentAlignment = Alignment.Center,
            ) {
                placeholder()
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(lazyListState),
            modifier = Modifier.align(Alignment.TopEnd).fillMaxHeight().width(15.dp),
        )
    }
}

@Composable
private fun SampleDirectoryBar(
    currentModuleName: String,
    allModuleNames: List<String>,
    onSelectModuleName: (String) -> Unit,
    directory: File,
    valid: Boolean,
    requestRedirectSampleDirectory: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        if (allModuleNames.size > 1) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BasicText(
                    modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp),
                    text = string(Strings.SampleListCurrentModuleLabel),
                    style = MaterialTheme.typography.body2.copy(
                        color = MaterialTheme.colors.onBackground,
                        fontWeight = FontWeight.Bold,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(5.dp))
                SelectionBox(
                    value = currentModuleName,
                    onSelect = { onSelectModuleName(it) },
                    options = allModuleNames,
                    getText = { it.ifEmpty { string(Strings.CommonRootModuleName) } },
                )
            }
            Spacer(Modifier.height(5.dp))
        }
        Row(Modifier.fillMaxWidth()) {
            BasicText(
                modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp).alignByBaseline(),
                text = string(Strings.SampleListSampleDirectoryLabel),
                style = MaterialTheme.typography.body2.copy(
                    color = MaterialTheme.colors.onBackground,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(5.dp))
            BasicTextField(
                modifier = Modifier.alignByBaseline()
                    .weight(1f)
                    .background(color = White20, shape = RoundedCornerShape(2.dp))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                value = directory.absolutePath,
                onValueChange = {},
                textStyle = MaterialTheme.typography.caption.copy(
                    color = if (valid) MaterialTheme.colors.onBackground else MaterialTheme.colors.error,
                ),
                readOnly = true,
            )
            Spacer(Modifier.width(20.dp))
            SingleClickableText(
                modifier = Modifier.alignByBaseline(),
                text = string(Strings.SampleListSampleDirectoryRedirectButton),
                style = MaterialTheme.typography.caption,
                onClick = { requestRedirectSampleDirectory() },
            )
        }
    }
}

@Composable
private fun ButtonBar(
    finish: () -> Unit,
    hasSelectedEntry: Boolean,
    jumpToSelectedEntry: () -> Unit,
    canOpenSampleDirectory: Boolean,
    openSampleDirectory: () -> Unit,
    canCreateDefault: Boolean,
    createDefault: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(
            enabled = canOpenSampleDirectory,
            onClick = { openSampleDirectory() },
        ) {
            Text(string(Strings.SampleListOpenSampleDirectoryButton))
        }
        Spacer(Modifier.width(25.dp))
        TextButton(
            enabled = canCreateDefault,
            onClick = { createDefault() },
        ) {
            Text(string(Strings.SampleListCreateDefaultForAllButton))
        }
        Spacer(Modifier.weight(1f))
        TextButton(
            enabled = hasSelectedEntry,
            onClick = { jumpToSelectedEntry() },
        ) {
            Text(string(Strings.SampleListJumpToSelectedEntryButton))
        }
        Spacer(Modifier.width(25.dp))
        ConfirmButton(onClick = finish)
    }
}
