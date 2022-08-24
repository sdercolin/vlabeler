package com.sdercolin.vlabeler.model

import com.sdercolin.vlabeler.io.getPropertyMap
import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.util.JavaScript
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Model for an entry selector used in a [Plugin.Type.Macro] plugin.
 * @param filters the filters to apply to the entry selector
 */
@Serializable
data class EntrySelector(
    val filters: List<FilterItem>,
) {

    fun select(entries: List<Entry>, labelerConf: LabelerConf, js: JavaScript) = filters
        .fold(entries.withIndex().toList()) { acc, filter ->
            filter.filter(acc, labelerConf, js)
        }
        .map { it.index }

    /**
     * Model for an item in the entry selector.
     */
    @Serializable
    sealed class FilterItem {

        abstract fun isValid(labelerConf: LabelerConf): Boolean

        fun filter(
            entries: List<IndexedValue<Entry>>,
            labelerConf: LabelerConf,
            js: JavaScript,
        ): List<IndexedValue<Entry>> = entries.filter { accept(it.value, labelerConf, js) }

        abstract fun accept(entry: Entry, labelerConf: LabelerConf, js: JavaScript): Boolean

        /**
         * The name of the property being filtered. `sample` and `name` are preserved.
         */
        abstract val subject: String
    }

    @Serializable
    @SerialName("text")
    data class TextFilterItem(
        override val subject: String,
        val matchType: TextMatchType,
        val matcherText: String,
    ) : FilterItem() {

        override fun isValid(labelerConf: LabelerConf): Boolean {
            if (subject !in textItemSubjects.map { it.first }) return false
            if (matchType == TextMatchType.Regex) {
                runCatching { matcherText.toRegex() }.getOrElse { return false }
            }
            return matcherText.isNotEmpty()
        }

        override fun accept(entry: Entry, labelerConf: LabelerConf, js: JavaScript): Boolean {
            val subjectValue = when (subject) {
                TextItemSubjectEntryName -> entry.name
                TextItemSubjectSampleName -> entry.sample
                else -> throw IllegalArgumentException("Unknown subject name as text: $subject")
            }
            return when (matchType) {
                TextMatchType.Equals -> subjectValue == matcherText
                TextMatchType.Contains -> subjectValue.contains(matcherText)
                TextMatchType.StartsWith -> subjectValue.startsWith(matcherText)
                TextMatchType.EndsWith -> subjectValue.endsWith(matcherText)
                TextMatchType.Regex -> subjectValue.matches(matcherText.toRegex())
            }
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
    @SerialName("number")
    data class NumberFilterItem(
        override val subject: String,
        val matchType: NumberMatchType,
        val comparerValue: Double,
        val comparerName: String?,
    ) : FilterItem() {

        override fun isValid(labelerConf: LabelerConf): Boolean {
            val propertyNames = labelerConf.properties.map { it.name }
            if (subject !in propertyNames) return false
            if (comparerName != null && comparerName !in propertyNames) return false
            return true
        }

        override fun accept(entry: Entry, labelerConf: LabelerConf, js: JavaScript): Boolean {
            val propertyMap = labelerConf.getPropertyMap(entry, js)
            val subjectProperty = labelerConf.properties.find { it.name == subject }
                ?: throw IllegalArgumentException("Unknown subject name as number: $subject")
            val subjectValue = propertyMap.getValue(subjectProperty)
            val comparerProperty = if (comparerName != null) {
                labelerConf.properties.find { it.name == comparerName }
                    ?: throw IllegalArgumentException("Unknown comparer name as number: $comparerName")
            } else null
            val comparerValue = if (comparerProperty != null) {
                propertyMap.getValue(comparerProperty)
            } else comparerValue
            return when (matchType) {
                NumberMatchType.Equals -> subjectValue == comparerValue
                NumberMatchType.GreaterThan -> subjectValue > comparerValue
                NumberMatchType.GreaterThanOrEquals -> subjectValue >= comparerValue
                NumberMatchType.LessThan -> subjectValue < comparerValue
                NumberMatchType.LessThanOrEquals -> subjectValue <= comparerValue
            }
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
                TextItemSubjectEntryName to Strings.PluginEntrySelectorPreservedSubjectName,
                TextItemSubjectSampleName to Strings.PluginEntrySelectorPreservedSubjectSample,
            )

        private const val TextItemSubjectEntryName = "name"
        private const val TextItemSubjectSampleName = "sample"
    }
}
