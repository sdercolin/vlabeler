package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.NewLabel
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.EntryListDiff
import com.sdercolin.vlabeler.model.EntryListDiffItem
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.DoneIcon
import com.sdercolin.vlabeler.ui.common.FreeSizedIconButton
import com.sdercolin.vlabeler.ui.common.LargeDialogContainer
import com.sdercolin.vlabeler.ui.common.NavigatorItemSummary
import com.sdercolin.vlabeler.ui.common.NavigatorListItemNumber
import com.sdercolin.vlabeler.ui.common.StarIcon
import com.sdercolin.vlabeler.ui.common.WithTooltip
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.ui.theme.White20
import com.sdercolin.vlabeler.ui.theme.White80
import com.sdercolin.vlabeler.ui.theme.getSwitchColors
import com.sdercolin.vlabeler.util.alpha
import com.sdercolin.vlabeler.util.toColor

data class ReloadLabelDialogArgs(
    val moduleName: String,
    val entries: List<Entry>,
    val diff: EntryListDiff,
)

data class ReloadLabelConfigs(
    val inheritStar: Boolean = true,
    val inheritDone: Boolean = true,
    val inheritTag: Boolean = true,
)

@Composable
fun ReloadLabelDialog(
    args: ReloadLabelDialogArgs,
    finish: (ReloadLabelConfigs?) -> Unit,
) {
    LargeDialogContainer {
        Column(modifier = Modifier.fillMaxSize().padding(vertical = 20.dp, horizontal = 45.dp)) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = string(Strings.ReloadLabelDialogTitle),
                style = MaterialTheme.typography.h5,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = string(Strings.ReloadLabelDialogNotice),
                style = MaterialTheme.typography.caption,
                maxLines = 5,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(25.dp))
            Content(moduleName = args.moduleName, diff = args.diff)
            Spacer(modifier = Modifier.height(25.dp))
            ButtonBar(finish)
        }
    }
}

@Composable
fun ColumnScope.Content(moduleName: String, diff: EntryListDiff) {
    var showUnchanged by remember { mutableStateOf(false) }
    val items = remember(showUnchanged, diff) {
        if (showUnchanged) {
            diff.items
        } else {
            diff.items.filter { it !is EntryListDiffItem.Unchanged }
        }
    }
    Column(modifier = Modifier.fillMaxWidth().weight(1f)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (moduleName.isNotBlank()) {
                Text(
                    text = string(Strings.ReloadLabelDialogModuleNameTemplate, moduleName),
                    style = MaterialTheme.typography.body1.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(string(Strings.ReloadLabelDialogShowUnchanged))
            Spacer(Modifier.width(10.dp))
            Switch(
                checked = showUnchanged,
                onCheckedChange = { showUnchanged = it },
                colors = getSwitchColors(),
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier.fillMaxWidth()
                .weight(1f)
                .background(color = MaterialTheme.colors.background),
        ) {
            val listState = rememberLazyListState()
            LazyColumn(modifier = Modifier.fillMaxSize(), state = listState) {
                items(items) { item ->
                    EntryDiffItem(item)
                    Spacer(Modifier.height(1.dp))
                }
            }
            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(listState),
                modifier = Modifier.align(Alignment.CenterEnd).width(15.dp),
            )
            if (items.isEmpty()) {
                Text(
                    text = string(Strings.ReloadLabelDialogNoDiff),
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }
    }
}

@Composable
private fun EntryDiffItem(item: EntryListDiffItem) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max)) {
        when (item) {
            is EntryListDiffItem.Edit -> {
                val color = "#d67c0d".toColor().alpha(0.3f)
                EntryItemSummary(item.oldIndex, item.old, color)
                Spacer(Modifier.width(1.dp))
                EntryItemSummary(item.newIndex, item.new, color)
            }
            is EntryListDiffItem.Add -> {
                EmptyEntryItem()
                Spacer(Modifier.width(1.dp))
                EntryItemSummary(item.newIndex, item.new, "#1cd10f".toColor().alpha(0.3f))
            }
            is EntryListDiffItem.Remove -> {
                EntryItemSummary(item.oldIndex, item.old, "#e82b09".toColor().alpha(0.3f))
                Spacer(Modifier.width(1.dp))
                EmptyEntryItem()
            }
            is EntryListDiffItem.Unchanged -> {
                EntryItemSummary(item.oldIndex, item.old, White20)
                Spacer(Modifier.width(1.dp))
                EntryItemSummary(item.newIndex, item.new, White20)
            }
        }
    }
}

