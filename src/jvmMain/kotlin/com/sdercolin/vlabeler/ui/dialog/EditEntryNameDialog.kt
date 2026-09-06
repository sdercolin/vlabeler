package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sdercolin.vlabeler.model.PhonemizerRunner
import com.sdercolin.vlabeler.model.Plugin
import com.sdercolin.vlabeler.model.key.Key
import com.sdercolin.vlabeler.model.key.KeySet
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.ui.theme.DarkGray
import com.sdercolin.vlabeler.ui.theme.White20
import com.sdercolin.vlabeler.util.removeControlCharacters

data class InputEntryNameDialogArgs(
    val index: Int,
    val initial: String,
    val invalidOptions: List<String>,
    val showSnackbar: (String) -> Unit,
    val purpose: InputEntryNameDialogPurpose,
    val presets: List<String> = listOf(),
    val phonemizerPlugins: List<Plugin> = emptyList(),
    val nextKeySet: KeySet? = KeySet(Key.Tab),
    val previousKeySet: KeySet? = KeySet(Key.Tab, setOf(Key.Shift)),
) : EmbeddedDialogArgs

enum class InputEntryNameDialogPurpose(val stringKey: Strings) {
    Rename(Strings.InputEntryNameDialogDescription),
    Duplicate(Strings.InputEntryNameDuplicateDialogDescription),
    CutFormer(Strings.InputEntryNameCutFormerDialogDescription),
    CutLatter(Strings.InputEntryNameCutLatterDialogDescription),
}

enum class NavigationDirection {
    Next,
    Previous,
}

data class InputEntryNameDialogResult(
    val index: Int,
    val name: String,
    val purpose: InputEntryNameDialogPurpose,
    val navigation: NavigationDirection? = null,
    val extraNames: List<String> = emptyList(),
) : EmbeddedDialogResult<InputEntryNameDialogArgs>

