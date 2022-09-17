package com.sdercolin.vlabeler.ui.dialog.preferences

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.action.Action
import com.sdercolin.vlabeler.ui.common.ColorHexInputBox
import com.sdercolin.vlabeler.ui.common.FloatInputBox
import com.sdercolin.vlabeler.ui.common.IntegerInputBox
import com.sdercolin.vlabeler.ui.common.SearchBar
import com.sdercolin.vlabeler.ui.common.SelectionBox
import com.sdercolin.vlabeler.ui.common.SingleClickableText
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.dialog.ColorPickerArgs
import com.sdercolin.vlabeler.ui.dialog.ColorPickerDialog
import com.sdercolin.vlabeler.ui.dialog.OpenFileDialog
import com.sdercolin.vlabeler.ui.dialog.SaveFileDialog
import com.sdercolin.vlabeler.ui.string.LocalLanguage
import com.sdercolin.vlabeler.ui.string.LocalizedText
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.Text
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.Black50
import com.sdercolin.vlabeler.ui.theme.getSwitchColors
import com.sdercolin.vlabeler.util.argbHexString
import com.sdercolin.vlabeler.util.rgbHexString
import com.sdercolin.vlabeler.util.runIf
import com.sdercolin.vlabeler.util.toColor

@Composable
private fun rememberPreferencesEditorState(
    currentConf: AppConf,
    submit: (AppConf?) -> Unit,
    apply: (AppConf) -> Unit,
    initialPage: PreferencesPage?,
    onViewPage: (PreferencesPage) -> Unit,
    showSnackbar: (String) -> Unit,
) =
    remember(currentConf, submit, initialPage, onViewPage) {
        PreferencesEditorState(
            initConf = currentConf,
            submit = submit,
            apply = apply,
            initialPage = initialPage,
            onViewPage = onViewPage,
            showSnackbar = showSnackbar,
        )
    }

@Composable
fun PreferencesEditor(
    currentConf: AppConf,
    submit: (AppConf?) -> Unit,
    apply: (AppConf) -> Unit,
    initialPage: PreferencesPage?,
    onViewPage: (PreferencesPage) -> Unit,
    showSnackbar: (String) -> Unit,
    state: PreferencesEditorState = rememberPreferencesEditorState(
        currentConf,
        submit,
        apply,
        initialPage,
        onViewPage,
        showSnackbar,
    ),
) {
    Box(Modifier.fillMaxSize(0.8f).plainClickable()) {
        Column(Modifier.fillMaxSize()) {
            Content(state)
            Divider(Modifier.height(1.dp), color = Black50)
            ButtonBar(
                requestImport = { state.currentFilePicker = PreferencesEditorState.FilePicker.Import },
                requestExport = { state.currentFilePicker = PreferencesEditorState.FilePicker.Export },
                resetPage = { state.resetPage() },
                resetAll = { state.resetAll() },
                cancel = { state.finish(false) },
                canApply = state.needSave,
                apply = { state.save() },
                finish = { state.finish(true) },
            )
        }
        state.keymapItemEditDialogArgs?.let {
            KeymapItemEditDialog(it)
        }
        state.keymapItemEditConflictDialogArgs?.let {
            KeymapItemEditConflictDialog(it)
        }
    }
    FilePicker(state)
}

@Composable
private fun ColumnScope.Content(state: PreferencesEditorState) {
    Row(Modifier.fillMaxWidth().weight(1f)) {
        PageList(state)
        Page(state)
    }
}

