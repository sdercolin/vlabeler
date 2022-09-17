package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import com.sdercolin.vlabeler.model.LabelerConf.Property
import com.sdercolin.vlabeler.ui.string.LocalizedJsonString
import com.sdercolin.vlabeler.ui.string.toLocalized
import com.sdercolin.vlabeler.util.DefaultLabelerDir
import kotlinx.serialization.Serializable

/**
 * Configuration of the labeler's appearances and behaviors
 */
@Serializable
@Immutable
data class LabelerConf(
    /**
     * Unique name of the labeler
     */
    val name: String,
    /**
     * Version code in integer
     * Configurations with same [name] and [version] should always have same contents if distributed in public
     */
    val version: Int = 1,
    /**
     * File extension of the raw label file
     */
    val extension: String,
    /**
     * Default name of the input file relative to the sample directory
     */
    val defaultInputFilePath: String? = null,
    /**
     * Name displayed in the UI (localized)
     */
    val displayedName: LocalizedJsonString = name.toLocalized(),
    val author: String,
    val email: String = "",
    val description: LocalizedJsonString = "".toLocalized(),
    val website: String = "",
    /**
     * Continuous mode, where the end of entry is forced set to the start of its next entry
     */
    val continuous: Boolean = false,
    /**
     * Whether to allow more than one entry with a shared name in one sample
     */
    val allowSameNameEntry: Boolean = false,
    /**
     * Default value listed as [start, *fields, end] in millisecond
     */
    val defaultValues: List<Float>,
    /**
     * Default [extraFieldNames] values
     */
    val defaultExtras: List<String>,
    /**
     * Fields defined except for built-in "start" and "end".
     * Corresponds to [Entry.points]
     */
    val fields: List<Field> = listOf(),
    /**
     * Extra field names for some data only for calculation.
     * Saved as String
     */
    val extraFieldNames: List<String> = listOf(),
    /**
     * Defines when to use locked dragging (all parameters will move with dragged one)
     */
    val lockedDrag: LockedDrag = LockedDrag(),
    /**
     * Action taken when there are points before "start"
     */
    val overflowBeforeStart: PointOverflow = PointOverflow.Error,
    /**
     * Action taken when there are points after "end"
     */
    val overflowAfterEnd: PointOverflow = PointOverflow.Error,
    /**
     * Decimal digit count of the properties and writer
     */
    val decimalDigit: Int = 2,
    /**
     * Properties that are used in the following procedures. See [Property]
     */
    val properties: List<Property> = listOf(),
    /**
     * Defines how data from the original format are parsed
     */
    val parser: Parser,
    /**
     * Defines how to write content in the original format
     */
    val writer: Writer,
) {

    val fileName get() = "$name.$LabelerFileExtension"
    val isBuiltIn get() = DefaultLabelerDir.listFiles().orEmpty().any { it.name == fileName }

    /**
     * Get constraints for canvas usage
     * Pair<a, b> represents "a <= b"
     */
    val connectedConstraints: List<Pair<Int, Int>>
        get() = fields.withIndex()
            .flatMap { item ->
                item.value.constraints.flatMap { constraint ->
                    val min = constraint.min?.let { it to item.index }
                    val max = constraint.max?.let { item.index to it }
                    listOfNotNull(min, max)
                }
            }
            .distinct()

    /**
     * Custom field of the labeler
     * @param name Unique name of the field
     * @param label Label displayed in the UI (localized)
     * @param color Color code in format of "#ffffff"
     * @param height Label height ratio to the height of waveforms part (0~1)
     * @param dragBase True if all the other parameter line move together with this one
     * @param filling Name of the target field to which a filled area is drawn from this field. "start" and "end" are
     * also available
     * @param constraints Define value constraints between the fields. See [Constraint]
     * @param shortcutIndex Index in the shortcut list. Could be 1~8 (0 is reserved for "start")
     */
    @Serializable
    @Immutable
    data class Field(
        val name: String,
        val label: LocalizedJsonString,
        val color: String,
        val height: Float,
        val dragBase: Boolean = false,
        val filling: String? = null,
        val constraints: List<Constraint> = listOf(),
        val shortcutIndex: Int? = null,
    )

    /**
     * Except for "start" and "end" (all fields should be between "start" and "end").
     * You don't have to declare the same constraint in both two fields
     * @param min Index of the field that should be smaller or equal to this field
     * @param max Index of the field that should be greater or equal to this field
     */
    @Serializable
    @Immutable
    data class Constraint(
        val min: Int? = null,
        val max: Int? = null,
    )

    /**
     * Definition of when should all parameter lines move together when dragging
     * @param useDragBase True if locked drag is enabled when field with [Field.dragBase] == true is dragged
     * @param useStart True if locked drag is enabled when the start line is dragged
     */
    @Serializable
    @Immutable
    data class LockedDrag(
        val useDragBase: Boolean = false,
        val useStart: Boolean = false,
    )

    /**
     * Action to be taken when points are set outside borders (start/end)
     */
    @Serializable
    @Immutable
    enum class PointOverflow {
        /**
         * Adjust the border to contain the point
         */
        AdjustBorder,

        /**
         * Adjust the overflowing point to the border
         */
        AdjustPoint,

        /**
         * Do nothing, which leads to an error
         */
        Error
    }

    /**
     * Definition for parsing the raw label file to local [Entry]
     * @param defaultEncoding Default text encoding of the input file
     * @param extractionPattern Regex pattern that extract groups
     * @param variableNames Definition of how the extracted string groups will be put into variables later in the parser
     * JavaScript code. Should be in the same order as the extracted groups
     * @param scripts JavaScript code in lines that sets properties of [Entry] using the variables extracted
     */
    @Serializable
    @Immutable
    data class Parser(
        val defaultEncoding: String,
        val extractionPattern: String,
        val variableNames: List<String>,
        /**
         * Available input variables:
         * - String "inputFileName": Name of the input file without extension
         * - String List "sampleNames": Names of the samples without extension in the folder
         * - String "<item in [variableNames]>": Values extracted by [extractionPattern]
         *
         * Output variables that the scripts should set:
         * - String "sample" (sample file name without extension)
         * - String "name"
         * - Float "start" (in millisecond)
         * - Float "end" (in millisecond)
         * - Float List "points" (in millisecond)
         * - String List corresponding values defined in [extraFieldNames]
         *
         * If "sample" is not set, the first sample file is used by all entries in case all entries are bound to the
         * only one sample file, so the file name doesn't exist in the line.
         *
         * If "name" is not set, this entry is ignored.
         * If any of "start", "end" is not set, or points don't have a same size with [fields], this entry will fall
         * back to default values except the entry name.
         */
        val scripts: List<String>,
    )

    /**
     * Definition for line format in the raw label file
     * @param format String format to generate the output line
     * @param scripts JavaScript code in lines that generate the output line
     * Either [format] or [scripts] should be given. If both of them are given, [scripts] is used
     */
    @Serializable
    @Immutable
    data class Writer(
        /**
         * String format using the following variables written as "{<var_name>}":
         * {sample} - sample file name without extension
         * {name} - entry name
         * {start} - [Entry.start]
         * {end} - [Entry.end]
         * {[Property.name]} - Evaluated value of a [Property]
         * {[Field.name]} - value in [Entry.points] with the same index of the corresponding [Field]
         * {<item in [extraFieldNames]>} - value saved in [Parser]
         *
         * If a name is shared by a [Property] and [Field], it's considered as [Property].
         *
         * @sample "{sample}.wav:{name}={start},{middle},{end}" will be written like "a.wav:a:100,220.5,300"
         */
        val format: String? = null,
        /**
         * JavaScript code lines that sets "output" variable using the same variables as described in [format]
         * String type: sample, name, extras
         * Float type: start, end, and others
         */
        val scripts: List<String>? = null,
    )

    /**
     * Definition of properties that will be written to the raw label file
     * @param name Unique name of the property
     * @param displayedName Name displayed in property view UI (localized)
     * @param value Mathematical expression text including fields written as "{[Field.name]}" and "{start}", "{end}".
     * Extra fields of number type defined in [extraFieldNames] are also available. The expression is evaluated in
     * JavaScript.
     */
    @Serializable
    @Immutable
    data class Property(
        val name: String,
        val displayedName: LocalizedJsonString,
        val value: String,
    )

    companion object {
        const val LabelerFileExtension = "labeler.json"
    }
}
