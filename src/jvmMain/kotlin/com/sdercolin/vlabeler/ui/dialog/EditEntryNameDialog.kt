package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string

data class EditEntryNameDialogArgs(
    val sampleName: String,
    val index: Int,
    val initial: String,
    val invalidOptions: List<String>,
    val showSnackbar: (String) -> Unit,
    val duplicate: Boolean
) : EmbeddedDialogArgs

data class EditEntryNameDialogResult(
    val sampleName: String,
    val index: Int,
    val name: String,
    val duplicate: Boolean
) : EmbeddedDialogResult

@Composable
fun EditEntryNameDialog(
    args: EditEntryNameDialogArgs,
    finish: (EditEntryNameDialogResult?) -> Unit,
) {
    val dismiss = { finish(null) }
    val submit = { name: String ->
        finish(EditEntryNameDialogResult(args.sampleName, args.index, name, args.duplicate))
    }

    var input by remember { mutableStateOf(args.initial) }

    Column(Modifier.widthIn(min = 350.dp)) {
        Spacer(Modifier.height(15.dp))
        Text(
            text = string(
                if (args.duplicate) {
                    Strings.EditEntryNameDuplicateDialogDescription
                } else {
                    Strings.EditEntryNameDialogDescription
                }
            ),
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            modifier = Modifier.width(150.dp),
            value = input,
            singleLine = true,
            isError = args.invalidOptions.contains(input),
            onValueChange = { input = it }
        )
        Spacer(Modifier.height(25.dp))
        Row(modifier = Modifier.align(Alignment.End), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = { dismiss() }
            ) {
                Text(string(Strings.CommonCancel))
            }
            Spacer(Modifier.width(25.dp))
            Button(
                onClick = {
                    if (args.invalidOptions.contains(input)) {
                        args.showSnackbar(string(Strings.EditEntryNameDialogExistingError))
                    } else {
                        submit(input)
                    }
                }
            ) {
                Text(string(Strings.CommonOkay))
            }
        }
    }
}
