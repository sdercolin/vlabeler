import com.sdercolin.vlabeler.util.roundPixels
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [roundPixels].
 */
class PixelListTest {

    @Test
    fun testRoundPixelListAllSame() {
        val pixelFloatList = listOf(1.3f, 1.3f, 1.3f, 1.3f, 1.3f, 1.3f, 1.3f)
        val expectedIntList = listOf(1, 1, 1, 2, 1, 1, 2)

        val actual = pixelFloatList.roundPixels()

        assertEquals(expectedIntList, actual)
    }

    @Test
    fun testRoundPixelListAllDifferentDecimal() {
        val pixelFloatList = listOf(1.3f, 1.4f, 1.5f, 1.6f, 1.7f, 1.8f, 1.9f)
        val expectedIntList = listOf(1, 1, 2, 1, 2, 2, 2)

        val actual = pixelFloatList.roundPixels()

        assertEquals(expectedIntList, actual)
    }
}
