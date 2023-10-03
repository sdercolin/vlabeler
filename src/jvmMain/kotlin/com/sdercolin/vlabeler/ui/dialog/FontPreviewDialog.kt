package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.util.Resources

@Composable
fun FontPreviewDialog(appConf: AppConf, finish: () -> Unit) {
    DialogWindow(
        title = "Font Preview",
        icon = painterResource(Resources.iconIco),
        onCloseRequest = finish,
        state = rememberDialogState(width = 800.dp, height = 1000.dp),
    ) {
        AppTheme(appConf.view) {
            Content()
        }
    }
}

@Composable
private fun Content() {
    val fixedText = "aAあア亜啊"
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                "h1:$fixedText",
                style = MaterialTheme.typography.h1,
            )
            Text(
                "h2:$fixedText",
                style = MaterialTheme.typography.h2,
            )
            Text(
                "h3el:$fixedText",
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.ExtraLight,
            )
            Text(
                "h3l:$fixedText",
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Light,
            )
            Text(
                "h3:$fixedText",
                style = MaterialTheme.typography.h3,
            )
            Text(
                "h3b:$fixedText",
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "h3eb:$fixedText",
                style = MaterialTheme.typography.h3,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                "h4:$fixedText",
                style = MaterialTheme.typography.h4,
            )
            Text(
                "h5:$fixedText",
                style = MaterialTheme.typography.h5,
            )
            Text(
                "h6:$fixedText",
                style = MaterialTheme.typography.h6,
            )
            Text(
                "subtitle1:$fixedText",
                style = MaterialTheme.typography.subtitle1,
            )
            Text(
                "subtitle2:$fixedText",
                style = MaterialTheme.typography.subtitle2,
            )
            Text(
                "body1:$fixedText",
                style = MaterialTheme.typography.body1,
            )
            Text(
                "body2:$fixedText",
                style = MaterialTheme.typography.body2,
            )
            Text(
                "button:$fixedText",
                style = MaterialTheme.typography.button,
            )
            Text(
                "caption:$fixedText",
                style = MaterialTheme.typography.caption,
            )
            Text(
                "overline:$fixedText",
                style = MaterialTheme.typography.overline,
            )
        }
    }
}
