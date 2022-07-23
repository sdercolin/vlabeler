package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun <T> SelectionBox(value: T, onSelect: (T) -> Unit, options: Array<T>, getText: (T) -> String) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier.border(
                width = 1.dp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
                shape = RoundedCornerShape(5.dp)
            )
                .clickable { expanded = !expanded }
                .padding(vertical = 10.dp, horizontal = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.widthIn(min = 150.dp),
                text = getText(value),
                style = MaterialTheme.typography.body2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(imageVector = Icons.Default.ExpandMore, modifier = Modifier.size(24.dp), contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach {
                DropdownMenuItem(
                    onClick = {
                        onSelect(it)
                        expanded = false
                    }
                ) {
                    Text(text = getText(it), style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}
