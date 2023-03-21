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
    val density: Density,
) {
    val lengthInPixel = dataLength.toFloat() / resolution

    private val chunkWidths = List(chunkCount) { lengthInPixel / chunkCount }.roundPixels()

    fun getChunkWidthInDp(index: Int) = with(density) {
        chunkWidths[index].toDp()
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
