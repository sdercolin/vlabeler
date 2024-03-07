package model

import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.EntryListDiffItem.Add
import com.sdercolin.vlabeler.model.EntryListDiffItem.Edit
import com.sdercolin.vlabeler.model.EntryListDiffItem.Remove
import com.sdercolin.vlabeler.model.computeEntryListDiff
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class EntryListDiffTest {

    @Test
    fun `simple add`() {
        val old = BasicEntryList
        val new = BasicEntryList + Entry(
            sample = "6.wav",
            name = "entry1",
            start = 0.0f,
            end = 1.0f,
            points = listOf(),
            extras = listOf(),
        )
        val diff = computeEntryListDiff(old, new)
        assertContentEquals(
            listOf(
                Add(10, new[10]),
            ),
            diff.items,
        )
        assertEquals(
            BasicUnchangedMap,
            diff.unchangedIndexMap,
        )
    }

    @Test
    fun `multiple add`() {
        val old = BasicEntryList
        val new = old.toMutableList()
        new.add(
            0,
            Entry(
                sample = "6.wav",
                name = "entry1",
                start = 0.0f,
                end = 1.0f,
                points = listOf(),
                extras = listOf(),
            ),
        )
        new.add(
            8,
            Entry(
                sample = "6.wav",
                name = "entry2",
                start = 1.0f,
                end = 2.0f,
                points = listOf(),
                extras = listOf(),
            ),
        )
        new.add(
            Entry(
                sample = "6.wav",
                name = "entry3",
                start = 2.0f,
                end = 3.0f,
                points = listOf(),
                extras = listOf(),
            ),
        )
        val diff = computeEntryListDiff(old, new)
        assertContentEquals(
            listOf(
                Add(0, new[0]),
                Add(8, new[8]),
                Add(12, new[12]),
            ),
            diff.items,
        )
        val unchangedMap = mapOf(
            0 to 1,
            1 to 2,
            2 to 3,
            3 to 4,
            4 to 5,
            5 to 6,
            6 to 7,
            7 to 9,
            8 to 10,
            9 to 11,
        )
        assertEquals(
            unchangedMap,
            diff.unchangedIndexMap,
        )
    }

    @Test
    fun `simple remove`() {
        val old = BasicEntryList
        val new = BasicEntryList - BasicEntryList[0]
        val diff = computeEntryListDiff(old, new)
        assertContentEquals(
            listOf(
                Remove(0, old[0]),
            ),
            diff.items,
        )
        val unchangedMap = mapOf(
            1 to 0,
            2 to 1,
            3 to 2,
            4 to 3,
            5 to 4,
            6 to 5,
            7 to 6,
            8 to 7,
            9 to 8,
        )
        assertEquals(
            unchangedMap,
            diff.unchangedIndexMap,
        )
    }

    @Test
    fun `multiple remove`() {
        val old = BasicEntryList
        val new = old.toMutableList()
        new.removeAt(0)
        new.removeAt(7)
        new.removeAt(7)
        val diff = computeEntryListDiff(old, new)
        assertContentEquals(
            listOf(
                Remove(0, old[0]),
                Remove(8, old[8]),
                Remove(9, old[9]),
            ),
            diff.items,
        )
        val unchangedMap = mapOf(
            1 to 0,
            2 to 1,
            3 to 2,
            4 to 3,
            5 to 4,
            6 to 5,
            7 to 6,
        )
        assertEquals(
            unchangedMap,
            diff.unchangedIndexMap,
        )
    }

    @Test
    fun `multiple add and remove`() {
        val old = BasicEntryList
        var new = old.toMutableList()
        val newEntries = listOf(
            Entry(
                sample = "6.wav",
                name = "entry1",
                start = 0.0f,
                end = 1.0f,
                points = listOf(),
                extras = listOf(),
            ),
            Entry(
                sample = "6.wav",
                name = "entry2",
                start = 1.0f,
                end = 2.0f,
                points = listOf(),
                extras = listOf(),
            ),
            Entry(
                sample = "6.wav",
                name = "entry3",
                start = 2.0f,
                end = 3.0f,
                points = listOf(),
                extras = listOf(),
            ),
        )
        new.add(0, newEntries[0])
        new.add(8, newEntries[1])
        new.add(newEntries[2])
        new = (new - old[0] - old[3] - old[6]).toMutableList()
        val diff = computeEntryListDiff(old, new)
        assertContentEquals(
            listOf(
                Remove(0, old[0]),
                Add(0, newEntries[0]),
                Remove(3, old[3]),
                Add(5, newEntries[1]),
                Remove(6, old[6]),
                Add(9, newEntries[2]),
            ),
            diff.items,
        )
        val unchangedMap = mapOf(
            1 to 1,
            2 to 2,
            4 to 3,
            5 to 4,
            7 to 6,
            8 to 7,
            9 to 8,
        )
        assertEquals(
            unchangedMap,
            diff.unchangedIndexMap,
        )
    }

    @Test
    fun `simple edit`() {
        val old = BasicEntryList
        val new = old.toMutableList()
        new[0] = new[0].copy(start = 0.1f)
        new[6] = new[6].copy(start = 0.1f)
        val diff = computeEntryListDiff(old, new)
        assertContentEquals(
            listOf(
                Edit(0, old[0], 0, new[0]),
                Edit(6, old[6], 6, new[6]),
            ),
            diff.items,
        )
        val unchangedMap = BasicUnchangedMap - 0 - 6
        assertEquals(
            unchangedMap,
            diff.unchangedIndexMap,
        )
    }

    @Test
    fun `edit with move`() {
        val old = BasicEntryList
        val new = old.toMutableList()
        new[0] = new[0].copy(start = 0.1f)
        new[6] = new[6].copy(start = 0.1f)
        new.add(new.removeAt(6))
        val diff = computeEntryListDiff(old, new)
        assertContentEquals(
            listOf(
                Edit(0, old[0], 0, new[0]),
                Edit(6, old[6], 9, new[9]),
            ),
            diff.items,
        )
        val unchangedMap = BasicUnchangedMap.toMutableMap().apply {
            remove(0)
            remove(6)
            put(7, 6)
            put(8, 7)
            put(9, 8)
        }
        assertEquals(
            unchangedMap,
            diff.unchangedIndexMap,
        )
    }
}

