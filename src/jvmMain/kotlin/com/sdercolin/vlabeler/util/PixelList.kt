package com.sdercolin.vlabeler.util

/**
 * Round a list of float to a list of int, keeping the sum of the list as close as possible to the sum of the float
 * list.
 *
 * The purpose of this function is to avoid the sum of the list to be changed after rounding. On Windows/Linux, only
 * integer pixels are supported, so we need to round the pixel floats to integers, however, we should keep every sum of
 * the first n pixel integers as close as possible to the sum of the first n pixel floats, so that we don't get a great
 * shift in pixels when we have a large list of chunks.
 */
fun List<Float>.roundPixels(): List<Int> {
    var remain = 0f
    val result = mutableListOf<Int>()

    for (i in indices) {
        val value = this[i]
        val valueWithRemain = value + remain
        val valueInt = valueWithRemain.toInt()
        remain = valueWithRemain - valueInt
        result.add(valueInt)
    }

    return result
}
