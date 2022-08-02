package com.sdercolin.vlabeler.model

import com.sdercolin.vlabeler.ui.string.Strings
import kotlinx.serialization.Serializable

/**
 * Model for an entry selector used in a [Plugin.Type.Macro] plugin.
 * @param filters the filters to apply to the entry selector
 */
@Serializable
data class EntrySelector(
    val filters: List<FilterItem>
) {

    /**
     * Model for an item in the entry selector.
     * @param subject The name of the property being filtered. `sample` and `name` are preserved.
     */
    @Serializable
    sealed class FilterItem {

        abstract fun isValid(labelerConf: LabelerConf): Boolean

        abstract val subject: String
    }

    @Serializable
    data class TextFilterItem(
        override val subject: String,
        val matchType: TextMatchType,
        val matcherText: String
    ) : FilterItem() {
        override fun isValid(labelerConf: LabelerConf): Boolean {
            if (subject !in textItemSubjects.map { it.first }) return false
            return matcherText.isNotEmpty()
        }
    }

    @Serializable
    enum class TextMatchType(val strings: Strings) {
        Equals(Strings.PluginEntrySelectorTextMatchTypeEquals),
        Contains(Strings.PluginEntrySelectorTextMatchTypeContains),
        StartsWith(Strings.PluginEntrySelectorTextMatchTypeStartsWith),
        EndsWith(Strings.PluginEntrySelectorTextMatchTypeEndsWith),
        Regex(Strings.PluginEntrySelectorTextMatchTypeRegex)
    }

    @Serializable
    data class NumberFilterItem(
        override val subject: String,
        val matchType: NumberMatchType,
        val absoluteComparerValue: Double,
        val comparerName: String?
    ) : FilterItem() {
        override fun isValid(labelerConf: LabelerConf): Boolean {
            val propertyNames = labelerConf.properties.map { it.name }
            if (subject !in propertyNames) return false
            if (comparerName != null && comparerName !in propertyNames) return false
            return true
        }
    }

    @Serializable
    enum class NumberMatchType(val strings: Strings) {
        Equals(Strings.PluginEntrySelectorNumberMatchTypeEquals),
        GreaterThan(Strings.PluginEntrySelectorNumberMatchTypeGreaterThan),
        LessThan(Strings.PluginEntrySelectorNumberMatchTypeLessThan),
        GreaterThanOrEquals(Strings.PluginEntrySelectorNumberMatchTypeGreaterThanOrEquals),
        LessThanOrEquals(Strings.PluginEntrySelectorNumberMatchTypeLessThanOrEquals)
    }

    companion object {
        val textItemSubjects
            get() = listOf(
                "sample" to Strings.PluginEntrySelectorPreservedSubjectSample,
                "name" to Strings.PluginEntrySelectorPreservedSubjectName
            )
    }
}
