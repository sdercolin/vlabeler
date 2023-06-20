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
                moveTo(8.0f, 20.0f)
                lineToRelative(10.0f, -10.0f)
                lineToRelative(-10.0f, -10.0f)
                verticalLineToRelative(20.0f)
                close()
            }
        }
        return arrowRightVector!!
    }

private var arrowRightVector: ImageVector? = null
