package env

import com.sdercolin.vlabeler.util.getTimeText
import kotlin.test.Test
import kotlin.test.assertContentEquals

class TimeTest {

    private fun time(hour: Int = 0, minute: Int = 0, second: Int = 0, millisecond: Double = 0.0): Double =
        (hour * 3600 + minute * 60 + second) * 1000 + millisecond

    @Test
    fun testTimeText() {
        val sources = listOf(
            time(1, 2, 3, 4.5),
            time(11, 2, 3, 4.0),
            time(0, 2, 3),
            time(0, 0, 3),
            time(0, 0, 3, 400.0),
            time(0, 0, 0, 4.5),
            time(0, 0, 0, 0.6),
        )
        val expected = listOf(
            "1:02:03.0045",
            "11:02:03.004",
            "2:03",
            "3",
            "3.4",
            "0.0045",
            "0.0006",
        )
        val results = sources.map { getTimeText(it) }
        assertContentEquals(expected, results)
    }
}
