package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.DarkRed
import com.sdercolin.vlabeler.ui.theme.DarkYellow

@Composable
fun WarningText(text: String, style: WarningTextStyle) {
    Row(Modifier.widthIn(300.dp, 600.dp)) {
        Icon(
            imageVector = style.icon,
            contentDescription = null,
            tint = style.color,
            modifier = Modifier.size(35.dp),
        )
        Spacer(Modifier.width(20.dp))
        Column {
            Text(text = string(style.title), style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(15.dp))
            Text(text = text, style = MaterialTheme.typography.body2, softWrap = true)
        }
    }
}

enum class WarningTextStyle(val icon: ImageVector, val color: Color, val title: Strings) {
    Warning(Icons.Default.Warning, DarkYellow, Strings.CommonWarning),
    Error(Icons.Default.Error, DarkRed, Strings.CommonError)
}
