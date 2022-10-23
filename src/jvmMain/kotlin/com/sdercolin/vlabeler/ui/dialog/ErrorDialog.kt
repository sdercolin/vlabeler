package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.WarningText
import com.sdercolin.vlabeler.ui.common.WarningTextStyle
import com.sdercolin.vlabeler.ui.common.plainClickable
import com.sdercolin.vlabeler.ui.theme.Black50

@Composable
fun WarningDialog(
    message: String,
    finish: () -> Unit,
    style: WarningTextStyle,
) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(color = Black50)
            .plainClickable(),
        contentAlignment = Alignment.Center,
    ) {
        Surface {
            Box(
                modifier = Modifier
                    .padding(horizontal = 35.dp, vertical = 20.dp),
            ) {
                Column(Modifier.widthIn(min = 350.dp)) {
                    Spacer(Modifier.height(15.dp))
                    WarningText(
                        text = message,
                        style = style,
                    )
                    Spacer(Modifier.height(25.dp))
                    Row(modifier = Modifier.align(Alignment.End), horizontalArrangement = Arrangement.End) {
                        ConfirmButton(onClick = finish, autoFocus = true)
                    }
                }
            }
        }
    }
}
