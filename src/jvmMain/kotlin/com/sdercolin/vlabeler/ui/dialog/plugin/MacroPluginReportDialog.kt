package com.sdercolin.vlabeler.ui.dialog.plugin

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.MediumDialogContainer
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.util.Clipboard

@Composable
fun MacroPluginReportDialog(report: LocalizedJsonString, finish: () -> Unit) {
    MediumDialogContainer {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 30.dp)) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = string(Strings.MacroPluginReportDialogTitle),
                style = MaterialTheme.typography.h6,
            )
            Spacer(Modifier.height(15.dp))
            Box(
                modifier = Modifier.fillMaxWidth()
                    .weight(1f)
                    .background(MaterialTheme.colors.background)
                    .padding(20.dp),
            ) {
                val scrollState = rememberScrollState()
                Box(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState)) {
                    BasicTextField(
                        value = report.get(),
                        onValueChange = {},
                        readOnly = true,
                        textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onBackground),
                    )
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(scrollState),
                    modifier = Modifier.align(Alignment.CenterEnd).width(15.dp),
                )
            }
            Spacer(Modifier.height(30.dp))
            val localLanguage = LocalLanguage.current
            ButtonBar(copy = { Clipboard.copyToClipboard(report.getCertain(localLanguage)) }, finish = finish)
        }
    }
}

@Composable
private fun ButtonBar(copy: () -> Unit, finish: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(onClick = { copy() }) {
            Text(string(Strings.MacroPluginReportDialogCopy))
        }
        Spacer(Modifier.weight(1f))
        ConfirmButton(onClick = finish)
    }
}
