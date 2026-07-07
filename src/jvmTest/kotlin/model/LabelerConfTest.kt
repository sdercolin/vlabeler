package model

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.LabelerConf
import com.sdercolin.vlabeler.model.Parameter
import com.sdercolin.vlabeler.ui.string.toLocalized
import testutil.TestLabelers
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LabelerConfTest {

    @BeforeTest
    fun setup() {
        Log.muted = true
    }

    @AfterTest
    fun teardown() {
        Log.muted = false
    }

    private val labeler get() = TestLabelers.utauOto

    @Test
    fun `migrate from serialVersion 0 creates extraFields from the deprecated properties`() {
        val old = labeler.copy(
            serialVersion = 0,
            extraFields = emptyList(),
            extraFieldNames = listOf("f1", "f2"),
            defaultExtras = listOf("a", "b"),
        )
        val migrated = old.migrate()
        assertEquals(
            listOf(
                LabelerConf.ExtraField(name = "f1", default = "a"),
                LabelerConf.ExtraField(name = "f2", default = "b"),
            ),
            migrated.extraFields,
        )
        // the deprecated fields themselves are kept as-is
        assertEquals(old.extraFieldNames, migrated.extraFieldNames)
        assertEquals(old.defaultExtras, migrated.defaultExtras)
    }

    @Test
    fun `migrate from serialVersion 0 is skipped when the deprecated properties are missing`() {
        val old = labeler.copy(
            serialVersion = 0,
            extraFields = emptyList(),
            extraFieldNames = listOf("f1"),
            defaultExtras = null,
        )
        assertSame(old, old.migrate())
    }

    @Test
    fun `migrate is a no-op for the current serialVersion`() {
        assertSame(labeler, labeler.migrate())
    }

    @Test
    fun `validate passes for a bundled labeler`() {
        assertSame(labeler, labeler.validate())
    }

    @Test
    fun `validate rejects implicit start or end for continuous labelers`() {
        // the "left" field of the oto-labeler uses replaceStart
        assertFailsWith<IllegalArgumentException> {
            labeler.copy(continuous = true).validate()
        }
    }

    @Test
    fun `validate rejects a file reference as string parameter default value`() {
        val holder = LabelerConf.ParameterHolder(
            parameter = Parameter.StringParam(
                name = "param",
                label = "param".toLocalized(),
                defaultValue = "file::foo.txt",
            ),
        )
        assertFailsWith<IllegalArgumentException> {
            labeler.copy(parameters = labeler.parameters + holder).validate()
        }
        // a plain default value is accepted
        val plainHolder = LabelerConf.ParameterHolder(
            parameter = Parameter.StringParam(
                name = "param",
                label = "param".toLocalized(),
                defaultValue = "foo.txt",
            ),
        )
        labeler.copy(parameters = labeler.parameters + plainHolder).validate()
    }

    @Test
    fun `validate rejects quick project builders without an input source`() {
        // the oto-labeler defines quick project builders and defaultInputFilePath, but no projectConstructor
        assertFailsWith<IllegalArgumentException> {
            labeler.copy(defaultInputFilePath = null).validate()
        }
    }

    @Test
    fun `connectedConstraints are built from the field constraints`() {
        // fixed: min=3; preu: min=3, max=0; ovl: max=0; left: none
        assertEquals(
            listOf(3 to 0, 3 to 1, 1 to 0, 2 to 0),
            labeler.connectedConstraints,
        )
    }

    @Test
    fun `post edit trigger field names are built from the trigger settings`() {
        // oto-labeler: postEditDoneTrigger uses start, end and the fields with triggerPostEditDone == true;
        // postEditNextTrigger is not set anywhere
        assertEquals(listOf("start", "end", "fixed", "preu", "ovl"), labeler.postEditDoneTriggerFieldNames)
        assertEquals(emptyList(), labeler.postEditNextTriggerFieldNames)
    }

    @Test
    fun `implicit start replaces the entry start`() {
        // the "left" field (index 3) of the oto-labeler uses replaceStart
        assertTrue(labeler.useImplicitStart)
        assertFalse(labeler.useImplicitEnd)
        val entry = Entry(
            sample = "a.wav",
            name = "a",
            start = 100f,
            end = 500f,
            points = listOf(400f, 300f, 200f, 150f),
            extras = listOf("500"),
        )
        assertEquals(150f, labeler.getActualStart(entry))
        assertEquals(500f, labeler.getActualEnd(entry))

        // without replacing fields, start and end are used directly
        assertEquals(100f, TestLabelers.nnsvsSinger.getActualStart(entry))
        assertEquals(500f, TestLabelers.nnsvsSinger.getActualEnd(entry))
    }
}
