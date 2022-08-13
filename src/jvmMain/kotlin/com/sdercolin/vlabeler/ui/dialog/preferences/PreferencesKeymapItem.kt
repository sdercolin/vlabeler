package com.sdercolin.vlabeler.ui.dialog.preferences

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.action.ActionKeyBind
import com.sdercolin.vlabeler.model.action.ActionType
import com.sdercolin.vlabeler.model.action.KeyAction
import com.sdercolin.vlabeler.model.action.KeyActionKeyBind
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.util.runIf

@Composable
fun PreferencesKeymapItem(
    keyBind: ActionKeyBind<*>,
    actionType: ActionType,
    onClickItem: (ActionKeyBind<*>, ActionType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .height(48.dp)
            .clickable(enabled = keyBind.editable) { onClickItem(keyBind, actionType) }
            .padding(horizontal = 30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = keyBind.title,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onBackground.runIf(keyBind.editable) { copy(alpha = 0.8f) },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.weight(1f))
        val keySet = keyBind.keySet
        if (keySet != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    modifier = Modifier.background(
                        color = MaterialTheme.colors.primaryVariant,
                        shape = RoundedCornerShape(5.dp)
                    )
                        .padding(horizontal = 5.dp, vertical = 3.dp),
                    text = keySet.displayedKeyName,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
@Preview
private fun Preview() = AppTheme {
    Column(
        modifier = Modifier.background(MaterialTheme.colors.background)
            .fillMaxWidth()
    ) {
        PreferencesKeymapItem(
            KeyActionKeyBind(KeyAction.OpenProject, KeyAction.OpenProject.defaultKeySet),
            ActionType.Key
        ) { _, _ -> }
        PreferencesKeymapItem(
            KeyActionKeyBind(KeyAction.NewProject, null),
            ActionType.Key
        ) { _, _ -> }
    }
}
