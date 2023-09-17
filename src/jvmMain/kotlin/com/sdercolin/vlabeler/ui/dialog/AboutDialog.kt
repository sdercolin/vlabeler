package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.sdercolin.vlabeler.env.Locale
import com.sdercolin.vlabeler.env.appVersion
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.env.osInfo
import com.sdercolin.vlabeler.env.runtimeVersion
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.AppRecord
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.util.AppDir
import com.sdercolin.vlabeler.util.Clipboard
import com.sdercolin.vlabeler.util.ResourcePath
import com.sdercolin.vlabeler.util.Resources

@Composable
fun AboutDialog(
    appRecord: AppRecord,
    appConf: AppConf,
    showLicenses: () -> Unit,
    finish: () -> Unit,
) {
    DialogWindow(
        title = string(Strings.AboutDialogTitle),
        icon = painterResource(Resources.iconIco),
        onCloseRequest = finish,
        state = rememberDialogState(width = 450.dp, height = 350.dp),
        resizable = false,
    ) {
        AppTheme(appConf.view) {
            Content(
                appRecord = appRecord,
                showLicenses = showLicenses,
                finish = finish,
            )
        }
    }
}

@Composable
private fun Content(
    appRecord: AppRecord,
    showLicenses: () -> Unit,
    finish: () -> Unit,
) {
    val info = remember {
        """
        App version: $appVersion
        Runtime version: $runtimeVersion
        OS: $osInfo
        System locale: $Locale
        Debug mode: $isDebug
        User directory: $AppDir
        Resource directory: $ResourcePath
        Tracking id: ${appRecord.trackingId}
        """.trimIndent()
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 20.dp, horizontal = 35.dp),
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                horizontalArrangement = Arrangement.Center,
            ) {
                Image(
                    modifier = Modifier.size(70.dp),
                    painter = painterResource(Resources.iconPng),
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(30.dp))
                Text(
                    text = string(Strings.AppName),
                    style = MaterialTheme.typography.h2,
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            InfoBox(info)
            Spacer(modifier = Modifier.height(20.dp))
            ButtonBar(
                copyInfo = { Clipboard.copyToClipboard(info) },
                showLicenses = showLicenses,
                finish = finish,
            )
        }
    }
}

@Composable
private fun ColumnScope.InfoBox(info: String) {
    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier.weight(1f)
            .fillMaxWidth()
            .background(MaterialTheme.colors.background)
            .padding(15.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
        ) {
            BasicTextField(
                value = info,
                onValueChange = {},
                readOnly = true,
                textStyle = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.onBackground),
            )
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun ButtonBar(copyInfo: () -> Unit, showLicenses: () -> Unit, finish: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(
            onClick = showLicenses,
        ) {
            Text(string(Strings.AboutDialogShowLicenses))
        }
        Spacer(Modifier.width(25.dp))
        TextButton(
            onClick = copyInfo,
        ) {
            Text(string(Strings.AboutDialogCopyInfo))
        }
        Spacer(Modifier.weight(1f))
        ConfirmButton(onClick = finish)
    }
}
