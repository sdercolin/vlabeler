package com.sdercolin.vlabeler.ui.dialog.preferences

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.ui.string.Strings

sealed class PreferencesItem<T>(
    val title: Strings,
    val description: Strings?,
    val defaultValue: T,
    val select: (AppConf) -> T,
    val update: AppConf.(T) -> AppConf
) {

    fun reset(conf: AppConf) = update(conf, defaultValue)

    class Switch(
        title: Strings,
        description: Strings?,
        defaultValue: Boolean,
        select: (AppConf) -> Boolean,
        update: AppConf.(Boolean) -> AppConf
    ) : PreferencesItem<Boolean>(title, description, defaultValue, select, update)

    class IntegerInput(
        title: Strings,
        description: Strings?,
        defaultValue: Int,
        select: (AppConf) -> Int,
        update: AppConf.(Int) -> AppConf,
        val min: Int?,
        val max: Int?
    ) : PreferencesItem<Int>(title, description, defaultValue, select, update)

    class FloatInput(
        title: Strings,
        description: Strings?,
        defaultValue: Float,
        select: (AppConf) -> Float,
        update: AppConf.(Float) -> AppConf,
        val min: Float?,
        val max: Float?
    ) : PreferencesItem<Float>(title, description, defaultValue, select, update)

    class ColorStringInput(
        title: Strings,
        description: Strings?,
        defaultValue: String,
        select: (AppConf) -> String,
        update: AppConf.(String) -> AppConf,
        val useAlpha: Boolean
    ) : PreferencesItem<String>(title, description, defaultValue, select, update)

    class Selection<T>(
        title: Strings,
        description: Strings?,
        defaultValue: T,
        select: (AppConf) -> T,
        update: AppConf.(T) -> AppConf,
        val options: Array<T>
    ) : PreferencesItem<T>(title, description, defaultValue, select, update)
}
