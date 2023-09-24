package com.sdercolin.vlabeler.tracking

import com.sdercolin.vlabeler.env.Log
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

/**
 * Service for tracking implemented by Segment.
 */
class TrackingService(appRecordStore: AppRecordStore, mainScope: CoroutineScope) {

    private var enabled: Boolean? = null

    private val analytics = Analytics(WRITE_KEY) {
        application = APPLICATION_NAME
    }

    init {
        val trackingIdFlow = appRecordStore.stateFlow
            .map { it.trackingId }
            .distinctUntilChanged()
        trackingIdFlow.onEach {
            if (it != null) {
                // Only use the random uuid to identify user
                analytics.identify(it)
                Log.debug("Analytics identify: $it")
            } else {
                analytics.reset()
                Log.debug("Analytics reset")
            }
            val lastEnabled = enabled
            enabled = it != null
            if (lastEnabled != null && it != null) {
                track(InitializeEvent(it))
            }
        }
            .launchIn(mainScope)

        Log.fatalErrorTracker = Log.FatalErrorTracker { track(it) }
    }

    fun track(event: TrackingEvent) {
        if (enabled != true) return
        try {
            val eventObject = jsonMinified.encodeToJsonElement(event) as JsonObject
            analytics.track(event.name, eventObject)
            Log.debug("${event.name}: ${jsonMinified.encodeToString(event)}")
        } catch (e: Throwable) {
            Log.error(e)
        }
    }

    companion object {
        private const val WRITE_KEY = "o53TJAzB08cMpilJhu3mwdAAzojyVxIu"
        private const val APPLICATION_NAME = "vLabeler"
    }
}
