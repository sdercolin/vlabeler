package com.sdercolin.vlabeler.ui.dialog

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindow
import androidx.compose.ui.window.rememberDialogState
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.license.LicenseReport
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.SingleClickableText
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.AppTheme
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.Url

@Composable
fun LicenseDialog(
    appConf: AppConf,
    finish: () -> Unit,
) {
    DialogWindow(
        title = string(Strings.LicenseDialogTitle),
        icon = painterResource(Resources.iconIco),
        onCloseRequest = finish,
        state = rememberDialogState(width = 800.dp, height = 600.dp),
        resizable = false,
    ) {
        AppTheme(appConf.view) {
            Content(
                finish = finish,
            )
        }
    }
}

@Composable
private fun Content(
    finish: () -> Unit,
) {
    val report = remember { LicenseReport.load() }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 20.dp, horizontal = 35.dp),
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = string(Strings.LicenseDialogLicenses),
                style = MaterialTheme.typography.h4,
            )
            Spacer(modifier = Modifier.height(20.dp))
            ReportBox(report)
            Spacer(modifier = Modifier.height(20.dp))
            ButtonBar(
                finish = finish,
            )
        }
    }
}

@Composable
private fun ColumnScope.ReportBox(report: LicenseReport) {
    val lazyState = rememberLazyListState()
    Box(
        modifier = Modifier.weight(1f)
            .fillMaxWidth()
            .background(MaterialTheme.colors.background),
    ) {
        LazyColumn(state = lazyState) {
            items(report.dependencies) { dependency ->
                Row(modifier = Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = dependency.moduleName,
                                style = MaterialTheme.typography.body2.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = dependency.moduleVersion,
                                style = MaterialTheme.typography.caption,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        val license = dependency.moduleLicense
                        if (license != null) {
                            Spacer(modifier = Modifier.height(5.dp))
                            val url = dependency.moduleLicenseUrl
                            if (url == null) {
                                Text(
                                    text = license,
                                    style = MaterialTheme.typography.caption,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            } else {
                                SingleClickableText(
                                    text = license,
                                    style = MaterialTheme.typography.caption,
                                    onClick = { Url.open(url) },
                                )
                            }
                        }
                    }

                    val url = dependency.moduleUrl
                    if (url != null) {
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(
                            onClick = { Url.open(url) },
                        ) {
                            Icon(
                                painter = rememberVectorPainter(Icons.Default.OpenInBrowser),
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(lazyState),
            modifier = Modifier.align(Alignment.CenterEnd),
        )
    }
}

@Composable
private fun ButtonBar(finish: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        ConfirmButton(onClick = finish)
    }
}
