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
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.common.ClickableText
import com.sdercolin.vlabeler.ui.common.FloatInputBox
import com.sdercolin.vlabeler.ui.common.InputBox
import com.sdercolin.vlabeler.ui.common.IntegerInputBox
import com.sdercolin.vlabeler.ui.common.PlainSwitch
import com.sdercolin.vlabeler.ui.common.SelectionBox
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.string.LocalizedText
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.Black50
import com.sdercolin.vlabeler.util.argbHexString
import com.sdercolin.vlabeler.util.isHexChar
import com.sdercolin.vlabeler.util.rgbHexString
import com.sdercolin.vlabeler.util.runIf
import com.sdercolin.vlabeler.util.toColor
import com.sdercolin.vlabeler.util.toColorOrNull
import com.sdercolin.vlabeler.util.toRgbColorOrNull

@Composable
private fun rememberPreferencesEditorState(
    currentConf: AppConf,
    submit: (AppConf?) -> Unit,
    initialPage: PreferencesPage?,
    onViewPage: (PreferencesPage) -> Unit
) =
    remember(currentConf, submit, initialPage, onViewPage) {
        PreferencesEditorState(
            initConf = currentConf,
            submit = submit,
            initialPage = initialPage,
            onViewPage = onViewPage
        )
    }

@Composable
fun PreferencesEditor(
    currentConf: AppConf,
    submit: (AppConf?) -> Unit,
    initialPage: PreferencesPage?,
    onViewPage: (PreferencesPage) -> Unit,
    state: PreferencesEditorState = rememberPreferencesEditorState(currentConf, submit, initialPage, onViewPage)
) {
    Column(Modifier.fillMaxSize(0.8f).plainClickable()) {
        Content(state)
        Divider(Modifier.height(1.dp), color = Black50)
        ButtonBar(
            resetPage = { state.resetPage() },
            resetAll = { state.resetAll() },
            cancel = { state.finish(false) },
            canApply = state.canSave,
            apply = { state.save() },
            finish = { state.finish(true) }
        )
    }
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
            .background(color = MaterialTheme.colors.background)
    ) {
        val lazyListState = rememberLazyListState()
        LazyColumn(modifier = Modifier.fillMaxSize().padding(vertical = 15.dp), state = lazyListState) {
            items(
                items = state.pages,
                key = { it.model }
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
                    verticalAlignment = Alignment.CenterVertically
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
                            contentDescription = null
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
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(lazyListState),
            modifier = Modifier.align(Alignment.CenterEnd).width(15.dp)
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
                .verticalScroll(scrollState)
        ) {
            PageHeader(page, state)
            page.content.forEachIndexed { index, group ->
                if (index > 0) {
                    Spacer(Modifier.height(10.dp))
                }
                Group(group, state)
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterEnd).width(30.dp)
        )
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
            overflow = TextOverflow.Ellipsis
        )
        Text(
            modifier = Modifier.padding(vertical = 10.dp),
            text = string(page.description),
            style = MaterialTheme.typography.caption,
            softWrap = true
        )
        Spacer(Modifier.height(10.dp))
        page.children.forEach { child ->
            ClickableText(
                modifier = Modifier.padding(vertical = 5.dp).padding(start = 15.dp),
                text = string(child.displayedName),
                onClick = { state.selectPageByLink(child) }
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
                contentDescription = null
            )
            Text(
                modifier = Modifier.padding(vertical = 5.dp),
                text = string(group.name),
                style = MaterialTheme.typography.body2.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (group.description != null) {
            Text(
                modifier = Modifier.padding(vertical = 5.dp).padding(start = 30.dp),
                text = string(group.description),
                style = MaterialTheme.typography.caption,
                softWrap = true
            )
        }
    }
    group.items.forEach { item ->
        Row(Modifier.padding(vertical = 10.dp)) {
            if (group.name != null) {
                Spacer(Modifier.widthIn(30.dp))
            }
            Column(Modifier.widthIn(min = 200.dp, max = 400.dp)) {
                Text(
                    modifier = Modifier.padding(vertical = 14.dp),
                    text = string(item.title),
                    style = MaterialTheme.typography.body2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (item.description != null) {
                    Text(
                        text = string(item.description),
                        style = MaterialTheme.typography.caption,
                        softWrap = true
                    )
                }
            }
            Spacer(Modifier.width(25.dp))
            Item(item, state)
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
    }
}

@Composable
fun SwitchItem(item: PreferencesItem.Switch, state: PreferencesEditorState) {
    PlainSwitch(
        checked = item.select(state.conf),
        onCheckedChange = { state.update(item, it) }
    )
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
        intValue = value,
        onValueChange = { state.update(item, it) },
        min = item.min,
        max = item.max
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
        floatValue = value,
        onValueChange = { state.update(item, it) },
        min = item.min,
        max = item.max
    )
}

@Composable
private fun ColorStringInputItem(item: PreferencesItem.ColorStringInput, state: PreferencesEditorState) {
    val currentValue = item.select(state.conf)

    fun getColor(text: String) = if (item.useAlpha) text.toColorOrNull() else text.toRgbColorOrNull()

    var value by remember {
        if (getColor(currentValue) == null) {
            mutableStateOf(item.defaultValue)
        } else {
            mutableStateOf(currentValue)
        }
    }

    LaunchedEffect(currentValue) {
        val inputColor = getColor(currentValue)
        if (inputColor != null && inputColor != getColor(value)) {
            value = currentValue
        }
    }

    var colorPreview by remember(currentValue) { mutableStateOf(getColor(currentValue)) }
    val valueLength = if (item.useAlpha) 9 else 7

    InputBox(
        value = value,
        onValueChange = { newValue ->
            var sanitizedValue = newValue
            if (sanitizedValue.firstOrNull() != '#') {
                sanitizedValue = "#" + sanitizedValue.replace("#", "")
            }
            sanitizedValue = sanitizedValue.filter { it.isHexChar || it == '#' }
            sanitizedValue = sanitizedValue.take(valueLength)
            value = sanitizedValue
            getColor(value)?.let {
                colorPreview = it
                val hexString = if (item.useAlpha) it.argbHexString else it.rgbHexString
                state.update(item, hexString)
            }
        },
        leadingContent = {
            Box(Modifier.size(20.dp).background(color = colorPreview ?: item.defaultValue.toColor()))
            Spacer(Modifier.width(15.dp))
        }
    )
}

@Composable
private fun <T> SelectionItem(item: PreferencesItem.Selection<T>, state: PreferencesEditorState) {
    val currentValue = item.select(state.conf)
    SelectionBox(
        value = currentValue,
        onSelect = { state.update(item, it) },
        options = item.options.toList(),
        getText = { value ->
            when (value) {
                is LocalizedText -> value.getText()
                else -> value.toString()
            }
        }
    )
}

@Composable
private fun ButtonBar(
    resetPage: () -> Unit,
    resetAll: () -> Unit,
    cancel: () -> Unit,
    canApply: Boolean,
    apply: () -> Unit,
    finish: () -> Unit
) {
    var settingsExpanded by remember { mutableStateOf(false) }
    Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.End) {
        Box {
            IconButton(onClick = { settingsExpanded = true }) {
                Icon(Icons.Default.Settings, null)
            }
            DropdownMenu(
                expanded = settingsExpanded,
                onDismissRequest = { settingsExpanded = false }
            ) {
                DropdownMenuItem(
                    onClick = {
                        resetPage()
                        settingsExpanded = false
                    }
                ) {
                    Text(text = string(Strings.PreferencesEditorResetPage))
                }
                DropdownMenuItem(
                    onClick = {
                        resetAll()
                        settingsExpanded = false
                    }
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
            onClick = { apply() }
        ) {
            Text(string(Strings.CommonApply))
        }
        Spacer(Modifier.width(25.dp))
        Button(
            onClick = { finish() }
        ) {
            Text(string(Strings.CommonOkay))
        }
    }
}
