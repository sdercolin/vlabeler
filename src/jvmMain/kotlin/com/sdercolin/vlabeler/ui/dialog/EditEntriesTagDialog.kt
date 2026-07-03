package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.util.removeControlCharacters

data class EditEntriesTagDialogArgs(
    val indexes: List<Int>,
    val initial: String,
) : EmbeddedDialogArgs

data class EditEntriesTagDialogResult(
    val indexes: List<Int>,
    val tag: String,
) : EmbeddedDialogResult<EditEntriesTagDialogArgs>

@Composable
fun EditEntriesTagDialog(
    args: EditEntriesTagDialogArgs,
    finish: (EditEntriesTagDialogResult?) -> Unit,
) {
    val dismiss = { finish(null) }
    val submit = { tag: String ->
        finish(EditEntriesTagDialogResult(args.indexes, tag))
    }

    var input by remember {
        mutableStateOf(
            TextFieldValue(args.initial, selection = TextRange(0, args.initial.length)),
        )
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(Modifier.widthIn(min = 350.dp)) {
        Spacer(Modifier.height(15.dp))
        Text(
            text = string(Strings.EditEntriesTagDialogDescription, args.indexes.size),
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            modifier = Modifier.width(150.dp).focusRequester(focusRequester),
            value = input,
            singleLine = true,
            onValueChange = { input = it.copy(text = it.text.removeControlCharacters()) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { submit(input.text) },
            ),
        )
        Spacer(Modifier.height(25.dp))
        Row(modifier = Modifier.align(Alignment.End), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = { dismiss() },
            ) {
                Text(string(Strings.CommonCancel))
            }
            Spacer(Modifier.width(25.dp))
            ConfirmButton(
                onClick = { submit(input.text) },
            )
        }
    }
}
