package com.sdercolin.vlabeler.ui.dialog.preferences

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sdercolin.vlabeler.model.AppConf

class PreferencesEditorState(private val initConf: AppConf, private val submit: (AppConf?) -> Unit) {

    private var savedConf: AppConf by mutableStateOf(initConf)
    private var _conf: AppConf by mutableStateOf(initConf)
    val conf get() = _conf

    val canSave get() = savedConf != _conf

    fun save() {
        savedConf = _conf
    }

    fun finish(positive: Boolean) {
        if (positive) save()
        submit(savedConf.takeIf { it != initConf })
    }
}
