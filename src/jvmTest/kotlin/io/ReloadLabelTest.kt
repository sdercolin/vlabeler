package io

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.io.mergeEntryLists
import com.sdercolin.vlabeler.io.reloadEntriesFromLabelFile
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.EntryListDiffItem.Add
import com.sdercolin.vlabeler.model.EntryListDiffItem.Edit
import com.sdercolin.vlabeler.model.EntryListDiffItem.Remove
import com.sdercolin.vlabeler.model.EntryListDiffItem.Unchanged
import com.sdercolin.vlabeler.model.EntryNotes
import com.sdercolin.vlabeler.model.Project
import com.sdercolin.vlabeler.model.computeEntryListDiff
import com.sdercolin.vlabeler.ui.dialog.ReloadLabelConfigs
import testutil.TestEnv
import testutil.TestFixtures
import testutil.TestLabelers
import testutil.createTestProject
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Tests for [reloadEntriesFromLabelFile] and [mergeEntryLists].
 */
class ReloadLabelTest {

    private lateinit var tempDir: File
    private lateinit var sampleDir: File
    private lateinit var project: Project

    @BeforeTest
    fun setup() {
        Log.muted = true
        TestEnv.ensureLogDirectory()
        tempDir = createTempDirectory("vlabeler-test").toFile()
        sampleDir = TestFixtures.deploy(
            "utau-singer",
            tempDir.resolve("utau-singer"),
            wavFiles = listOf("C4/_a_ka.wav", "C4/_i_ki.wav", "A3/_a_ka.wav"),
        )
        project = createTestProject(
            labeler = TestLabelers.utauSinger,
            sampleDirectory = sampleDir,
        )
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
        tempDir.deleteRecursively()
    }

    private val module get() = project.modules.first { it.name == "C4" }
    private val otoFile get() = sampleDir.resolve("C4").resolve("oto.ini")

    private fun reload(): Pair<List<Entry>, com.sdercolin.vlabeler.model.EntryListDiff> =
        reloadEntriesFromLabelFile(project, module, otoFile).getOrThrow()

    @Test
    fun testReloadUnchangedFile() {
        val (entries, diff) = reload()
        assertEquals(module.entries, entries)
        assertEquals(4, diff.items.filterIsInstance<Unchanged>().size)
        assertEquals(4, diff.items.size)
    }

    @Test
    fun testReloadEditedValue() {
        // change preutterance of entry "- a" from 80.0 to 90.0
        otoFile.writeText(
            otoFile.readText().replace(
                "_a_ka.wav=- a,10.0,100.0,-400.0,80.0,30.0",
                "_a_ka.wav=- a,10.0,100.0,-400.0,90.0,30.0",
            ),
        )
        val (entries, diff) = reload()
        assertEquals(100f, entries.first { it.name == "- a" }.points[1])
        val edits = diff.items.filterIsInstance<Edit>()
        assertEquals(1, edits.size)
        assertEquals("- a", edits.single().old.name)
        assertEquals(90f, edits.single().new.points[1] - edits.single().new.points[3])
        assertEquals(3, diff.items.filterIsInstance<Unchanged>().size)
    }

    @Test
    fun testReloadAddedEntry() {
        otoFile.appendText("\n_i_ki.wav=u ki,450.0,120.0,-350.0,100.0,40.0\n")
        val (entries, diff) = reload()
        assertEquals(5, entries.size)
        val adds = diff.items.filterIsInstance<Add>()
        assertEquals(1, adds.size)
        assertEquals("u ki", adds.single().new.name)
        assertEquals(4, diff.items.filterIsInstance<Unchanged>().size)
    }

    @Test
    fun testReloadRemovedEntry() {
        otoFile.writeText(
            otoFile.readLines().filterNot { it.contains("=i ki,") }.joinToString("\n"),
        )
        val (entries, diff) = reload()
        assertEquals(3, entries.size)
        val removes = diff.items.filterIsInstance<Remove>()
        assertEquals(1, removes.size)
        assertEquals("i ki", removes.single().old.name)
        assertEquals(3, diff.items.filterIsInstance<Unchanged>().size)
    }

    private fun entry(name: String, start: Float) = Entry(
        sample = "a.wav",
        name = name,
        start = start,
        end = start + 100f,
        points = listOf(),
        extras = listOf(),
    )

    @Test
    fun testMergeInheritsNotesOnEditedAndUnchangedEntries() {
        val notes = EntryNotes(done = true, star = true, tag = "keep")
        val old = listOf(
            entry("a", 0f).copy(notes = notes),
            entry("b", 100f).copy(notes = EntryNotes(tag = "other")),
        )
        val new = listOf(
            entry("a", 10f), // edited
            entry("b", 100f), // unchanged
        )
        val diff = computeEntryListDiff(old, new)
        val merged = mergeEntryLists(new, old, diff, ReloadLabelConfigs())
        assertEquals(notes, merged[0].notes)
        assertEquals("other", merged[1].notes.tag)
        // positions come from the new list
        assertEquals(10f, merged[0].start)
    }

    @Test
    fun testMergeWithoutInheritanceReturnsNewList() {
        val old = listOf(entry("a", 0f).copy(notes = EntryNotes(tag = "keep")))
        val new = listOf(entry("a", 10f))
        val diff = computeEntryListDiff(old, new)
        val configs = ReloadLabelConfigs(inheritStar = false, inheritDone = false, inheritTag = false)
        val merged = mergeEntryLists(new, old, diff, configs)
        assertSame(new, merged)
    }

    @Test
    fun testMergeSelectiveInheritance() {
        val old = listOf(entry("a", 0f).copy(notes = EntryNotes(done = true, star = true, tag = "keep")))
        val new = listOf(entry("a", 10f))
        val diff = computeEntryListDiff(old, new)
        val configs = ReloadLabelConfigs(inheritStar = false, inheritDone = false, inheritTag = true)
        val merged = mergeEntryLists(new, old, diff, configs)
        assertEquals("keep", merged.single().notes.tag)
        assertFalse(merged.single().notes.done)
        assertFalse(merged.single().notes.star)
        assertTrue(old.single().notes.done)
    }
}
