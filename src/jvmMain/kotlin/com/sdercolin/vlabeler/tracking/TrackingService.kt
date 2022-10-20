package com.sdercolin.vlabeler.tracking

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.env.isDebug
import com.sdercolin.vlabeler.tracking.event.InitializeEvent
import com.sdercolin.vlabeler.tracking.event.TrackingEvent
import com.sdercolin.vlabeler.ui.AppRecordStore
import com.sdercolin.vlabeler.util.jsonMinified
import com.segment.analytics.kotlin.core.Analytics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement

class TrackingService(appRecordStore: AppRecordStore, mainScope: CoroutineScope) {

    private var enabled = false

    private val analytics = Analytics(WriteKey) {
        application = ApplicationName
    }

    init {
        val trackingIdFlow = appRecordStore.stateFlow
            .map { it.trackingId }
            .distinctUntilChanged()
        trackingIdFlow.onEach {
            enabled = it != null
            if (it != null) {
                // Only use the random uuid to identify user
                analytics.identify(it)
            } else {
                analytics.reset()
            }
        }
            .launchIn(mainScope)

        Log.fatalErrorTracker = Log.FatalErrorTracker { track(it) }

        if (isDebug) {
            // TODO: check if user has allowed tracking
            appRecordStore.update {
                generateTackingIdIfNeeded().also {
                    track(InitializeEvent(it.trackingId.orEmpty()))
                }
            }
        }
    }

    fun track(event: TrackingEvent) {
        if (!enabled) return
        try {
            val eventObject = jsonMinified.encodeToJsonElement(event) as JsonObject
            analytics.track(event.name, eventObject)
            Log.debug("${event.name}: ${jsonMinified.encodeToString(event)}")
        } catch (e: Throwable) {
            Log.error(e)
        }
    }

    companion object {
        private const val WriteKey = "o53TJAzB08cMpilJhu3mwdAAzojyVxIu"
        private const val ApplicationName = "vLabeler"
    }
}
