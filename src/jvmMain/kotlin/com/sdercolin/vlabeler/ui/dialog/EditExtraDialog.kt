package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.ui.common.CancelButton
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.HeaderFooterColumn
import com.sdercolin.vlabeler.ui.string.*

data class EditExtraDialogArgs(
    val index: Int,
    val initial: List<String?>,
    val extraFields: List<LabelerConf.ExtraField>,
    val target: EditExtraDialogTarget,
) : EmbeddedDialogArgs

enum class EditExtraDialogTarget(val stringKey: Strings) {
    EditEntry(Strings.EditEntryExtraDialogDescription),
    EditModule(Strings.EditModuleExtraDialogDescription),
}

data class EditExtraDialogResult(
    val index: Int,
    val extras: List<String?>,
    val target: EditExtraDialogTarget,
) : EmbeddedDialogResult<EditExtraDialogArgs>

class EditExtraState(
    args: EditExtraDialogArgs,
    private val finish: (EditExtraDialogResult?) -> Unit,
) {
    private val itemIndex = args.index
    var values by mutableStateOf(args.initial)
    val extraFields = args.extraFields
    val target = args.target

    fun getNotNull(index: Int): String {
        return values[index] ?: ""
    }

    fun update(index: Int, value: String?) {
        val newValues = values.toMutableList()
        newValues[index] = value
        values = newValues.toList()
    }

    fun isValid(index: Int): Boolean {
        return extraFields[index].isOptional || values[index].isNullOrBlank().not()
    }

    val isAllValid: Boolean
        get() = values
            .mapIndexed { index, s -> extraFields[index].isOptional || s != null }
            .all { it }

    fun cancel() {
        finish(null)
    }

    fun trySubmit() {
        if (isAllValid) {
            submit()
        }
    }

    fun submit() {
        finish(EditExtraDialogResult(itemIndex, values, target))
    }
}

@Composable
fun EditExtraDialog(
    args: EditExtraDialogArgs,
    finish: (EditExtraDialogResult?) -> Unit,
) {
    val state = remember {
        EditExtraState(args, finish)
    }
    HeaderFooterColumn(
        modifier = Modifier.fillMaxWidth(0.6f).padding(top = 20.dp),
        header = {
            Text(
                text = string(args.target.stringKey),
                style = MaterialTheme.typography.h5,
            )
            Spacer(Modifier.height(25.dp))
        },
        footer = {
            Spacer(Modifier.height(25.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                CancelButton(state::cancel)
                Spacer(Modifier.width(25.dp))
                ConfirmButton(
                    enabled = state.isAllValid,
                    onClick = state::trySubmit,
                )
            }
        },
    ) {
        Box(modifier = Modifier.heightIn(min = 300.dp)) {
            Content(state)
        }
    }
}

@Composable
private fun Content(
    state: EditExtraState,
) {
    state.extraFields.forEachIndexed { index, extraField ->
        if (extraField.isVisible) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    modifier = Modifier.width(200.dp),
                    text = extraField.displayedName.get(),
                    style = MaterialTheme.typography.body2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.width(25.dp))
                TextField(
                    modifier = Modifier.weight(1f),
                    value = state.getNotNull(index),
                    onValueChange = { state.update(index, it.ifBlank { null }) },
                    singleLine = true,
                    isError = state.isValid(index).not(),
                    enabled = extraField.isEditable,
                )
            }
        }
    }
}
