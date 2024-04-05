package model

import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.EntryListDiffItem.Add
import com.sdercolin.vlabeler.model.EntryListDiffItem.Edit
import com.sdercolin.vlabeler.model.EntryListDiffItem.Remove
import com.sdercolin.vlabeler.model.EntryListDiffItem.Unchanged
import com.sdercolin.vlabeler.model.computeEntryListDiff
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals

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
                Unchanged(0, 0, old[0]),
                Unchanged(1, 1, old[1]),
                Unchanged(2, 2, old[2]),
                Unchanged(3, 3, old[3]),
                Unchanged(4, 4, old[4]),
                Unchanged(5, 5, old[5]),
                Unchanged(6, 6, old[6]),
                Unchanged(7, 7, old[7]),
                Unchanged(8, 8, old[8]),
                Unchanged(9, 9, old[9]),
                Add(10, new[10]),
            ),
            diff.items,
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
                Unchanged(0, 1, old[0]),
                Unchanged(1, 2, old[1]),
                Unchanged(2, 3, old[2]),
                Unchanged(3, 4, old[3]),
                Unchanged(4, 5, old[4]),
                Unchanged(5, 6, old[5]),
                Unchanged(6, 7, old[6]),
                Add(8, new[8]),
                Unchanged(7, 9, old[7]),
                Unchanged(8, 10, old[8]),
                Unchanged(9, 11, old[9]),
                Add(12, new[12]),
            ),
            diff.items,
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
                Unchanged(1, 0, old[1]),
                Unchanged(2, 1, old[2]),
                Unchanged(3, 2, old[3]),
                Unchanged(4, 3, old[4]),
                Unchanged(5, 4, old[5]),
                Unchanged(6, 5, old[6]),
                Unchanged(7, 6, old[7]),
                Unchanged(8, 7, old[8]),
                Unchanged(9, 8, old[9]),
            ),
            diff.items,
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
                Unchanged(1, 0, old[1]),
                Unchanged(2, 1, old[2]),
                Unchanged(3, 2, old[3]),
                Unchanged(4, 3, old[4]),
                Unchanged(5, 4, old[5]),
                Unchanged(6, 5, old[6]),
                Unchanged(7, 6, old[7]),
                Remove(8, old[8]),
                Remove(9, old[9]),
            ),
            diff.items,
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
                Unchanged(1, 1, old[1]),
                Unchanged(2, 2, old[2]),
                Remove(3, old[3]),
                Unchanged(4, 3, old[4]),
                Unchanged(5, 4, old[5]),
                Add(5, newEntries[1]),
                Remove(6, old[6]),
                Unchanged(7, 6, old[7]),
                Unchanged(8, 7, old[8]),
                Unchanged(9, 8, old[9]),
                Add(9, newEntries[2]),
            ),
            diff.items,
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
                Unchanged(1, 1, old[1]),
                Unchanged(2, 2, old[2]),
                Unchanged(3, 3, old[3]),
                Unchanged(4, 4, old[4]),
                Unchanged(5, 5, old[5]),
                Edit(6, old[6], 6, new[6]),
                Unchanged(7, 7, old[7]),
                Unchanged(8, 8, old[8]),
                Unchanged(9, 9, old[9]),
            ),
            diff.items,
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
                Unchanged(1, 1, old[1]),
                Unchanged(2, 2, old[2]),
                Unchanged(3, 3, old[3]),
                Unchanged(4, 4, old[4]),
                Unchanged(5, 5, old[5]),
                Unchanged(7, 6, old[7]),
                Unchanged(8, 7, old[8]),
                Unchanged(9, 8, old[9]),
                Edit(6, old[6], 9, new[9]),
            ),
            diff.items,
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
