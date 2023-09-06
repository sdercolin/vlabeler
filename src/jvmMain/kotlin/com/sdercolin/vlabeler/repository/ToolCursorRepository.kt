package com.sdercolin.vlabeler.repository

import androidx.compose.ui.res.useResource
import com.sdercolin.vlabeler.ui.editor.Tool
import java.awt.Cursor
import java.awt.Image
import java.awt.Point
import java.awt.Toolkit
import javax.imageio.ImageIO

/**
 * Repository for tool cursors.
 */
object ToolCursorRepository {
    private val map: MutableMap<Tool, Image> = mutableMapOf()

    /**
     * Get cursor for the given tool.
     */
    fun get(tool: Tool): Cursor {
        val path = tool.cursorPath ?: return Cursor.getDefaultCursor()
        val image = map.getOrPut(tool) {
            useResource(path) {
                ImageIO.read(it)
            }
        }
        val point = Point(IMAGE_SIZE_PIXEL / 2, IMAGE_SIZE_PIXEL / 2)
        return Toolkit.getDefaultToolkit().createCustomCursor(image, point, tool.name)
    }

    private const val IMAGE_SIZE_PIXEL = 24
}
