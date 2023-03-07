package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import com.sdercolin.vlabeler.util.runIf

@Composable
fun <T> SelectionBox(
    value: T,
    onSelect: (T) -> Unit,
    options: Collection<T>,
    getText: @Composable (T) -> String = { it.toString() },
    modifier: Modifier = Modifier,
    fixedWidth: Boolean = false,
    customPadding: Boolean = false,
    showIcon: Boolean = true,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = modifier
                .heightIn(min = 46.dp)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(5.dp),
                )
                .clickable(enabled) { expanded = !expanded }
                .runIf(!customPadding) { padding(vertical = 10.dp, horizontal = 15.dp) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = if (fixedWidth) Modifier.weight(1f) else Modifier.widthIn(min = 120.dp),
                text = getText(value),
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onBackground.runIf(!enabled) { copy(alpha = 0.5f) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showIcon) {
                Spacer(Modifier.size(10.dp))
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    modifier = Modifier.size(24.dp),
                    contentDescription = null,
                    tint = MaterialTheme.colors.onBackground,
                )
            } else {
                Spacer(Modifier.height(24.dp).width(1.dp))
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach {
                DropdownMenuItem(
                    onClick = {
                        onSelect(it)
                        expanded = false
                    },
                ) {
                    Text(text = getText(it), style = MaterialTheme.typography.body2)
                }
            }
        }
    }
}