private val BasicEntryList = listOf(
    Entry(
        sample = "1.wav",
        name = "entry1",
        start = 0.0f,
        end = 1.0f,
        points = listOf(),
        extras = listOf(),
    ),
    Entry(
        sample = "1.wav",
        name = "entry2",
        start = 1.0f,
        end = 2.0f,
        points = listOf(),
        extras = listOf(),
    ),
    Entry(
        sample = "2.wav",
        name = "entry1",
        start = 0.0f,
        end = 1.0f,
        points = listOf(),
        extras = listOf(),
    ),
    Entry(
        sample = "2.wav",
        name = "entry2",
        start = 1.0f,
        end = 2.0f,
        points = listOf(),
        extras = listOf(),
    ),
    Entry(
        sample = "3.wav",
        name = "entry1",
        start = 0.0f,
        end = 1.0f,
        points = listOf(),
        extras = listOf(),
    ),
    Entry(
        sample = "3.wav",
        name = "entry2",
        start = 1.0f,
        end = 2.0f,
        points = listOf(),
        extras = listOf(),
    ),
    Entry(
        sample = "4.wav",
        name = "entry1",
        start = 0.0f,
        end = 1.0f,
        points = listOf(),
        extras = listOf(),
    ),
    Entry(
        sample = "4.wav",
        name = "entry2",
        start = 1.0f,
        end = 2.0f,
        points = listOf(),
        extras = listOf(),
    ),
    Entry(
        sample = "5.wav",
        name = "entry1",
        start = 0.0f,
        end = 1.0f,
        points = listOf(),
        extras = listOf(),
    ),
    Entry(
        sample = "5.wav",
        name = "entry2",
        start = 1.0f,
        end = 2.0f,
        points = listOf(),
        extras = listOf(),
    ),
)

private val BasicUnchangedMap = BasicEntryList.indices.associateWith { it }
