package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import java.io.File

sealed class AskIfSaveDialogPurpose(
    val stringKey: Strings,
    val action: AppState.PendingActionAfterSaved,
) : EmbeddedDialogArgs {
    object IsOpening :
        AskIfSaveDialogPurpose(Strings.AskIfSaveBeforeOpenDialogDescription, AppState.PendingActionAfterSaved.Open)

    class IsOpeningCertain(val file: File) :
        AskIfSaveDialogPurpose(
            Strings.AskIfSaveBeforeOpenDialogDescription,
            AppState.PendingActionAfterSaved.OpenCertain(file),
        )

    object IsExporting :
        AskIfSaveDialogPurpose(Strings.AskIfSaveBeforeExportDialogDescription, AppState.PendingActionAfterSaved.Export)

    object IsExportingOverwrite :
        AskIfSaveDialogPurpose(
            Strings.AskIfSaveBeforeExportDialogDescription,
            AppState.PendingActionAfterSaved.ExportOverwrite(all = false),
        )

    object IsExportingOverwriteAll :
        AskIfSaveDialogPurpose(
            Strings.AskIfSaveBeforeExportDialogDescription,
            AppState.PendingActionAfterSaved.ExportOverwrite(all = true),
        )

    object IsClosing :
        AskIfSaveDialogPurpose(Strings.AskIfSaveBeforeCloseDialogDescription, AppState.PendingActionAfterSaved.Close)

    object IsCreatingNew : AskIfSaveDialogPurpose(
        Strings.AskIfSaveBeforeCloseDialogDescription,
        AppState.PendingActionAfterSaved.CreatingNew,
    )

    object IsClearingCaches : AskIfSaveDialogPurpose(
        Strings.AskIfSaveBeforeCloseDialogDescription,
        AppState.PendingActionAfterSaved.ClearCaches,
    )

    object IsExiting :
        AskIfSaveDialogPurpose(Strings.AskIfSaveBeforeExitDialogDescription, AppState.PendingActionAfterSaved.Exit)
}

data class AskIfSaveDialogResult(
    val save: Boolean,
    val actionAfterSaved: AppState.PendingActionAfterSaved,
) : EmbeddedDialogResult<AskIfSaveDialogPurpose>

@Composable
fun AskIfSaveDialog(
    args: AskIfSaveDialogPurpose,
    finish: (AskIfSaveDialogResult?) -> Unit,
) {
    val dismiss = { finish(null) }
    val submitYes = { finish(AskIfSaveDialogResult(true, args.action)) }
    val submitNo = { finish(AskIfSaveDialogResult(false, args.action)) }

    Column {
        Spacer(Modifier.height(15.dp))
        Text(
            text = string(args.stringKey),
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(25.dp))
        Row(modifier = Modifier.align(Alignment.End), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = { dismiss() },
            ) {
                Text(string(Strings.CommonCancel))
            }
            Spacer(Modifier.width(25.dp))
            TextButton(
                onClick = { submitNo() },
            ) {
                Text(string(Strings.CommonNo))
            }
            Spacer(Modifier.width(25.dp))
            Button(
                onClick = { submitYes() },
            ) {
                Text(string(Strings.CommonYes))
            }
        }
    }
}

@Composable
@Preview
private fun Preview() = AskIfSaveDialog(AskIfSaveDialogPurpose.IsClosing) {}
