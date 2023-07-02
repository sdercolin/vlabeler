package com.sdercolin.vlabeler.ui.dialog.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.action.Action
import com.sdercolin.vlabeler.model.action.ActionKeyBind
import com.sdercolin.vlabeler.model.action.MouseClickAction
import com.sdercolin.vlabeler.ui.string.LocalLanguage
import com.sdercolin.vlabeler.ui.theme.Black20
import com.sdercolin.vlabeler.ui.theme.White20

@Composable
fun <K : Action> PreferencesKeymapItem(
    keyBind: ActionKeyBind<K>,
    keymap: PreferencesItem.Keymap<K>,
    onClickItem: (ActionKeyBind<K>, PreferencesItem.Keymap<K>) -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .height(48.dp)
            .clickable(enabled = keyBind.editable) { onClickItem(keyBind, keymap) },
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.padding(horizontal = 30.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = keyBind.getTitle(LocalLanguage.current),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val action = keyBind.action
            if (action is MouseClickAction) {
                Spacer(Modifier.width(10.dp))
                val tool = action.tool
                Icon(
                    imageVector = tool.icon,
                    contentDescription = null,
                    modifier = Modifier.rotate(tool.iconRotate)
                        .background(color = White20, shape = RoundedCornerShape(5.dp))
                        .padding(3.dp)
                        .size(16.dp),
                    tint = MaterialTheme.colors.onBackground,
                )
            }
            Spacer(Modifier.weight(1f))
            val keySet = keyBind.keySet
            if (keySet != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        modifier = Modifier.background(
                            color = MaterialTheme.colors.primaryVariant,
                            shape = RoundedCornerShape(5.dp),
                        )
                            .padding(horizontal = 5.dp, vertical = 3.dp),
                        text = keySet.displayedKeyName,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        if (keyBind.editable.not()) {
            Box(Modifier.fillMaxSize().background(Black20))
        }
    }
}
