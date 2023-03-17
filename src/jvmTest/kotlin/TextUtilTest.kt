import com.sdercolin.vlabeler.util.toStringTrimmed
import kotlin.test.Test
import kotlin.test.assertEquals

class TextUtilTest {

    @Test
    fun testFloatToStringTrimmed() {
        val sources = listOf(1f, 1.1f, 3.0f, 0f, 0.01f, 0.020f)
        val expected = listOf("1", "1.1", "3", "0", "0.01", "0.02")
        val actual = sources.map { it.toStringTrimmed() }
        assertEquals(expected, actual)
    }
}
