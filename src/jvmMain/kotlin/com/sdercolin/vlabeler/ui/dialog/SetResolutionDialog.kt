package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import com.sdercolin.vlabeler.ui.string.*

data class SetResolutionDialogArgs(val current: Int, val min: Int, val max: Int) : EmbeddedDialogArgs

data class SetResolutionDialogResult(val newValue: Int) : EmbeddedDialogResult<SetResolutionDialogArgs>

@Composable
fun SetResolutionDialog(
    args: SetResolutionDialogArgs,
    finish: (EmbeddedDialogResult<SetResolutionDialogArgs>?) -> Unit,
) {
    val dismiss = { finish(null) }
    val submit = { newValue: Int -> finish(SetResolutionDialogResult(newValue)) }

    var input by remember {
        mutableStateOf(
            TextFieldValue(args.current.toString(), selection = TextRange(0, args.current.toString().length)),
        )
    }
    var value by remember { mutableStateOf<Int?>(args.current) }

    val submitIfValid: () -> Unit = remember { { input.text.toIntOrNull()?.let { submit(it) } } }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column {
        Spacer(Modifier.height(15.dp))
        Text(
            text = string(Strings.SetResolutionDialogDescription, args.min, args.max),
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
                val intValue = it.text.toIntOrNull()
                value = if (intValue != null && intValue in args.min..args.max) {
                    intValue
                } else {
                    null
                }
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

@Composable
@Preview
private fun Preview() = SetResolutionDialog(SetResolutionDialogArgs(100, 10, 1000)) {}
