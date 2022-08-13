package com.sdercolin.vlabeler.ui.dialog.preferences

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.action.ActionKeyBind
import com.sdercolin.vlabeler.model.action.ActionType
import com.sdercolin.vlabeler.ui.string.Strings

sealed class PreferencesItem<T>(
    val title: Strings?,
    val description: Strings?,
    val defaultValue: T,
    val select: (AppConf) -> T,
    val update: AppConf.(T) -> AppConf,
    val enabled: (AppConf) -> Boolean
) {

    fun reset(conf: AppConf) = update(conf, defaultValue)

    class Switch(
        title: Strings,
        description: Strings?,
        defaultValue: Boolean,
        select: (AppConf) -> Boolean,
        update: AppConf.(Boolean) -> AppConf,
        enabled: (AppConf) -> Boolean
    ) : PreferencesItem<Boolean>(title, description, defaultValue, select, update, enabled)

    class IntegerInput(
        title: Strings,
        description: Strings?,
        defaultValue: Int,
        select: (AppConf) -> Int,
        update: AppConf.(Int) -> AppConf,
        enabled: (AppConf) -> Boolean,
        val min: Int?,
        val max: Int?
    ) : PreferencesItem<Int>(title, description, defaultValue, select, update, enabled)

    class FloatInput(
        title: Strings,
        description: Strings?,
        defaultValue: Float,
        select: (AppConf) -> Float,
        update: AppConf.(Float) -> AppConf,
        enabled: (AppConf) -> Boolean,
        val min: Float?,
        val max: Float?
    ) : PreferencesItem<Float>(title, description, defaultValue, select, update, enabled)

    class ColorStringInput(
        title: Strings,
        description: Strings?,
        defaultValue: String,
        select: (AppConf) -> String,
        update: AppConf.(String) -> AppConf,
        enabled: (AppConf) -> Boolean,
        val useAlpha: Boolean
    ) : PreferencesItem<String>(title, description, defaultValue, select, update, enabled)

    class Selection<T>(
        title: Strings,
        description: Strings?,
        defaultValue: T,
        select: (AppConf) -> T,
        update: AppConf.(T) -> AppConf,
        enabled: (AppConf) -> Boolean,
        val options: Array<T>
    ) : PreferencesItem<T>(title, description, defaultValue, select, update, enabled)

    class Keymap<T : Any>(
        val actionType: ActionType,
        defaultValue: List<ActionKeyBind<T>>,
        select: (AppConf) -> List<ActionKeyBind<T>>,
        update: AppConf.(List<ActionKeyBind<T>>) -> AppConf,
    ) : PreferencesItem<List<ActionKeyBind<T>>>(null, null, defaultValue, select, update, { true })
}
