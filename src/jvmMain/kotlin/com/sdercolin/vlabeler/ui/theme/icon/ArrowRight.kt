package com.sdercolin.vlabeler.ui.theme.icon

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

val ToolPlaybackArrowRight: ImageVector
    get() {
        if (arrowRightVector != null) {
            return arrowRightVector!!
        }
        arrowRightVector = materialIcon(name = "Filled.ArrowRight") {
            materialPath {
                moveTo(9f, 20f)
                lineToRelative(9f, -9f)
                lineToRelative(-9f, -9f)
                verticalLineToRelative(18f)
                close()
            }
        }
        return arrowRightVector!!
    }

private var arrowRightVector: ImageVector? = null
