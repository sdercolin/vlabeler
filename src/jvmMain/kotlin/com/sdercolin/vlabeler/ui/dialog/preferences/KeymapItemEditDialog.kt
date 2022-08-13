package com.sdercolin.vlabeler.ui.dialog.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.env.released
import com.sdercolin.vlabeler.model.action.Action
import com.sdercolin.vlabeler.model.action.getConflictingKeyBinds
import com.sdercolin.vlabeler.model.key.KeySet
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.Black50
import com.sdercolin.vlabeler.ui.theme.DarkYellow

class KeymapItemEditDialogState<K : Action>(private val args: PreferencesEditorState.KeymapItemEditDialogArgs<K>) {
    private var keySet: KeySet? by mutableStateOf(args.actionKeyBind.keySet)

    var text: TextFieldValue by mutableStateOf(
        args.actionKeyBind.keySet?.displayedKeyName.orEmpty().let { textValue ->
            TextFieldValue(
                textValue,
                selection = TextRange(textValue.length)
            )
        }
    )
        private set

    val isValid: Boolean
        get() {
            val keySet = keySet ?: return true
            if (!keySet.isValid()) return false
            if (args.keymapItem.actionType.requiresCompleteKeySet && keySet.isComplete.not()) return false
            return true
        }

    fun getConflictingActions(): List<K> = args.allKeyBinds.getConflictingKeyBinds(keySet, args.actionKeyBind.action)
        .map { it.action }

    fun updateKeySet(keyEvent: KeyEvent) {
        if (keyEvent.released) return
        val keySet = KeySet.fromKeyEvent(keyEvent)
        if (keySet.isValid()) {
            updateKeySet(keySet)
        }
    }

    private fun updateKeySet(keySet: KeySet?) {
        this.keySet = keySet
        val textValue = keySet?.displayedKeyName.orEmpty()
        text = TextFieldValue(
            textValue,
            selection = TextRange(textValue.length)
        )
    }

    fun clearKeySet() {
        updateKeySet(null)
    }

    fun cancel() {
        args.submit(null)
    }

    fun submit() {
        args.submit(args.actionKeyBind.update(keySet))
    }
}

@Composable
fun <K : Action> KeymapItemEditDialog(
    args: PreferencesEditorState.KeymapItemEditDialogArgs<K>,
    state: KeymapItemEditDialogState<K> = remember(args) { KeymapItemEditDialogState(args) }
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    Box(
        modifier = Modifier.fillMaxSize().background(color = Black50),
        contentAlignment = Alignment.Center
    ) {
        Surface {
            Column(
                modifier = Modifier.widthIn(min = 300.dp)
                    .padding(horizontal = 30.dp, vertical = 20.dp)
            ) {
                Text(
                    text = string(Strings.PreferencesKeymapEditDialogTitle),
                    style = MaterialTheme.typography.caption,
                    maxLines = 1
                )
                Spacer(Modifier.height(15.dp))
                Text(
                    text = args.actionKeyBind.title,
                    style = MaterialTheme.typography.body2,
                    maxLines = 1
                )
                Spacer(Modifier.height(30.dp))
                Row(
                    modifier = Modifier
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(5.dp)
                        )
                        .padding(vertical = 15.dp, horizontal = 15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        modifier = Modifier.widthIn(min = 120.dp)
                            .focusRequester(focusRequester)
                            .onPreviewKeyEvent { keyEvent ->
                                state.updateKeySet(keyEvent)
                                true
                            },
                        value = state.text,
                        textStyle = MaterialTheme.typography.body2
                            .copy(color = MaterialTheme.colors.onBackground),
                        onValueChange = {},
                        maxLines = 1,
                        cursorBrush = SolidColor(MaterialTheme.colors.onBackground)
                    )
                    Spacer(Modifier.width(20.dp))
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { state.clearKeySet() }
                            .padding(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = null,
                            tint = MaterialTheme.colors.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                val conflictedActions = state.getConflictingActions()
                if (conflictedActions.isNotEmpty()) {
                    Spacer(Modifier.height(15.dp))
                    Row {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = DarkYellow,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(20.dp))
                        Column {
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = string(Strings.PreferencesKeymapEditDialogConflictingLabel),
                                style = MaterialTheme.typography.caption,
                                maxLines = 1
                            )
                            for (action in conflictedActions) {
                                Text(
                                    text = action.title,
                                    style = MaterialTheme.typography.caption,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(25.dp))
                Row(modifier = Modifier.align(Alignment.End), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = { state.cancel() }
                    ) {
                        Text(string(Strings.CommonCancel))
                    }
                    Spacer(Modifier.width(25.dp))
                    Button(
                        enabled = state.isValid,
                        onClick = { state.submit() }
                    ) {
                        Text(string(Strings.CommonOkay))
                    }
                }
            }
        }
    }
}
