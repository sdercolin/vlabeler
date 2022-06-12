package com.sdercolin.vlabeler.model

import kotlinx.serialization.Serializable

/**
 * Main configuration of the labeler
 */
@Serializable
data class LabelerConf(
    val name: String,
    val description: String,
    val fields: List<Field>,
    val properties: List<Property>
) {
    @Serializable
    data class Field(
        val index: Int, // Starts from 0
        val name: String,
        val abbr: String, // Displayed
        val color: String, // In format of "#ffffff"
        val height: Float, // 0~1
        val filling: Int?, // Index of the target field; -2 is start, -1 is end
        val constraints: List<Constraint>
    )

    @Serializable
    data class Property(
        val name: String, // Displayed
        val value: String // Calculated by fields ({\d}); -2 is start, -1 is end
    )

    @Serializable
    data class Constraint(
        val min: Int?, // Index of field (except for start and end)
        val max: Int?
    )
}