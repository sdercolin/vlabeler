import com.sdercolin.vlabeler.util.matchGroups
import kotlin.test.Test
import kotlin.test.assertEquals

class RegexTest {

    @Test
    fun testGroupValues() {
        val source = "1_fix asdadsawavwed3.wav=e,3062.808,177.447,-177.447,0.0,0.0"
        val regex = Regex("(.*)\\.wav=(.*),(.*),(.*),(.*),(.*),(.*)")
        val expected = listOf("1_fix asdadsawavwed3", "e", "3062.808", "177.447", "-177.447", "0.0", "0.0")
        val actual = source.matchGroups(regex)
        assertEquals(expected, actual)
    }
}
