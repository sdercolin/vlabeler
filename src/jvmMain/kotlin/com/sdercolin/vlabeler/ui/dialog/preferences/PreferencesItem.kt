package com.sdercolin.vlabeler.ui.dialog.preferences

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.action.Action
import com.sdercolin.vlabeler.model.action.ActionKeyBind
import com.sdercolin.vlabeler.model.action.ActionType
import com.sdercolin.vlabeler.ui.AppState
import com.sdercolin.vlabeler.ui.string.*

sealed class PreferencesItem(
    val title: Strings?,
    val description: Strings?,
    val clickableTags: List<ClickableTag>,
    val columnStyle: Boolean,
    val enabled: (AppConf) -> Boolean,
) {
    class Button(
        title: Strings,
        description: Strings?,
        val buttonText: Strings,
        val onClick: (AppState) -> Unit,
        val isDangerous: Boolean,
        clickableTags: List<ClickableTag>,
        enabled: (AppConf) -> Boolean,
    ) : PreferencesItem(
        title,
        description,
        clickableTags,
        columnStyle = false,
        enabled,
    )

    sealed class Valued<T>(
        title: Strings?,
        description: Strings?,
        clickableTags: List<ClickableTag>,
        columnStyle: Boolean,
        val defaultValue: T,
        val select: (AppConf) -> T,
        val update: AppConf.(T) -> AppConf,
        enabled: (AppConf) -> Boolean,
        val validationRules: List<PreferencesItemValidationRule>,
    ) : PreferencesItem(
        title,
        description,
        clickableTags,
        columnStyle,
        enabled,
    ) {

        fun getInvalidPrompt(conf: AppConf, newValue: T): Strings? {
            val updated = conf.update(newValue)
            return validationRules.firstOrNull { !it.validate(updated) }?.prompt
        }

        fun reset(conf: AppConf) = update(conf, defaultValue)
    }

    class Switch(
        title: Strings,
        description: Strings?,
        clickableTags: List<ClickableTag>,
        columnStyle: Boolean,
        defaultValue: Boolean,
        select: (AppConf) -> Boolean,
        update: AppConf.(Boolean) -> AppConf,
        enabled: (AppConf) -> Boolean,
    ) : Valued<Boolean>(
        title,
        description,
        clickableTags,
        columnStyle,
        defaultValue,
        select,
        update,
        enabled,
        validationRules = listOf(),
    )

    class IntegerInput(
        title: Strings,
        description: Strings?,
        clickableTags: List<ClickableTag>,
        columnStyle: Boolean,
        defaultValue: Int,
        select: (AppConf) -> Int,
        update: AppConf.(Int) -> AppConf,
        enabled: (AppConf) -> Boolean,
        validationRules: List<PreferencesItemValidationRule>,
        val min: Int?,
        val max: Int?,
    ) : Valued<Int>(
        title,
        description,
        clickableTags,
        columnStyle,
        defaultValue,
        select,
        update,
        enabled,
        validationRules,
    )

    class FloatInput(
        title: Strings,
        description: Strings?,
        clickableTags: List<ClickableTag>,
        columnStyle: Boolean,
        defaultValue: Float,
        select: (AppConf) -> Float,
        update: AppConf.(Float) -> AppConf,
        enabled: (AppConf) -> Boolean,
        validationRules: List<PreferencesItemValidationRule>,
        val min: Float?,
        val max: Float?,
    ) : Valued<Float>(
        title,
        description,
        clickableTags,
        columnStyle,
        defaultValue,
        select,
        update,
        enabled,
        validationRules,
    )

    class StringInput(
        title: Strings,
        description: Strings?,
        clickableTags: List<ClickableTag>,
        columnStyle: Boolean,
        defaultValue: String,
        select: (AppConf) -> String,
        update: AppConf.(String) -> AppConf,
        enabled: (AppConf) -> Boolean,
        validationRules: List<PreferencesItemValidationRule>,
    ) : Valued<String>(
        title,
        description,
        clickableTags,
        columnStyle,
        defaultValue,
        select,
        update,
        enabled,
        validationRules,
    )

    class ColorStringInput(
        title: Strings,
        description: Strings?,
        clickableTags: List<ClickableTag>,
        columnStyle: Boolean,
        defaultValue: String,
        select: (AppConf) -> String,
        update: AppConf.(String) -> AppConf,
        enabled: (AppConf) -> Boolean,
        val useAlpha: Boolean,
    ) : Valued<String>(
        title,
        description,
        clickableTags,
        columnStyle,
        defaultValue,
        select,
        update,
        enabled,
        validationRules = listOf(),
    )

    class Selection<T>(
        title: Strings?,
        description: Strings?,
        clickableTags: List<ClickableTag>,
        columnStyle: Boolean,
        defaultValue: T,
        select: (AppConf) -> T,
        update: AppConf.(T) -> AppConf,
        enabled: (AppConf) -> Boolean,
        val options: Array<T>,
    ) : Valued<T>(
        title,
        description,
        clickableTags,
        columnStyle,
        defaultValue,
        select,
        update,
        enabled,
        validationRules = listOf(),
    )

    class Keymap<K : Action>(
        val actionType: ActionType,
        defaultValue: List<ActionKeyBind<K>>,
        select: (AppConf) -> List<ActionKeyBind<K>>,
        update: AppConf.(List<ActionKeyBind<K>>) -> AppConf,
    ) : Valued<List<ActionKeyBind<K>>>(
        title = null,
        description = null,
        clickableTags = listOf(),
        columnStyle = false,
        defaultValue = defaultValue,
        select = select,
        update = update,
        enabled = { true },
        validationRules = listOf(),
    )
}
