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
     * Name of the labeler to be displayed
     */
    val name: String,
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
    val lockedDrag: LockedDrag = LockedDrag()
) {

    val connectedConstraints: List<Pair<Int, Int>> = fields
        .flatMap { field ->
            field.constraints.flatMap { constraint ->
                val min = constraint.min?.let { it to field.index }
                val max = constraint.max?.let { field.index to it }
                listOfNotNull(min, max)
            }
        }
        .distinct()

    @Serializable
    @Immutable
    data class Field(
        val index: Int, // Starts from 0
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
}
