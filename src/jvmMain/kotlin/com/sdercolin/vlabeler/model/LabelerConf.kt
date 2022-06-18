package com.sdercolin.vlabeler.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable
import javax.swing.text.html.parser.Parser

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
     * Properties that are displayed in the entry list
     */
    val properties: List<Property> = listOf(),
    /**
     * Defines when to use locked dragging (all parameters will move with dragged one)
     */
    val lockedDrag: LockedDrag = LockedDrag(),
    /**
     * Defines how data from the original format are parsed
     */
    val parser: Parser? = null
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
        val abbr: String, // Displayed
        val color: String, // In format of "#ffffff"
        val height: Float, // 0~1
        val dragBase: Boolean = false,
        val filling: Int? = null, // Index of the target field; -2 is start, -1 is end
        val constraints: List<Constraint> = listOf()
    )

    @Serializable
    @Immutable
    data class Property(
        val name: String, // Displayed
        val value: String // Calculated by fields ({\d}); -2 is start, -1 is end
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
     * Definition for parsing the raw format to local [Entry]
     */
    @Serializable
    @Immutable
    data class Parser(
        /**
         * Available file extension for this parser
         */
        val extension: String,
        /**
         * Default name of the input file relative to the sample directory
         */
        val defaultInputFilePath: String,
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
        val parsingScript: List<String>,
    )
}
