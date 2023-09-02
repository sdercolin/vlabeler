package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string

data class EditEntryExtraDialogArgs(
    val index: Int,
    val initial: List<String?>,
    val extraFields: List<LabelerConf.ExtraField>,
) : EmbeddedDialogArgs

data class EditEntryExtraDialogResult(
    val index: Int,
    val extras: List<String?>,
) : EmbeddedDialogResult<EditEntryExtraDialogArgs>

class EditEntryExtraState(args: EditEntryExtraDialogArgs) {
    var values by mutableStateOf(args.initial)
    val extraFields = args.extraFields

    fun update(index: Int, value: String?) {
        val newValues = values.toMutableList()
        newValues[index] = value
        values = newValues.toList()
    }

    val isValid: Boolean
        get() = values
            .mapIndexed { index, s -> extraFields[index].isOptional || s != null }
            .all { it }
}

@Composable
fun EditEntryExtraDialog (
    args: EditEntryExtraDialogArgs,
    finish: (EditEntryExtraDialogResult?) -> Unit,
) {
    val dismiss = { finish(null) }
    val submit = { extra: List<String?> ->
        finish(EditEntryExtraDialogResult(args.index, extra))
    }
    val state = remember {
        EditEntryExtraState(args)
    }

    Column(modifier = Modifier.fillMaxWidth(0.5f).fillMaxHeight(0.5f)) {
        Spacer(Modifier.height(20.dp))
        Text(
            text = string(Strings.EditEntryExtraDialogDescription),
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(25.dp))
        ExtraContent(state)
        Spacer(Modifier.height(25.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = dismiss,
            ) {
                Text(string(Strings.CommonCancel))
            }
            Spacer(Modifier.width(25.dp))
            ConfirmButton(
                enabled = state.isValid,
                onClick = { submit(state.values) },
            )
        }
    }
}

@Composable
private fun ColumnScope.ExtraContent(
    state: EditEntryExtraState,
) {
    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
        val scrollState = rememberScrollState()
        Column(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
            state.extraFields.forEachIndexed { index, extraField ->
                if (extraField.isVisible) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = extraField.name,
                            style = MaterialTheme.typography.body2,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.width(25.dp))
                        TextField(
                            modifier = Modifier.weight(1f),
                            value = state.values[index] ?: "",
                            onValueChange = { state.update(index, it.ifBlank { null }) },
                            singleLine = true,
                            isError = !extraField.isOptional && state.values[index].isNullOrBlank(),
                            enabled = extraField.isEditable,
                        )
                    }
                }
            }
        }

        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterVertically).width(15.dp),
        )
    }
}
