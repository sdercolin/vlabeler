package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.filter.EntryFilter
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.DoneTriStateIcon
import com.sdercolin.vlabeler.ui.common.StarTriStateIcon
import com.sdercolin.vlabeler.ui.common.ToggleButtonGroup
import com.sdercolin.vlabeler.ui.common.WithTooltip
import com.sdercolin.vlabeler.ui.string.*

data class EntryFilterSetterDialogArgs(
    val value: EntryFilter,
) : EmbeddedDialogArgs

data class EntryFilterSetterDialogResult(
    val value: EntryFilter,
) : EmbeddedDialogResult<EntryFilterSetterDialogArgs>

@Composable
fun EntryFilterSetterDialog(
    args: EntryFilterSetterDialogArgs,
    finish: (EntryFilterSetterDialogResult?) -> Unit,
) {
    var value by remember { mutableStateOf(args.value.parse()) }
    val dismiss = { finish(null) }
    val submit = { finish(EntryFilterSetterDialogResult(value.toEntryFilter())) }
    Column(modifier = Modifier.fillMaxWidth(0.6f)) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = string(Strings.EntryFilterSetterDialogTitle),
            style = MaterialTheme.typography.h5,
        )
        Spacer(modifier = Modifier.height(25.dp))
        Content(
            value = value,
            setValue = { value = it },
        )
        Spacer(modifier = Modifier.height(25.dp))
        ButtonBar(submit, dismiss)
    }
}

private val HEADER_WIDTH = 200.dp

@Composable
private fun Content(value: EntryFilter.Args, setValue: (EntryFilter.Args) -> Unit) {
    TextItemRow(
        headerText = string(Strings.EntryFilterSetterDialogHeaderAny),
        value = value.any ?: "",
        setValue = { setValue(value.copy(any = it.ifEmpty { null })) },
    )
    TextItemRow(
        headerText = string(Strings.EntryFilterSetterDialogHeaderName),
        value = value.name ?: "",
        setValue = { setValue(value.copy(name = it.ifEmpty { null })) },
    )
    TextItemRow(
        headerText = string(Strings.EntryFilterSetterDialogHeaderSample),
        value = value.sample ?: "",
        setValue = { setValue(value.copy(sample = it.ifEmpty { null })) },
    )
    TextItemRow(
        headerText = string(Strings.EntryFilterSetterDialogHeaderTag),
        value = value.tag ?: "",
        setValue = { setValue(value.copy(tag = it.ifEmpty { null })) },
    )
    ItemRow(
        headerText = string(Strings.EntryFilterSetterDialogHeaderDone),
        hasValue = value.done != null,
    ) {
        ToggleButtonGroup(
            selected = value.done,
            options = listOf(null, false, true),
            onSelectedChange = { setValue(value.copy(done = it)) },
            buttonContent = {
                WithTooltip(
                    string(
                        when (it) {
                            true -> Strings.FilterDone
                            false -> Strings.FilterUndone
                            null -> Strings.FilterDoneIgnored
                        },
                    ),
                ) {
                    DoneTriStateIcon(it, Modifier.padding(12.dp))
                }
            },
        )
    }
    ItemRow(
        headerText = string(Strings.EntryFilterSetterDialogHeaderStar),
        hasValue = value.star != null,
    ) {
        ToggleButtonGroup(
            selected = value.star,
            options = listOf(null, false, true),
            onSelectedChange = { setValue(value.copy(star = it)) },
            buttonContent = {
                WithTooltip(
                    string(
                        when (it) {
                            true -> Strings.FilterStarred
                            false -> Strings.FilterUnstarred
                            null -> Strings.FilterStarIgnored
                        },
                    ),
                ) {
                    StarTriStateIcon(it, Modifier.padding(12.dp))
                }
            },
        )
    }
}

@Composable
private fun TextItemRow(headerText: String, value: String, setValue: (String) -> Unit) {
    ItemRow(headerText, value.isNotEmpty()) {
        TextField(
            modifier = Modifier.weight(1f),
            value = value,
            onValueChange = setValue,
            singleLine = true,
        )
    }
}

@Composable
private fun ItemRow(headerText: String, hasValue: Boolean, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.padding(vertical = 10.dp).heightIn(min = 60.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ItemRowHeaderText(text = headerText, isActive = hasValue)
        content()
    }
}

@Composable
private fun ItemRowHeaderText(text: String, isActive: Boolean) {
    val alpha = if (isActive) 1f else 0.5f
    Text(
        text = text,
        modifier = Modifier.width(HEADER_WIDTH),
        style = MaterialTheme.typography.body2,
        fontWeight = FontWeight.Bold,
        color = LocalContentColor.current.copy(alpha = alpha),
    )
}

@Composable
private fun ButtonBar(submit: () -> Unit, dismiss: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = dismiss) {
            Text(text = string(Strings.CommonCancel))
        }
        Spacer(Modifier.width(25.dp))
        ConfirmButton(onClick = submit)
    }
}
