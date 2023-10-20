package com.sdercolin.vlabeler.model

import com.sdercolin.vlabeler.io.getPropertyMap
import com.sdercolin.vlabeler.ui.string.*
import com.sdercolin.vlabeler.util.JavaScript
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Model for an entry selector used in a [Plugin.Type.Macro] plugin.
 *
 * @property filters the filters to apply to the entry selector.
 * @property rawExpression the raw expression of the entry selector, if null, all filters are combined with `and`.
 */
@Serializable
data class EntrySelector(
    val filters: List<FilterItem>,
    @SerialName("expression") val rawExpression: String? = null,
) {

    @Transient
    private var _expression: LogicalExpression? = null

    private val expression: LogicalExpression?
        get() {
            if (rawExpression == null) return null
            if (_expression == null) {
                _expression = LogicalExpression.parse(rawExpression).getOrThrow()
            }
            return _expression
        }

    fun isValid(labelerConf: LabelerConf): Boolean {
        if (rawExpression != null) {
            if (runCatching { expression }.isFailure) return false
            if ((expression?.requiredPlaceholderCount ?: 0) > filters.size) return false
        }
        return filters.all { it.isValid(labelerConf) }
    }

    fun select(
        entries: List<Entry>,
        labelerConf: LabelerConf,
        js: JavaScript,
    ): List<Int> = entries.withIndex().filter { (_, entry) ->
        val filterResults = filters.map { it.accept(entry, labelerConf, js) }
        val expression = expression ?: LogicalExpression.default(filterResults.size)
        expression?.evaluate(filterResults) ?: true
    }.map { it.index }

    /**
     * Model for an item in the entry selector.
     */
    @Serializable
    sealed class FilterItem {

        abstract fun isValid(labelerConf: LabelerConf): Boolean

        abstract fun accept(entry: Entry, labelerConf: LabelerConf, js: JavaScript): Boolean

        /**
         * The name of the property being filtered. `sample`, `name`, `tag`, `star`, `done` and `script` are preserved.
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
                TEXT_ITEM_SUBJECT_ENTRY_NAME -> entry.name
                TEXT_ITEM_SUBJECT_SAMPLE_NAME -> entry.sampleNameWithoutExtension
                TEXT_ITEM_SUBJECT_TAG_NAME -> entry.notes.tag
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

    @Serializable
    @SerialName("boolean")
    data class BooleanFilterItem(
        override val subject: String,
        val matcherBoolean: Boolean,
    ) : FilterItem() {
        override fun isValid(labelerConf: LabelerConf): Boolean {
            return subject in booleanItemSubjects.map { it.first }
        }

        override fun accept(entry: Entry, labelerConf: LabelerConf, js: JavaScript): Boolean {
            val subjectValue = when (subject) {
                BOOLEAN_ITEM_SUBJECT_DONE -> entry.notes.done
                BOOLEAN_ITEM_SUBJECT_STAR -> entry.notes.star
                else -> throw IllegalArgumentException("Unknown subject name as boolean: $subject")
            }
            return subjectValue == matcherBoolean
        }
    }

    @Serializable
    @SerialName("script")
    data class ScriptFilterItem(
        val script: String, // a JavaScript expression of a function (entry, labeler) => boolean
    ) : FilterItem() {

        override val subject: String = SCRIPT_ITEM_SUBJECT

        override fun isValid(labelerConf: LabelerConf) = true

        override fun accept(entry: Entry, labelerConf: LabelerConf, js: JavaScript): Boolean {
            if (script.isBlank()) return true
            js.setJson("entry", entry)
            js.setJson("labeler", labelerConf)
            val propertyMap = labelerConf.getPropertyMap(entry, js)
            for ((property, value) in propertyMap) {
                js.eval("entry.${property.name} = $value")
            }
            val result = js.eval(script) ?: throw IllegalStateException("Evaluated script is null: $script")
            if (result.isBoolean.not()) {
                throw IllegalStateException("Evaluated script is not boolean: $script -> $result")
            }
            return result.asBoolean()
        }
    }

    companion object {
        val textItemSubjects
            get() = listOf(
                TEXT_ITEM_SUBJECT_ENTRY_NAME to Strings.PluginEntrySelectorPreservedSubjectName,
                TEXT_ITEM_SUBJECT_SAMPLE_NAME to Strings.PluginEntrySelectorPreservedSubjectSample,
                TEXT_ITEM_SUBJECT_TAG_NAME to Strings.PluginEntrySelectorPreservedSubjectTag,
            )

        val booleanItemSubjects
            get() = listOf(
                BOOLEAN_ITEM_SUBJECT_DONE to Strings.PluginEntrySelectorPreservedSubjectDone,
                BOOLEAN_ITEM_SUBJECT_STAR to Strings.PluginEntrySelectorPreservedSubjectStar,
            )

        val scriptItemSubjects
            get() = listOf(
                SCRIPT_ITEM_SUBJECT to Strings.PluginEntrySelectorPreservedSubjectScript,
            )

        private const val TEXT_ITEM_SUBJECT_ENTRY_NAME = "name"
        private const val TEXT_ITEM_SUBJECT_SAMPLE_NAME = "sample"
        private const val TEXT_ITEM_SUBJECT_TAG_NAME = "tag"
        private const val BOOLEAN_ITEM_SUBJECT_DONE = "done"
        private const val BOOLEAN_ITEM_SUBJECT_STAR = "star"
        private const val SCRIPT_ITEM_SUBJECT = "script"
    }
}
