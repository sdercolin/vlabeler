import com.sdercolin.vlabeler.util.matchGroups
import com.sdercolin.vlabeler.util.replaceWithVariables
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [matchGroups] and [replaceWithVariables].
 */
class RegexTest {

    @Test
    fun testGroupValues() {
        val source = "1_fix asdadsawavwed3.wav=e,3062.808,177.447,-177.447,0.0,0.0"
        val regex = Regex("(.*)\\.wav=(.*),(.*),(.*),(.*),(.*),(.*)")
        val expected = listOf("1_fix asdadsawavwed3", "e", "3062.808", "177.447", "-177.447", "0.0", "0.0")
        val actual = source.matchGroups(regex)
        assertEquals(expected, actual)
    }

    @Test
    fun testReplaceWithVariables() {
        val variables = mapOf(
            "a" to 1.1,
            "b" to "sss",
            "c" to (1 to 2),
        )
        val template = "{a}...{b}..{c}"
        val expected = "1.1...sss..(1, 2)"
        val actual = template.replaceWithVariables(variables)
        assertEquals(expected, actual)
    }
}
