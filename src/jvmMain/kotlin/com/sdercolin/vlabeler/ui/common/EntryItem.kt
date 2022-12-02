package com.sdercolin.vlabeler.ui.common

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.theme.LightGray
import com.sdercolin.vlabeler.util.runIf

@Composable
fun EntryItemNumber(index: Int) {
    BasicText(
        text = "${index + 1}",
        modifier = Modifier.padding(start = 20.dp, end = 15.dp, top = 3.dp).widthIn(20.dp),
        maxLines = 1,
        style = MaterialTheme.typography.caption.copy(color = LightGray.copy(alpha = 0.5f)),
    )
}

@Composable
fun EntryItemSummary(name: String, sample: String, viewConf: AppConf.View) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        BasicText(
            text = name,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onBackground),
        )
        BasicText(
            text = sample.runIf(viewConf.hideSampleExtension) {
                substringBeforeLast('.')
            },
            modifier = Modifier.padding(start = 10.dp, top = 3.dp),
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = MaterialTheme.typography.caption.copy(color = LightGray.copy(alpha = 0.5f)),
        )
    }
}