@Composable
private fun RowScope.PageList(state: PreferencesEditorState) {
    Box(
        Modifier.fillMaxHeight()
            .weight(0.25f)
            .background(color = MaterialTheme.colors.background),
    ) {
        val lazyListState = rememberLazyListState()
        LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 15.dp), state = lazyListState) {
            items(
                items = state.pages,
                key = { it.model },
            ) { page ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .runIf(page == state.selectedPage) {
                            background(color = MaterialTheme.colors.primaryVariant)
                        }
                        .plainClickable { state.selectPage(page) }
                        .padding(vertical = 3.dp)
                        .padding(start = 10.dp + (page.level * 15).dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (page.canExpand) {
                        val icon = if (page.isExpanded) {
                            Icons.Default.KeyboardArrowRight
                        } else {
                            Icons.Default.KeyboardArrowDown
                        }
                        Icon(
                            imageVector = icon,
                            modifier = Modifier.size(25.dp).plainClickable { state.togglePage(page) },
                            contentDescription = null,
                        )
                    } else {
                        Spacer(Modifier.size(25.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = string(page.model.displayedName),
                        style = MaterialTheme.typography.body2.runIf(page.level == 0) {
                            copy(fontWeight = FontWeight.Bold)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(lazyListState),
            modifier = Modifier.align(Alignment.CenterEnd).width(15.dp),
        )
    }
}

@Composable
private fun RowScope.Page(state: PreferencesEditorState) {
    val page = state.selectedPage.model
    val scrollState = rememberScrollState()
    Box(Modifier.weight(0.75f).fillMaxHeight()) {
        Column(
            modifier = Modifier.fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .runIf(page.scrollable) { verticalScroll(scrollState) },
        ) {
            PageHeader(page, state)
            page.content.forEachIndexed { index, group ->
                if (index > 0) {
                    Spacer(Modifier.height(10.dp))
                }
                Group(group, state)
            }
        }
        if (page.scrollable) {
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier.align(Alignment.CenterEnd).width(30.dp),
            )
        }
    }
}

@Composable
private fun PageHeader(page: PreferencesPage, state: PreferencesEditorState) {
    Column {
        Text(
            modifier = Modifier.padding(vertical = 10.dp),
            text = string(page.displayedName),
            style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            modifier = Modifier.padding(vertical = 10.dp),
            text = string(page.description),
            style = MaterialTheme.typography.caption,
            softWrap = true,
        )
        Spacer(Modifier.height(10.dp))
        page.children.forEach { child ->
            SingleClickableText(
                modifier = Modifier.padding(vertical = 5.dp).padding(start = 15.dp),
                text = string(child.displayedName),
                onClick = { state.selectPageByLink(child) },
            )
        }
        if (page.children.isNotEmpty()) {
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun Group(group: PreferencesGroup, state: PreferencesEditorState) {
    if (group.name != null) {
        Spacer(Modifier.height(15.dp))
        Row {
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                modifier = Modifier.size(25.dp).padding(end = 10.dp),
                contentDescription = null,
            )
            Text(
                modifier = Modifier.padding(vertical = 5.dp),
                text = string(group.name),
                style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (group.description != null) {
            Text(
                modifier = Modifier.padding(vertical = 5.dp).padding(start = 30.dp),
                text = string(group.description),
                style = MaterialTheme.typography.caption,
                softWrap = true,
            )
        }
    }
    group.items.forEach { item ->
        Row(Modifier.padding(vertical = 10.dp)) {
            if (group.name != null) {
                Spacer(Modifier.widthIn(30.dp))
            }
            if (item.title != null) {
                Column(Modifier.widthIn(min = 200.dp, max = if (item.columnStyle) Dp.Unspecified else 400.dp)) {
                    val topMargin = if (item.description == null) 14.dp else 4.dp
                    Text(
                        modifier = Modifier.padding(top = topMargin),
                        text = string(item.title),
                        style = MaterialTheme.typography.body2,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (item.description != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = string(item.description),
                            style = MaterialTheme.typography.caption,
                            softWrap = true,
                        )
                    }
                    if (item.columnStyle) {
                        Spacer(Modifier.height(15.dp))
                        Item(item, state)
                    }
                }
                Spacer(Modifier.width(25.dp))
            }
            if (!item.columnStyle) {
                Item(item, state)
            }
        }
    }
}

@Composable
private fun Item(item: PreferencesItem<*>, state: PreferencesEditorState) {
    when (item) {
        is PreferencesItem.Switch -> SwitchItem(item, state)
        is PreferencesItem.IntegerInput -> IntegerInputItem(item, state)
        is PreferencesItem.FloatInput -> FloatInputItem(item, state)
        is PreferencesItem.ColorStringInput -> ColorStringInputItem(item, state)
        is PreferencesItem.Selection -> SelectionItem(item, state)
        is PreferencesItem.Keymap<*> -> Keymap(item, state)
    }
}

@Composable
fun SwitchItem(item: PreferencesItem.Switch, state: PreferencesEditorState) {
    key(item) {
        Switch(
            enabled = item.enabled(state.conf),
            checked = item.select(state.conf),
            onCheckedChange = { state.update(item, it) },
            colors = getSwitchColors(),
        )
    }
}

@Composable
private fun IntegerInputItem(item: PreferencesItem.IntegerInput, state: PreferencesEditorState) {
    val loadedValue = item.select(state.conf)
    val value = if ((item.max != null && loadedValue > item.max) || (item.min != null && loadedValue < item.min)) {
        item.defaultValue
    } else {
        loadedValue
    }

    IntegerInputBox(
        enabled = item.enabled(state.conf),
        intValue = value,
        onValueChange = { state.update(item, it) },
        min = item.min,
        max = item.max,
    )
}

@Composable
private fun FloatInputItem(item: PreferencesItem.FloatInput, state: PreferencesEditorState) {
    val loadedValue = item.select(state.conf)
    val value = if ((item.max != null && loadedValue > item.max) || (item.min != null && loadedValue < item.min)) {
        item.defaultValue
    } else {
        loadedValue
    }

    FloatInputBox(
        enabled = item.enabled(state.conf),
        floatValue = value,
        onValueChange = { state.update(item, it) },
        min = item.min,
        max = item.max,
    )
}

@Composable
private fun ColorStringInputItem(item: PreferencesItem.ColorStringInput, state: PreferencesEditorState) {
    val value = item.select(state.conf)
    var colorPickerArgs: ColorPickerArgs? by remember { mutableStateOf(null) }

    Row {
        ColorHexInputBox(
            value = value,
            defaultValue = item.defaultValue,
            onValidValue = { state.update(item, it) },
            useAlpha = item.useAlpha,
            enabled = item.enabled(state.conf),
        )
        Spacer(Modifier.width(10.dp))
        IconButton(
            onClick = {
                colorPickerArgs = ColorPickerArgs(
                    color = value.toColor(),
                    useAlpha = item.useAlpha,
                    submit = {
                        if (it != null) {
                            state.update(item, if (item.useAlpha) it.argbHexString else it.rgbHexString)
                        }
                    },
                )
            },
            enabled = item.enabled(state.conf),
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
            )
        }
    }

    colorPickerArgs?.let { args ->
        ColorPickerDialog(
            state.savedConf,
            args.color,
            args.useAlpha,
            submit = {
                args.submit(it)
                colorPickerArgs = null
            },
        )
    }
}

@Composable
private fun <T> SelectionItem(item: PreferencesItem.Selection<T>, state: PreferencesEditorState) {
    SelectionBox(
        enabled = item.enabled(state.conf),
        value = item.select(state.conf),
        onSelect = { state.update(item, it) },
        options = item.options.toList(),
        getText = { value ->
            when (value) {
                is Text -> value.text
                is LocalizedText -> value.getText()
                else -> value.toString()
            }
        },
    )
}

@Composable
private fun <K : Action> Keymap(
    item: PreferencesItem.Keymap<K>,
    state: PreferencesEditorState,
    keymapState: PreferencesKeymapState<K> = remember(item) { PreferencesKeymapState(item, state) },
) {
    val language = LocalLanguage.current
    LaunchedEffect(state.conf) {
        keymapState.update(state.conf, language)
    }

    val lazyListState = rememberLazyListState()
    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            text = keymapState.searchText,
            onTextChange = { keymapState.search(it, language) },
            modifier = Modifier.background(color = MaterialTheme.colors.background),
        )
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.background),
                state = lazyListState,
            ) {
                items(keymapState.displayedKeyBinds, key = { it.action }) { keyBind ->
                    PreferencesKeymapItem(
                        keyBind = keyBind,
                        keymap = item,
                        onClickItem = { keyBindItem, keymapItem ->
                            state.openKeymapItemEditDialog(
                                keyBindItem,
                                keymapItem,
                                keymapState.allKeyBinds,
                            )
                        },
                    )
                }
            }
            VerticalScrollbar(rememberScrollbarAdapter(lazyListState), Modifier.width(15.dp).align(Alignment.CenterEnd))
        }
    }
}

@Composable
private fun ButtonBar(
    requestImport: () -> Unit,
    requestExport: () -> Unit,
    resetPage: () -> Unit,
    resetAll: () -> Unit,
    cancel: () -> Unit,
    canApply: Boolean,
    apply: () -> Unit,
    finish: () -> Unit,
) {
    var settingsExpanded by remember { mutableStateOf(false) }

    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.End) {
        Box {
            IconButton(onClick = { settingsExpanded = true }) {
                Icon(Icons.Default.Settings, null)
            }
            DropdownMenu(
                expanded = settingsExpanded,
                onDismissRequest = { settingsExpanded = false },
            ) {
                DropdownMenuItem(
                    onClick = {
                        requestImport()
                        settingsExpanded = false
                    },
                ) {
                    Text(text = string(Strings.PreferencesEditorImport))
                }
                DropdownMenuItem(
                    onClick = {
                        requestExport()
                        settingsExpanded = false
                    },
                ) {
                    Text(text = string(Strings.PreferencesEditorExport))
                }
                DropdownMenuItem(
                    onClick = {
                        resetPage()
                        settingsExpanded = false
                    },
                ) {
                    Text(text = string(Strings.PreferencesEditorResetPage))
                }
                DropdownMenuItem(
                    onClick = {
                        resetAll()
                        settingsExpanded = false
                    },
                ) {
                    Text(text = string(Strings.PreferencesEditorResetAll))
                }
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { cancel() }) {
            Text(string(Strings.CommonCancel))
        }
        Spacer(Modifier.width(25.dp))
        TextButton(
            enabled = canApply,
            onClick = { apply() },
        ) {
            Text(string(Strings.CommonApply))
        }
        Spacer(Modifier.width(25.dp))
        Button(
            onClick = { finish() },
        ) {
            Text(string(Strings.CommonOkay))
        }
    }
}

@Composable
private fun FilePicker(state: PreferencesEditorState) {
    state.currentFilePicker?.let { picker ->
        val title = string(picker.title)
        val extensions = picker.extensions
        val writeMode = picker.writeMode
        val initialFileName = picker.initialFileName
        if (writeMode) {
            SaveFileDialog(
                title = title,
                extensions = extensions,
                initialFileName = initialFileName,
                onCloseRequest = { parent, name ->
                    state.handleFilePickerResult(picker, parent, name)
                },
            )
        } else {
            OpenFileDialog(
                title = title,
                extensions = extensions,
                initialFileName = initialFileName,
                onCloseRequest = { parent, name ->
                    state.handleFilePickerResult(picker, parent, name)
                },
            )
        }
    }
}
