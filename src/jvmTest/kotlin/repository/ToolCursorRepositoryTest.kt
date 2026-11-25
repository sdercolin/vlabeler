package repository

import androidx.compose.ui.res.useResource
import com.sdercolin.vlabeler.repository.ToolCursorRepository
import com.sdercolin.vlabeler.ui.editor.Tool
import java.awt.Cursor
import java.awt.GraphicsEnvironment
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for [ToolCursorRepository].
 */
class ToolCursorRepositoryTest {

    @Test
    fun testCursorToolUsesDefaultCursor() {
        // Tool.Cursor has no cursor image, so the default system cursor is used
        assertEquals(Cursor.getDefaultCursor(), ToolCursorRepository.get(Tool.Cursor))
    }

    @Test
    fun testCursorImagesExistWithExpectedSize() {
        // the hotspot is hard-coded to the center of a 24x24 image, so all cursor images must have that size
        Tool.values().mapNotNull { it.cursorPath }.forEach { path ->
            val image = assertNotNull(useResource(path) { ImageIO.read(it) }, "Cannot read cursor image: $path")
            assertEquals(24, image.width, "Unexpected width of cursor image: $path")
            assertEquals(24, image.height, "Unexpected height of cursor image: $path")
        }
    }

    @Test
    fun testCustomCursorCreation() {
        if (GraphicsEnvironment.isHeadless()) {
            // custom cursors cannot be created in a headless environment
            return
        }
        Tool.values().filter { it.cursorPath != null }.forEach { tool ->
            val cursor = ToolCursorRepository.get(tool)
            assertEquals(tool.name, cursor.name)
        }
    }
}
