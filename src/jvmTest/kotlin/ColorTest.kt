import androidx.compose.ui.graphics.Color
import com.sdercolin.vlabeler.util.argbHexString
import com.sdercolin.vlabeler.util.rgbHexString
import com.sdercolin.vlabeler.util.toColor
import com.sdercolin.vlabeler.util.toColorOrNull
import com.sdercolin.vlabeler.util.toHsv
import com.sdercolin.vlabeler.util.toRgbColor
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/**
 * Tests for src/jvmMain/kotlin/com/sdercolin/vlabeler/util/Color.kt.
 */
class ColorTest {

    @Test
    fun testParseRgb() {
        val expected = Color(0xffff0000)
        assertEquals(expected, "#FF0000".toColor())
        assertEquals(expected, "FF0000".toColor())
        assertEquals(expected, "ff0000".toColor())
        assertEquals(expected, "#ffff0000".toColor())
    }

    @Test
    fun testParseArgb() {
        val expected = Color(0xccff0000)
        assertEquals(expected, "#CCFF0000".toColor())
        assertEquals(expected, "CCFF0000".toColor())
        assertEquals(expected, "ccff0000".toColor())
        assertEquals(expected, "#ccff0000".toColor())
        assertEquals(null, "#ffwwff0000".toColorOrNull())
    }

    @Test
    fun testParseArgbDiscardingAlpha() {
        assertEquals(Color(0xffff0000), "#ccff0000".toRgbColor())
    }

    @Test
    fun testStringifyRgb() {
        val expected = "#FFAAAA"
        assertEquals(expected, Color(0xffffaaaa).rgbHexString)
    }

    @Test
    fun testStringifyArgb() {
        val expected = "#AAFFAAAA"
        assertEquals(expected, Color(0xaaffaaaa).argbHexString)
    }

    @Test
    fun testToHsv() {
        val colors = listOf(
            "#f286e2",
            "#37eea7",
            "#d29c9d",
            "#8d0e20",
            "#d2cbcc",
            "#0d1450",
            "#3ae7cb",
            "#06fc88",
        )
        for (color in colors) {
            val hsv = color.toColor().toHsv()
            val restored = Color.hsv(hsv[0], hsv[1], hsv[2])
            assertEquals(color.toColor(), restored)
        }
    }
}
