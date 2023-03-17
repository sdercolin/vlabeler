import com.sdercolin.vlabeler.util.getDefaultNewEntryName
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

/**
 * Tests for [getDefaultNewEntryName].
 */
class DefaultNewEntryNameTest {

    @Test
    fun testDefaultNewEntryNameNoExisting() {
        val base = "new_entry"
        val existingNames = listOf(base)
        val expected = "new_entry_2"
        val actual = getDefaultNewEntryName(base, existingNames, allowDuplicate = false)
        assertEquals(expected, actual)
    }

    @Test
    fun testDefaultNewEntryNameExisting() {
        val base = "new_entry"
        val existingNames = listOf(base, "new_entry_2")
        val expected = "new_entry_3"
        val actual = getDefaultNewEntryName(base, existingNames, allowDuplicate = false)
        assertEquals(expected, actual)
    }

    @Test
    fun testDefaultNewEntryNameExistingMultiple() {
        val base = "new_entry"
        val existingNames = listOf(base, "new_entry_2", "new_entry_3")
        val expected = "new_entry_4"
        val actual = getDefaultNewEntryName(base, existingNames, allowDuplicate = false)
        assertEquals(expected, actual)
    }

    @Test
    fun testDefaultNewEntryNameExistingMultipleWithGap() {
        val base = "new_entry"
        val existingNames = listOf(base, "new_entry_2", "new_entry_4")
        val expected = "new_entry_3"
        val actual = getDefaultNewEntryName(base, existingNames, allowDuplicate = false)
        assertEquals(expected, actual)
    }

    @Test
    fun testDefaultNewEntryNameExistingMultipleAllowDuplicate() {
        val existingNames = listOf("new_entry")
        val base = "new_entry"
        val expected = "new_entry"
        val actual = getDefaultNewEntryName(base, existingNames, allowDuplicate = true)
        assertEquals(expected, actual)
    }
}
