package com.sdercolin.vlabeler.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sdercolin.vlabeler.tracking.TrackingState
import com.sdercolin.vlabeler.ui.common.ConfirmButton
import com.sdercolin.vlabeler.ui.common.SmallDialogContainer
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.ui.string.string
import com.sdercolin.vlabeler.ui.theme.getSwitchColors

@Composable
fun TrackingSettingsDialog(
    trackingState: TrackingState,
    finish: () -> Unit,
) {
    SmallDialogContainer(wrapHeight = true) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp, horizontal = 40.dp)) {
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = string(Strings.TrackingSettingsDialogTitle),
                style = MaterialTheme.typography.h5,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = string(Strings.TrackingSettingsDialogDescription),
                style = MaterialTheme.typography.body2,
            )
            Spacer(modifier = Modifier.height(20.dp))
            Content(trackingState)
            if (trackingState.hasNotAskedForTrackingPermission()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = string(Strings.TrackingSettingsDialogFirstTimeAlert),
                    style = MaterialTheme.typography.caption,
                )
            }
            Spacer(Modifier.height(30.dp))
            ButtonBar(openDetails = trackingState::openDetailsWebPage, finish = finish)
        }
    }
}

@Composable
private fun Content(trackingState: TrackingState) {
    val trackingId by trackingState.trackingIdFlow.collectAsState()
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = string(Strings.TrackingSettingsDialogEnabled),
                style = MaterialTheme.typography.body1,
            )
            Spacer(modifier = Modifier.width(20.dp))
            Switch(
                checked = trackingId != null,
                onCheckedChange = {
                    if (it) trackingState.enable() else trackingState.disable()
                },
                colors = getSwitchColors(),
            )
        }
        Spacer(modifier = Modifier.height(15.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = string(Strings.TrackingSettingsDialogTrackingIdLabel),
                style = MaterialTheme.typography.body1,
            )
            Spacer(modifier = Modifier.width(20.dp))
            TextField(
                value = trackingId ?: "",
                onValueChange = {},
                readOnly = true,
            )
        }
    }
}

@Composable
private fun ButtonBar(openDetails: () -> Unit, finish: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        TextButton(
            onClick = { openDetails() },
        ) {
            Text(string(Strings.CommonDetails))
        }
        Spacer(Modifier.weight(1f))
        ConfirmButton(onClick = finish)
    }
}
