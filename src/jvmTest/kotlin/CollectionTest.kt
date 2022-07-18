import com.sdercolin.vlabeler.util.splitAveragely
import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionTest {

    @Test
    fun testSplitAveragely() {
        val source = List(17) { it }
        val actual = source.splitAveragely(5)
        val expected = listOf(
            listOf(0, 1, 2, 3),
            listOf(4, 5, 6, 7),
            listOf(8, 9, 10),
            listOf(11, 12, 13),
            listOf(14, 15, 16)
        )
        assertEquals(expected, actual)
    }
}
