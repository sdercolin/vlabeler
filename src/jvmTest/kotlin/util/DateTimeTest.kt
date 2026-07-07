package util

import com.sdercolin.vlabeler.util.getLocalDate
import com.sdercolin.vlabeler.util.parseIsoTime
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [parseIsoTime] and [getLocalDate].
 */
class DateTimeTest {

    @Test
    fun testParseIsoTimeWithUtcOffset() {
        // 2022-01-02T03:04:05 UTC
        val expected = LocalDateTime.of(2022, 1, 2, 3, 4, 5).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(expected, parseIsoTime("2022-01-02T03:04:05Z"))
    }

    @Test
    fun testParseIsoTimeIgnoresOffset() {
        // The current implementation parses the value as a LocalDateTime and always
        // converts it with the UTC offset, so the offset in the input is ignored.
        assertEquals(
            parseIsoTime("2022-01-02T03:04:05Z"),
            parseIsoTime("2022-01-02T03:04:05+09:00"),
        )
    }

    @Test
    fun testGetLocalDate() {
        val time = 1641092645000L // 2022-01-02T03:04:05Z
        val expected = LocalDateTime
            .ofEpochSecond(time / 1000, 0, OffsetDateTime.now().offset)
            .format(DateTimeFormatter.ISO_LOCAL_DATE)
        assertEquals(expected, getLocalDate(time))
    }

    @Test
    fun testGetLocalDateFormat() {
        val result = getLocalDate(1641092645000L)
        assertEquals(true, Regex("""\d{4}-\d{2}-\d{2}""").matches(result))
    }
}
