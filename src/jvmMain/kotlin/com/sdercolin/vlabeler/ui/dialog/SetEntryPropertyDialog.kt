package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string

data class SetEntryPropertyDialogArgs(
    val currentValue: Float,
    val propertyIndex: Int,
    val propertyDisplayedName: LocalizedJsonString,
) : EmbeddedDialogArgs

data class SetEntryPropertyDialogResult(
    val newValue: Float,
    val propertyIndex: Int,
) : EmbeddedDialogResult<SetEntryPropertyDialogArgs>

@Composable
fun SetEntryPropertyDialog(
    args: SetEntryPropertyDialogArgs,
    finish: (EmbeddedDialogResult<SetEntryPropertyDialogArgs>?) -> Unit,
) {
    val dismiss = { finish(null) }
    val submit = { newValue: Float -> finish(SetEntryPropertyDialogResult(newValue, args.propertyIndex)) }

    var input by remember {
        mutableStateOf(
            TextFieldValue(
                text = args.currentValue.toString(),
                selection = TextRange(0, args.currentValue.toString().length),
            ),
        )
    }
    var value by remember { mutableStateOf<Float?>(args.currentValue) }

    val submitIfValid: () -> Unit = remember { { input.text.toFloatOrNull()?.let { submit(it) } } }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth(0.75f),
    ) {
        Spacer(Modifier.height(15.dp))
        Text(
            text = string(Strings.SetEntryPropertyDialogDescription, args.propertyDisplayedName.get()),
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            modifier = Modifier.width(150.dp).focusRequester(focusRequester),
            value = input,
            singleLine = true,
            onValueChange = {
                input = it
                value = it.text.toFloatOrNull()
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { submitIfValid() },
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
                enabled = value != null,
                onClick = submitIfValid,
            )
        }
    }
}
