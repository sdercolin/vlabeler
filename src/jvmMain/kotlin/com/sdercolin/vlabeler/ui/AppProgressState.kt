package com.sdercolin.vlabeler.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

interface AppProgressState {
    val isBusy: Boolean
    fun showProgress()
    fun hideProgress()
}

class AppProgressStateImpl : AppProgressState {
    override var isBusy: Boolean by mutableStateOf(false)
        private set

    override fun showProgress() {
        isBusy = true
    }

    override fun hideProgress() {
        isBusy = false
    }
}
