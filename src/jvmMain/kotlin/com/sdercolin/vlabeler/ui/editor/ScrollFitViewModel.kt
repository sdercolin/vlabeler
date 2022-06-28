package com.sdercolin.vlabeler.ui.editor

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ScrollFitViewModel(private val coroutineScope: CoroutineScope) {

    private val _eventFlow = MutableSharedFlow<Int>(replay = 0)
    val eventFlow = _eventFlow.asSharedFlow()

    private var pendingValue: Int = 0
    private var waitingNext = false

    fun update(value: Int) {
        pendingValue = value
        if (waitingNext) {
            waitingNext = false
            emit()
        }
    }

    fun emit() {
        coroutineScope.launch {
            _eventFlow.emit(pendingValue)
        }
    }

    fun emitNext() {
        waitingNext = true
    }
}
