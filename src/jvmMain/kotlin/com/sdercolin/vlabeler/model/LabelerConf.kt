package com.sdercolin.vlabeler.model

import kotlinx.serialization.Serializable

/**
 * Main configuration of the labeler
 */
@Serializable
data class LabelerConf(
    val name: String,
    val description: String = "",
    val fields: List<Field> = listOf(),
    val properties: List<Property> = listOf(),
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
    data class Property(
        val name: String, // Displayed
        val value: String // Calculated by fields ({\d}); -2 is start, -1 is end
    )

    @Serializable
    data class Constraint(
        val min: Int? = null, // Index of field (except for start and end)
        val max: Int? = null
    )

    @Serializable
    data class LockedDrag(
        val useDragBase: Boolean = false,
        val useStart: Boolean = false
    )
}