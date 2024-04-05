package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.NavigatorItemSummary
import com.sdercolin.vlabeler.ui.common.NavigatorListItemNumber
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.util.runIf

data class MoveEntryDialogArgs(val currentIndex: Int, val entries: List<Entry>, val viewConf: AppConf.View) :
    EmbeddedDialogArgs

data class MoveEntryDialogResult(val oldIndex: Int, val newIndex: Int) : EmbeddedDialogResult<MoveEntryDialogArgs>

@Composable
fun MoveEntryDialog(
    args: MoveEntryDialogArgs,
    finish: (EmbeddedDialogResult<MoveEntryDialogArgs>?) -> Unit,
) {
    val dismiss = { finish(null) }
    val submit = { newIndex: Int -> finish(MoveEntryDialogResult(args.currentIndex, newIndex)) }

    var input by remember {
        val text = args.currentIndex.inc().toString()
        mutableStateOf(
            TextFieldValue(text, selection = TextRange(0, text.length)),
        )
    }
    var validIndex by remember { mutableStateOf(args.currentIndex) }
    var index by remember { mutableStateOf<Int?>(args.currentIndex) }

    fun getValidNewIndex(): Int? {
        val newIndex = input.text.toIntOrNull()?.dec()
        return if (newIndex != null && newIndex in args.entries.indices) {
            newIndex
        } else {
            null
        }
    }

    fun submitIfValid() {
        index?.let { submit(it) }
    }

    fun updateInput(newInput: TextFieldValue) {
        input = newInput
        index = getValidNewIndex()
        index?.let { validIndex = it }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Column(modifier = Modifier.widthIn(min = 400.dp)) {
        Spacer(Modifier.height(15.dp))
        Text(
            text = string(
                Strings.MoveEntryDialogDescription,
                args.entries[args.currentIndex].name,
                1,
                args.entries.size,
            ),
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                modifier = Modifier.width(150.dp).focusRequester(focusRequester),
                value = input,
                singleLine = true,
                onValueChange = { updateInput(it) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { submitIfValid() },
                ),
            )
            IconButton(
                enabled = validIndex > 0,
                onClick = { updateInput(TextFieldValue(validIndex.inc().dec().toString())) },
            ) {
                Icon(Icons.Default.Remove, null)
            }
            IconButton(
                enabled = validIndex < args.entries.lastIndex,
                onClick = { updateInput(TextFieldValue(validIndex.inc().inc().toString())) },
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
        Spacer(Modifier.height(20.dp))
        ResultView(args.entries, args.currentIndex, validIndex, args.viewConf)
        Spacer(Modifier.height(25.dp))
        Row(modifier = Modifier.align(Alignment.End), horizontalArrangement = Arrangement.End) {
            TextButton(
                onClick = { dismiss() },
            ) {
                Text(string(Strings.CommonCancel))
            }
            Spacer(Modifier.width(25.dp))
            ConfirmButton(
                enabled = index != null,
                onClick = ::submitIfValid,
            )
        }
    }
}

private const val RESULT_ENTRY_INDEX_RADIUS = 2

private data class EntryForResultView(val entry: Entry, val oldIndex: Int, val newIndex: Int)

@Composable
private fun ResultView(entries: List<Entry>, oldIndex: Int, newIndex: Int, viewConf: AppConf.View) {
    val entriesToShow = remember(entries, oldIndex, newIndex) {
        val list = entries.withIndex().toMutableList()
        val movedEntry = list.removeAt(oldIndex)
        list.add(newIndex, movedEntry)
        var startIndex = (newIndex - RESULT_ENTRY_INDEX_RADIUS).coerceAtLeast(0)
        val endIndex = (startIndex + 2 * RESULT_ENTRY_INDEX_RADIUS + 1).coerceAtMost(entries.size)
        if (endIndex - startIndex < 2 * RESULT_ENTRY_INDEX_RADIUS + 1) {
            startIndex = (endIndex - 2 * RESULT_ENTRY_INDEX_RADIUS - 1).coerceAtLeast(0)
        }
        list.map { EntryForResultView(it.value, it.index, list.indexOf(it)) }.subList(startIndex, endIndex)
    }
    Column(modifier = Modifier.width(IntrinsicSize.Max)) {
        entriesToShow.forEach { entry ->
            Row(
                modifier = Modifier.fillMaxWidth()
                    .height(30.dp)
                    .runIf(entry.oldIndex == oldIndex) {
                        background(color = MaterialTheme.colors.primaryVariant)
                    }
                    .padding(end = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NavigatorListItemNumber(entry.newIndex)
                NavigatorItemSummary(entry.entry.name, entry.entry.sample, viewConf.hideSampleExtension, isEntry = true)
            }
        }
    }
}
