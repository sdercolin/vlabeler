package com.sdercolin.vlabeler.ui.editor.labeler

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CenterFocusWeak
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.AppConf

@Composable
fun BottomBar(
    currentEntryIndexInTotal: Int,
    totalEntryCount: Int,
    resolution: Int,
    onChangeResolution: (Int) -> Unit,
    openSetResolutionDialog: () -> Unit,
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    goNextEntry: () -> Unit,
    goPreviousEntry: () -> Unit,
    goNextSample: () -> Unit,
    goPreviousSample: () -> Unit,
    openJumpToEntryDialog: () -> Unit,
    scrollFit: () -> Unit,
    appConf: AppConf
) {
    Log.info("BottomBar: composed")
    val resolutionRange = CanvasParams.ResolutionRange(appConf.painter.canvasResolution)
    Surface {
        Row(
            modifier = Modifier.fillMaxWidth().height(30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.width(30.dp).fillMaxHeight()
                    .clickable(
                        enabled = canGoPrevious,
                        onClick = goPreviousSample
                    )
                    .padding(start = 8.dp)
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "<<",
                    style = MaterialTheme.typography.caption
                )
            }
            Box(
                Modifier.width(30.dp).fillMaxHeight()
                    .clickable(
                        enabled = canGoPrevious,
                        onClick = goPreviousEntry
                    )
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "<",
                    style = MaterialTheme.typography.caption
                )
            }
            Box(
                Modifier.fillMaxHeight()
                    .clickable { openJumpToEntryDialog() }
                    .padding(horizontal = 15.dp)
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "${currentEntryIndexInTotal + 1} / $totalEntryCount",
                    style = MaterialTheme.typography.caption
                )
            }
            Box(
                Modifier.width(30.dp).fillMaxHeight()
                    .clickable(
                        enabled = canGoNext,
                        onClick = goNextEntry
                    )
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = ">",
                    style = MaterialTheme.typography.caption
                )
            }
            Box(
                Modifier.width(30.dp).fillMaxHeight()
                    .clickable(
                        enabled = canGoNext,
                        onClick = goNextSample
                    )
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = ">>",
                    style = MaterialTheme.typography.caption
                )
            }

            Spacer(Modifier.weight(0.8f))

            Box(
                Modifier.fillMaxHeight()
                    .clickable { scrollFit() }
                    .padding(horizontal = 15.dp)
            ) {
                Icon(
                    modifier = Modifier.size(15.dp).align(Alignment.Center),
                    imageVector = Icons.Default.CenterFocusWeak,
                    contentDescription = null
                )
            }

            Spacer(Modifier.weight(1f))

            Box(
                Modifier.width(30.dp).fillMaxHeight()
                    .clickable(
                        enabled = resolutionRange.canIncrease(resolution),
                        onClick = { onChangeResolution(resolutionRange.increaseFrom(resolution)) }
                    )
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "-",
                    style = MaterialTheme.typography.caption
                )
            }
            Box(
                Modifier.fillMaxHeight()
                    .clickable(
                        enabled = resolutionRange.canIncrease(resolution),
                        onClick = { openSetResolutionDialog() }
                    )
            ) {
                Text(
                    modifier = Modifier.defaultMinSize(minWidth = 55.dp).align(Alignment.Center),
                    text = "1/$resolution",
                    style = MaterialTheme.typography.caption,
                    textAlign = TextAlign.Center
                )
            }
            Box(
                Modifier.width(30.dp).fillMaxHeight()
                    .clickable(
                        enabled = resolutionRange.canDecrease(resolution),
                        onClick = { onChangeResolution(resolutionRange.decreaseFrom(resolution)) }
                    )
                    .padding(end = 8.dp)
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = "+",
                    style = MaterialTheme.typography.caption
                )
            }
        }
    }
}