@Composable
private fun RowScope.EntryItemSummary(index: Int, entry: Entry, color: Color) {
    Row(modifier = Modifier.weight(1f).fillMaxHeight().background(color = color).padding(5.dp)) {
        NavigatorListItemNumber(index)
        Column(Modifier.fillMaxWidth()) {
            NavigatorItemSummary(entry.name, entry.sample, hideSampleExtension = false, isEntry = true)
            Spacer(Modifier.height(5.dp))
            Text(
                text = "Start: ${entry.start}, End: ${entry.end}",
                style = MaterialTheme.typography.caption,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (entry.points.isNotEmpty()) {
                Text(
                    text = "Points: ${entry.points}",
                    style = MaterialTheme.typography.caption,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.extras.isNotEmpty()) {
                Text(
                    text = "Extras: ${entry.extras}",
                    style = MaterialTheme.typography.caption,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (entry.notes.star || entry.notes.done || entry.notes.tag.isNotBlank()) {
                Spacer(Modifier.height(5.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (entry.notes.star) {
                        StarIcon(star = true, modifier = Modifier.size(16.dp))
                    }
                    if (entry.notes.done) {
                        DoneIcon(done = true, modifier = Modifier.size(16.dp))
                    }
                    if (entry.notes.tag.isNotBlank()) {
                        Text(
                            modifier = Modifier.background(color = White20).padding(horizontal = 5.dp, vertical = 2.dp),
                            text = entry.notes.tag,
                            style = MaterialTheme.typography.caption,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.height(5.dp))
        }
    }
}

@Composable
private fun RowScope.EmptyEntryItem() {
    Box(modifier = Modifier.weight(1f).height(IntrinsicSize.Max))
}

@Composable
private fun ButtonBar(finish: (ReloadLabelConfigs?) -> Unit) {
    var inheritStar by remember { mutableStateOf(true) }
    var inheritDone by remember { mutableStateOf(true) }
    var inheritTag by remember { mutableStateOf(true) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = string(Strings.ReloadLabelDialogInheritNotes),
                style = MaterialTheme.typography.body2,
            )
            Spacer(Modifier.width(5.dp))
            WithTooltip(string(Strings.ReloadLabelDialogInheritNotesDescription)) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colors.onSurface,
                )
            }
            Spacer(Modifier.width(10.dp))
            FreeSizedIconButton(
                onClick = { inheritStar = !inheritStar },
                modifier = Modifier.padding(4.dp),
            ) {
                StarIcon(inheritStar, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(5.dp))
            FreeSizedIconButton(
                onClick = { inheritDone = !inheritDone },
                modifier = Modifier.padding(4.dp),
            ) {
                DoneIcon(inheritDone, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(5.dp))
            FreeSizedIconButton(
                onClick = { inheritTag = !inheritTag },
                modifier = Modifier.padding(4.dp),
            ) {
                Icon(
                    modifier = Modifier.size(20.dp),
                    imageVector = Icons.Default.NewLabel,
                    contentDescription = null,
                    tint = if (inheritTag) White80 else White20,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = { finish(null) }) {
            Text(text = string(Strings.CommonCancel))
        }
        Spacer(Modifier.width(25.dp))
        ConfirmButton(
            onClick = {
                finish(ReloadLabelConfigs(inheritStar, inheritDone, inheritTag))
            },
        )
    }
}
