package model

import com.sdercolin.vlabeler.model.AppConf
import com.sdercolin.vlabeler.model.Entry
import testutil.TestLabelers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EntryTest {

    @Test
    fun `fromDefaultValues uses the labeler's default values`() {
        // oto-labeler: defaultValues = [100, 400, 300, 200, 100, 500], one extra field with default "500"
        val entry = Entry.fromDefaultValues("_a_ka.wav", TestLabelers.utauOto)
        assertEquals("_a_ka.wav", entry.sample)
        // no defaultEntryName: the sample name without extension is used
        assertEquals("_a_ka", entry.name)
        assertEquals(100f, entry.start)
        assertEquals(500f, entry.end)
        assertEquals(listOf(400f, 300f, 200f, 100f), entry.points)
        assertEquals(listOf<String?>("500"), entry.extras)
        assertTrue(entry.needSync)
    }

    @Test
    fun `fromDefaultValues uses the labeler's defaultEntryName when given`() {
        // nnsvs-singer-labeler: defaultValues = [0, 0], defaultEntryName = "pau", no fields or extras
        val entry = Entry.fromDefaultValues("doremi.wav", TestLabelers.nnsvsSinger)
        assertEquals("pau", entry.name)
        assertEquals(0f, entry.start)
        assertEquals(0f, entry.end)
        assertEquals(emptyList(), entry.points)
        assertEquals(emptyList(), entry.extras)
        assertTrue(entry.needSync)
    }

    @Test
    fun `sample name extension can be hidden by the view configuration`() {
        val entry = Entry.fromDefaultValues("_a_ka.wav", TestLabelers.utauOto)
        assertEquals("_a_ka", entry.sampleNameWithoutExtension)
        assertEquals("_a_ka", entry.getDisplayedSampleName(AppConf.View(hideSampleExtension = true)))
        assertEquals("_a_ka.wav", entry.getDisplayedSampleName(AppConf.View(hideSampleExtension = false)))
    }

    @Test
    fun `note editing helpers only change the corresponding note`() {
        val entry = Entry.fromDefaultValues("_a_ka.wav", TestLabelers.utauOto)

        val starred = entry.starToggled()
        assertTrue(starred.notes.star)
        assertFalse(starred.starToggled().notes.star)

        val done = entry.doneToggled()
        assertTrue(done.notes.done)
        assertFalse(done.doneToggled().notes.done)
        assertTrue(entry.done().notes.done)
        assertTrue(done.done().notes.done)

        val tagged = entry.tagEdited("tag")
        assertEquals("tag", tagged.notes.tag)
        assertEquals(entry, tagged.tagEdited(""))
    }

    @Test
    fun `needSyncCompatibly covers entries created by older labelers`() {
        val entry = Entry.fromDefaultValues("_a_ka.wav", TestLabelers.utauOto)
        assertTrue(entry.copy(end = 0f, needSync = true).needSyncCompatibly)
        // negative end is always relative to the sample end, even without the flag
        assertTrue(entry.copy(end = -100f, needSync = false).needSyncCompatibly)
        assertFalse(entry.copy(end = 0f, needSync = false).needSyncCompatibly)
        assertFalse(entry.copy(end = 100f, needSync = true).needSyncCompatibly)
    }
}
