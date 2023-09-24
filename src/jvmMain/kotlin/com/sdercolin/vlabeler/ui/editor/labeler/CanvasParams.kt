package com.sdercolin.vlabeler.ui.editor.labeler

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.util.roundPixels

@Stable
data class CanvasParams(
    val dataLength: Int,
    val chunkCount: Int,
    val resolution: Int,
) {
    val lengthInPixel = dataLength.toFloat() / resolution

    private val chunkWidths = List(chunkCount) { lengthInPixel / chunkCount }.roundPixels()

    fun getChunkWidthInDp(index: Int, density: Density) = with(density) {
        chunkWidths[index].toDp()
    }

    data class LazyRowScrollTarget(
        val itemIndex: Int,
        val itemOffset: Int,
    )

    fun getScrollTarget(value: Int): LazyRowScrollTarget {
        var offset = value
        var index = 0
        while (offset >= chunkWidths[index]) {
            offset -= chunkWidths[index]
            index++
            if (index >= chunkCount) {
                index = chunkCount - 1
                offset = chunkWidths[index]
                break
            }
        }
        return LazyRowScrollTarget(index, offset)
    }

    @Immutable
    class ResolutionRange(
        private val conf: AppConf.CanvasResolution,
    ) {

        fun canIncrease(resolution: Int) = resolution < conf.max
        fun canDecrease(resolution: Int) = resolution > conf.min
        fun increaseFrom(resolution: Int) = resolution.plus(conf.step).coerceAtMost(conf.max)
        fun decreaseFrom(resolution: Int) = resolution.minus(conf.step).coerceAtLeast(conf.min)
    }
}
