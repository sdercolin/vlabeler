package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string

@Composable
fun SetResolutionDialog(
    current: Int,
    min: Int,
    max: Int,
    submit: (Int) -> Unit,
    dismiss: () -> Unit
) {
    Dialog(
        onCloseRequest = { dismiss() },
        state = DialogState(width = 400.dp, height = 250.dp),
        undecorated = true
    ) {
        Content(current, min, max, submit, dismiss)
    }
}

@Composable
private fun Content(
    current: Int,
    min: Int,
    max: Int,
    submit: (Int) -> Unit,
    dismiss: () -> Unit
) {
    var input by remember { mutableStateOf(current.toString()) }
    var value by remember { mutableStateOf<Int?>(current) }
    Surface {
        Box(Modifier.fillMaxSize()) {
            Column(
                Modifier.padding(horizontal = 50.dp, vertical = 15.dp)
                    .align(Alignment.Center)
            ) {
                Spacer(Modifier.height(15.dp))
                Text(
                    text = string(Strings.SetResolutionDialogDescription, min, max),
                    style = MaterialTheme.typography.body2,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    modifier = Modifier.width(150.dp),
                    value = input,
                    singleLine = true,
                    onValueChange = {
                        input = it
                        val intValue = it.toIntOrNull()
                        value = if (intValue != null && intValue in min..max) {
                            intValue
                        } else {
                            null
                        }
                    }
                )
                Spacer(Modifier.height(25.dp))
                Row(modifier = Modifier.align(Alignment.End), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = { dismiss() }
                    ) {
                        Text(string(Strings.CommonDialogCancelButton))
                    }
                    Spacer(Modifier.width(25.dp))
                    Button(
                        enabled = value != null,
                        onClick = { input.toIntOrNull()?.let { submit(it) } }
                    ) {
                        Text(string(Strings.CommonDialogConfirmButton))
                    }
                }
            }
        }
    }
}

@Composable
@Preview
private fun Preview() = Content(current = 100, min = 10, max = 1000, submit = {}, dismiss = {})