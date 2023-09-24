package com.sdercolin.vlabeler.tracking

import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.util.Url
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

/**
 * State object for tracking.
 */
class TrackingState(
    private val appRecordStore: AppRecordStore,
    private val scope: CoroutineScope,
) {

    fun hasNotAskedForTrackingPermission(): Boolean {
        return appRecordStore.value.hasAskedForTrackingPermission.not()
    }

    val trackingIdFlow = MutableStateFlow(appRecordStore.value.trackingId).apply {
        appRecordStore.stateFlow.map { it.trackingId }.onEach { emit(it) }.launchIn(scope)
    }

    fun enable() {
        appRecordStore.update { generateTrackingId() }
    }

    fun disable() {
        appRecordStore.update { clearTrackingId() }
    }

    fun finishSettings() {
        appRecordStore.update { copy(hasAskedForTrackingPermission = true) }
    }

    fun openDetailsWebPage() {
        Url.open(Url.TRACKING_DOCUMENT)
    }
}
