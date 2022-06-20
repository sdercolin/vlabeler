package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Configuration of the labeler's appearances and behaviors
 */
@Serializable
@Immutable
data class LabelerConf(
    /**
     * Unique name of the labeler
     * Names ending with ".default" represent that it's a built-in labeler
     */
    val name: String,
    /**
     * Version code in integer
     * Configurations with same [name] and [version] should always have same contents
     */
    val version: Int = 1,
    /**
     * File extension of the raw label file
     */
    val extension: String,
    /**
     * Default name of the input file relative to the sample directory
     */
    val defaultInputFilePath: String,
    val displayedName: String = name,
    val author: String = "",
    val description: String = "",
    /**
     * Default value listed as [start, *fields, end] in millisecond
     */
    val defaultValues: List<Float> = listOf(100f, 200f),
    /**
     * Fields defined expect for built-in "start" and "end"
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
     * Defines how data from the original format are parsed
     */
    val parser: Parser,
    /**
     * Defines how to write content in the original format
     */
    val writer: Writer
) {

    val connectedConstraints: List<Pair<Int, Int>> = fields.withIndex()
        .flatMap { field ->
            field.value.constraints.flatMap { constraint ->
                val min = constraint.min?.let { it to field.index }
                val max = constraint.max?.let { field.index to it }
                listOfNotNull(min, max)
            }
        }
        .distinct()

    @Serializable
    @Immutable
    data class Field(
        val name: String,
        val label: String, // Displayed
        val color: String, // In format of "#ffffff"
        val height: Float, // 0~1
        val dragBase: Boolean = false,
        val filling: Int? = null, // Index of the target field; -2 is start, -1 is end
        val constraints: List<Constraint> = listOf()
    )

    @Serializable
    @Immutable
    data class Constraint(
        val min: Int? = null, // Index of field (except for start and end)
        val max: Int? = null
    )

    @Serializable
    @Immutable
    data class LockedDrag(
        val useDragBase: Boolean = false,
        val useStart: Boolean = false
    )

    /**
     * Definition for parsing the raw label file to local [Entry]
     */
    @Serializable
    @Immutable
    data class Parser(
        /**
         * Default text encoding of the input file
         */
        val defaultEncoding: String,
        /**
         * Regex pattern that extract groups
         */
        val extractionPattern: String,
        /**
         * Definition of how the extracted string groups will be put into variables later in the parser python code.
         * Should be in the same order as the extracted groups
         */
        val variableNames: List<String>,
        /**
         *  Python script in lines that sets properties of [Entry] using the variables extracted, including:
         *  String "name"
         *  Float "start"
         *  Float "end"
         *  Float List "points"
         *  String "sample" (sample file name without extension)
         *
         *  If "sample" is set empty, the first sample file is used by all entries in case all entries are bound to the
         *  only one sample file, so the file name doesn't exist in the line
         *
         *  String values with names defined in [variableNames] are available
         */
        val scripts: List<String>,
    )

    /**
     * Definition for line format in the raw label file
     */
    @Serializable
    @Immutable
    data class Writer(
        /**
         * Properties that are used
         */
        val properties: List<Property> = listOf(),
        /**
         * String format using the following variables written as "{<var_name>}":
         * {sample} - sample file name without extension
         * {name} - entry name
         * {start} - [Entry.start]
         * {end} - [Entry.end]
         * {[Property.name]} - Evaluated value of a [Property]
         * {[Field.name]} - value in [Entry.points] with the same index of the corresponding [Field]
         * {<item in [extraFieldNames]>} - value saved in
         *
         * If a name is shared by a [Property] and [Field], it's considered as [Property].
         *
         * @sample "{sample}.wav:{name}={start},{middle},{end}" will be written like "a.wav:a:100,220.5,300"
         */
        val format: String? = null,
        /**
         * Python scripts text lines that sets "output" variable using the same variables as described in [format]
         * String type: sample, name
         * Float type: start, end, and others
         *
         * Either [format] or [scripts] should be given. If both of them are given, [scripts] is used
         */
        val scripts: List<String>? = null
    )

    /**
     * Definition of properties that will be written to the raw label file
     */
    @Serializable
    @Immutable
    data class Property(
        val name: String,
        /**
         * Mathematical expression including fields written as "{[Field.name]}" and "{start}", "{end}".
         * Extra fields of number type defined in [extraFieldNames] are also available.
         * Expression is calculated by Python's "eval()"
         */
        val value: String
    )

    companion object {
        const val LabelerFileExtension = "labeler.json"
    }
}
