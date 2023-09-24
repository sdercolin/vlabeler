package com.sdercolin.vlabeler.ui.editor.labeler.timescale

object Timescale {

    private val items: List<TimescaleItem> by lazy {
        listOf(
            0.0005 to 0.0001,
            0.001 to 0.0005,
            0.005 to 0.001,
            0.01 to 0.005,
            0.05 to 0.01,
            0.1 to 0.05,
            0.5 to 0.1,
            1.0 to 0.5,
            5.0 to 1.0,
            15.0 to 5.0,
            30.0 to 10.0,
            60.0 to 15.0,
            60.0 to 30.0,
            5 * 60.0 to 60.0,
            15 * 60.0 to 5 * 60.0,
            30 * 60.0 to 10 * 60.0,
            60 * 60.0 to 15 * 60.0,
            60 * 60.0 to 30 * 60.0,
        ).map { TimescaleItem(it.first * 1000, it.second * 1000) }
    }

    fun find(convertTimeMillisToPx: (Float) -> Float): TimescaleItem = items.firstOrNull {
        convertTimeMillisToPx(it.minor.toFloat()) >= MIN_MINOR_STEP_PX
    } ?: items.last()

    private const val MIN_MINOR_STEP_PX = 80
}

/**
 * A timescale item is a pair of major and minor time unit in seconds.
 *
 * @property major major time unit in milliseconds, with label text displayed
 * @property minor minor time unit in milliseconds, without label text displayed
 */
class TimescaleItem(
    val major: Double,
    val minor: Double,
)
