package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string

enum class ErrorDialogContent(val stringKey: Strings) : EmbeddedDialogArgs {
    FailedToParseProject(Strings.FailedToParseProjectError)
}

object ErrorDialogResult : EmbeddedDialogResult<ErrorDialogContent>

@Composable
fun ErrorDialog(
    content: ErrorDialogContent,
    finish: (ErrorDialogResult) -> Unit,
) {
    val submit = { finish(ErrorDialogResult) }

    Column(Modifier.widthIn(min = 350.dp)) {
        Spacer(Modifier.height(15.dp))
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = null,
            tint = MaterialTheme.colors.error,
            modifier = Modifier.size(50.dp).align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(15.dp))
        Text(
            text = string(content.stringKey),
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(25.dp))
        Row(modifier = Modifier.align(Alignment.End), horizontalArrangement = Arrangement.End) {
            Button(onClick = submit) {
                Text(string(Strings.CommonOkay))
            }
        }
    }
}

@Composable
@Preview
private fun Preview() = ErrorDialog(ErrorDialogContent.FailedToParseProject) {}
