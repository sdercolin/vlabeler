import com.sdercolin.vlabeler.util.toStringTrimmed
import kotlin.test.Test
import kotlin.test.assertEquals

class TextUtilTest {

    @Test
    fun testFloatToStringTrimmed() {
        val sources = listOf(1f, 1.1f, 3.0f, 0f, 0.01f, 0.020f, 0.000005f)
        val expected = listOf("1", "1.1", "3", "0", "0.01", "0.02", "0.000005")
        val actual = sources.map { it.toStringTrimmed() }
        assertEquals(expected, actual)
    }

    @Test
    fun testDoubleToStringTrimmed() {
        val sources = listOf(1.0, 1.1, 3.0, 0.0, 0.01, 0.020, 0.000005)
        val expected = listOf("1", "1.1", "3", "0", "0.01", "0.02", "0.000005")
        val actual = sources.map { it.toStringTrimmed() }
        assertEquals(expected, actual)
    }
}
