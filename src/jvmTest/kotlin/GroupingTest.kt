import com.sdercolin.vlabeler.util.groupContinuouslyBy
import kotlin.test.Test
import kotlin.test.assertContentEquals

/**
 * Tests for [groupContinuouslyBy].
 */
class GroupingTest {

    private fun createdIndexedItem(list: List<Int>) = list.map { IndexedValue(it, it) }

    @Test
    fun testGroupContinuouslyByToOneGroup() {
        val items = createdIndexedItem(listOf(3, 2, 1, 4, 5, 6, 7, 8, 9))
        val expected = listOf(createdIndexedItem(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)))
        val actual = items.groupContinuouslyBy { index }
        assertContentEquals(expected, actual)
    }

    @Test
    fun testGroupContinuouslyByToTwoGroups() {
        val items = createdIndexedItem(listOf(3, 2, 1, 4, 5, 6, 7, 8, 11, 9, 12, 13))
        val expected = listOf(
            createdIndexedItem(listOf(1, 2, 3, 4, 5, 6, 7, 8, 9)),
            createdIndexedItem(listOf(11, 12, 13)),
        )
        val actual = items.groupContinuouslyBy { index }
        assertContentEquals(expected, actual)
    }
}
