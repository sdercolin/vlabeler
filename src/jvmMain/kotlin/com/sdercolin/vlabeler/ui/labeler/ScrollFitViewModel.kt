package com.sdercolin.vlabeler.ui.labeler

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ScrollFitViewModel(private val coroutineScope: CoroutineScope) {

    private val _eventFlow = MutableSharedFlow<Int>(replay = 0)
    val eventFlow = _eventFlow.asSharedFlow()

    private var pendingValue: Int = 0

    fun update(value: Int) {
        pendingValue = value
    }

    fun emit() {
        coroutineScope.launch {
            _eventFlow.emit(pendingValue)
        }
    }
}