object InputEntryNameDialogState {
    var phonemizeMode: Boolean = false
    var selectedPluginName: String? = null
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InputEntryNameDialog(
    args: InputEntryNameDialogArgs,
    finish: (InputEntryNameDialogResult?) -> Unit,
) {
    val dismiss = { finish(null) }

    var input by remember(args.index, args.initial) {
        mutableStateOf(TextFieldValue(args.initial, selection = TextRange(0, args.initial.length)))
    }

    var selectedPluginName by remember {
        mutableStateOf(
            InputEntryNameDialogState.selectedPluginName
                ?.takeIf { name -> args.phonemizerPlugins.any { it.name == name } }
                ?: args.phonemizerPlugins.firstOrNull()?.name,
        )
    }
    var phonemizeMode by remember { mutableStateOf(InputEntryNameDialogState.phonemizeMode) }

    val activePlugin = remember(selectedPluginName, args.phonemizerPlugins) {
        args.phonemizerPlugins.find { it.name == selectedPluginName } ?: args.phonemizerPlugins.firstOrNull()
    }

    val currentPhonemes = remember(input.text, activePlugin, phonemizeMode) {
        if (phonemizeMode && activePlugin != null && input.text.isNotBlank()) {
            PhonemizerRunner.run(activePlugin, input.text)
        } else {
            emptyList()
        }
    }

    val submit = { navigation: NavigationDirection? ->
        val phones = if (currentPhonemes.isNotEmpty()) currentPhonemes else listOf(input.text.trim())
        if (phones.isNotEmpty()) {
            finish(
                InputEntryNameDialogResult(
                    index = args.index,
                    name = phones.first(),
                    purpose = args.purpose,
                    navigation = navigation,
                    extraNames = phones.drop(1),
                ),
            )
        }
    }

    val trySubmit = { navigation: NavigationDirection? ->
        val text = input.text.trim()
        if (currentPhonemes.isEmpty() && args.invalidOptions.contains(text)) {
            args.showSnackbar(stringStatic(Strings.EditEntryNameDialogExistingError))
        } else if (text.isNotBlank()) {
            submit(navigation)
        }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(args.index, args.initial) {
        input = TextFieldValue(args.initial, selection = TextRange(0, args.initial.length))
        focusRequester.requestFocus()
    }

    Column(Modifier.widthIn(min = 420.dp)) {
        Spacer(Modifier.height(15.dp))
        Text(
            text = string(args.purpose.stringKey),
            style = MaterialTheme.typography.body2,
            fontWeight = FontWeight.Bold,
        )
        if (args.purpose == InputEntryNameDialogPurpose.Rename && args.phonemizerPlugins.isNotEmpty()) {
            Spacer(Modifier.height(15.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = string(Strings.PhonemizerLabel),
                    style = MaterialTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold,
                )
                Box(
                    modifier = Modifier
                        .background(
                            color = if (phonemizeMode) MaterialTheme.colors.primary else DarkGray,
                            shape = RoundedCornerShape(4.dp),
                        )
                        .clickable {
                            phonemizeMode = !phonemizeMode
                            InputEntryNameDialogState.phonemizeMode = phonemizeMode
                        }
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = string(if (phonemizeMode) Strings.PhonemizerOn else Strings.PhonemizerOff),
                        style = MaterialTheme.typography.caption.copy(
                            color = if (phonemizeMode) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
                if (phonemizeMode) {
                    for (plugin in args.phonemizerPlugins) {
                        val isSelected = activePlugin?.name == plugin.name
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) MaterialTheme.colors.primary.copy(alpha = 0.8f) else DarkGray,
                                    shape = RoundedCornerShape(4.dp),
                                )
                                .clickable {
                                    selectedPluginName = plugin.name
                                    InputEntryNameDialogState.selectedPluginName = plugin.name
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = plugin.displayedName.get(),
                                style = MaterialTheme.typography.caption.copy(
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colors.onPrimary else MaterialTheme.colors.onSurface,
                                ),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(15.dp))
        OutlinedTextField(
            modifier = Modifier.width(380.dp).focusRequester(focusRequester)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && args.purpose == InputEntryNameDialogPurpose.Rename) {
                        if (args.previousKeySet?.shouldCatch(event) == true) {
                            if (args.index > 0) {
                                trySubmit(NavigationDirection.Previous)
                            }
                            true
                        } else if (args.nextKeySet?.shouldCatch(event) == true) {
                            trySubmit(NavigationDirection.Next)
                            true
                        } else {
                            false
                        }
                    } else false
                },
            value = input,
            singleLine = true,
            isError = (currentPhonemes.isEmpty() && args.invalidOptions.contains(input.text)) || input.text.isBlank(),
            onValueChange = { input = it.copy(text = it.text.removeControlCharacters()) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { trySubmit(null) }),
        )
        if (args.presets.isNotEmpty()) {
            Spacer(Modifier.height(15.dp))
            FlowRow(
                modifier = Modifier.widthIn(max = 400.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                args.presets.forEach { preset ->
                    Text(
                        text = preset,
                        modifier = Modifier.background(color = White20, shape = RoundedCornerShape(12.dp)).clip(
                            RoundedCornerShape(12.dp),
                        )
                            .clickable {
                                input = TextFieldValue(preset, selection = TextRange(0, preset.length))
                                focusRequester.requestFocus()
                            }.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.caption,
                    )
                }
            }
        }
        if (currentPhonemes.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Column(
                modifier = Modifier.background(DarkGray, RoundedCornerShape(4.dp)).padding(8.dp).widthIn(min = 380.dp),
            ) {
                Text(
                    text = string(Strings.PhonemizerSpreadAcrossEntries, currentPhonemes.size),
                    style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.Bold),
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    currentPhonemes.take(8).forEachIndexed { i, phone ->
                        Box(
                            modifier = Modifier.background(
                                MaterialTheme.colors.primary.copy(alpha = 0.2f),
                                RoundedCornerShape(2.dp),
                            ).padding(horizontal = 5.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = "${args.index + i}: $phone",
                                style = MaterialTheme.typography.caption.copy(fontSize = 11.sp),
                            )
                        }
                    }
                    if (currentPhonemes.size > 8) {
                        Text(
                            text = "... +${currentPhonemes.size - 8}",
                            style = MaterialTheme.typography.caption.copy(fontSize = 11.sp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.align(Alignment.End), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { dismiss() }) { Text(string(Strings.CommonCancel)) }
            Spacer(Modifier.width(15.dp))
            if (args.purpose == InputEntryNameDialogPurpose.Rename) {
                TextButton(
                    enabled = input.text.isNotBlank() && args.index > 0,
                    onClick = { trySubmit(NavigationDirection.Previous) },
                ) {
                    val label = string(Strings.InputEntryNameDialogPrev) +
                        (args.previousKeySet?.displayedKeyName?.let { " ($it)" } ?: "")
                    Text(label)
                }
                Spacer(Modifier.width(10.dp))
                TextButton(enabled = input.text.isNotBlank(), onClick = { trySubmit(NavigationDirection.Next) }) {
                    val label = string(Strings.InputEntryNameDialogNext) +
                        (args.nextKeySet?.displayedKeyName?.let { " ($it)" } ?: "")
                    Text(label)
                }
                Spacer(Modifier.width(15.dp))
            }
            ConfirmButton(enabled = input.text.isNotBlank(), onClick = { trySubmit(null) })
        }
    }
}
