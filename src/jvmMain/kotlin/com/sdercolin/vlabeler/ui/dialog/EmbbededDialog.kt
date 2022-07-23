package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.theme.Black50
import com.sdercolin.vlabeler.util.runIf

data class EmbeddedDialogRequest<T : EmbeddedDialogArgs>(val args: T, val onResult: (EmbeddedDialogResult<T>?) -> Unit)

sealed interface EmbeddedDialogArgs {
    val customMargin: Boolean get() = false
    val cancellableOnClickOutside: Boolean get() = false
}

interface EmbeddedDialogResult<T : EmbeddedDialogArgs>

@Composable
fun <T : EmbeddedDialogArgs> EmbeddedDialog(request: EmbeddedDialogRequest<T>) {
    val args = request.args
    Box(
        modifier = Modifier.fillMaxSize()
            .background(color = Black50)
            .plainClickable { if (args.cancellableOnClickOutside) request.onResult(null) },
        contentAlignment = Alignment.Center
    ) {
        Surface {
            Box(
                modifier = Modifier
                    .runIf<Modifier>(!args.customMargin) { padding(horizontal = 50.dp, vertical = 20.dp) }
            ) {
                TypedDialog(args, request)
            }
        }
    }
}

@Composable
private fun <T : EmbeddedDialogArgs> TypedDialog(
    args: T,
    request: EmbeddedDialogRequest<T>
) {
    @Suppress("UNCHECKED_CAST")
    when (args) {
        is SetResolutionDialogArgs ->
            SetResolutionDialog(args, (request as EmbeddedDialogRequest<SetResolutionDialogArgs>).onResult)
        is AskIfSaveDialogPurpose ->
            AskIfSaveDialog(args, (request as EmbeddedDialogRequest<AskIfSaveDialogPurpose>).onResult)
        is JumpToEntryDialogArgs ->
            JumpToEntryDialog(args, (request as EmbeddedDialogRequest<JumpToEntryDialogArgs>).onResult)
        is InputEntryNameDialogArgs ->
            InputEntryNameDialog(args, (request as EmbeddedDialogRequest<InputEntryNameDialogArgs>).onResult)
        is CommonConfirmationDialogAction ->
            CommonConfirmationDialog(args, (request as EmbeddedDialogRequest<CommonConfirmationDialogAction>).onResult)
        is ErrorDialogContent ->
            ErrorDialog(args, (request as EmbeddedDialogRequest<ErrorDialogContent>).onResult)
        is PreferencesDialogArgs ->
            PreferencesDialog(args, (request as EmbeddedDialogRequest<PreferencesDialogArgs>).onResult)
        else -> throw NotImplementedError("Dialog args handler is not implemented")
    }
}
