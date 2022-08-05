package com.sdercolin.vlabeler.ui.dialog.customization

import androidx.compose.runtime.Composable
import com.sdercolin.vlabeler.ui.AppState

@Composable
fun CustomizableItemManagerDialog(
    type: CustomizableItem.Type,
    appState: AppState,
    state: CustomizableItemManagerDialogState<*> = rememberCustomizableItemManagerDialogState(type, appState)
) {
}
