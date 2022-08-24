package com.sdercolin.vlabeler.ui.dialog.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Button
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.action.Action
import com.sdercolin.vlabeler.ui.common.WarningText
import com.sdercolin.vlabeler.ui.common.WarningTextStyle
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.Black50

@Composable
fun <K : Action> KeymapItemEditConflictDialog(
    args: PreferencesEditorState.KeymapItemEditConflictDialogArgs<K>,
) {
    Box(
        modifier = Modifier.fillMaxSize().background(color = Black50),
        contentAlignment = Alignment.Center,
    ) {
        Surface {
            Column(
                modifier = Modifier.widthIn(min = 350.dp)
                    .padding(horizontal = 35.dp, vertical = 20.dp),
            ) {
                Spacer(Modifier.height(15.dp))
                WarningText(
                    text = string(Strings.PreferencesKeymapEditDialogConflictingWarning),
                    style = WarningTextStyle.Warning,
                )
                Spacer(Modifier.height(25.dp))
                Row(modifier = Modifier.align(Alignment.End), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = { args.cancel() },
                    ) {
                        Text(string(Strings.CommonCancel))
                    }
                    Spacer(Modifier.width(20.dp))
                    TextButton(
                        onClick = { args.keep() },
                    ) {
                        Text(string(Strings.PreferencesKeymapEditDialogConflictingWarningKeep))
                    }
                    Spacer(Modifier.width(20.dp))
                    Button(
                        onClick = { args.remove() },
                    ) {
                        Text(string(Strings.PreferencesKeymapEditDialogConflictingWarningRemove))
                    }
                }
            }
        }
    }
}
