package model

import com.sdercolin.vlabeler.model.Entry
import com.sdercolin.vlabeler.model.EntryNotes
import com.sdercolin.vlabeler.util.parseJson
import kotlin.test.Test
import kotlin.test.assertEquals

class EntryMigrationDeserializationTest {

    @Test
    fun testDeserializeOldEntry() {
        val input = """
            {
                "sample": "1",
                "name": "f",
                "start": 2376.94,
                "end": 2572.555,
                "points": [
                ],
                "extras": [
                ],
                "meta": {
                    "done": false,
                    "star": false,
                    "tag": ""
                }
            }
        """.trimIndent()
        val expected = Entry(
            sample = "1",
            name = "f",
            start = 2376.94f,
            end = 2572.555f,
            points = emptyList(),
            extras = emptyList(),
            notes = EntryNotes(
                done = false,
                star = false,
                tag = "",
            ),
        )
        val actual = input.parseJson<Entry>()
        assertEquals(expected, actual)
    }
}
