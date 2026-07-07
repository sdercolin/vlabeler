package util

import com.sdercolin.vlabeler.util.savedMutableStateOf
import kotlin.test.Test
import kotlin.test.assertEquals

class SavedMutableStateTest {

    @Test
    fun `initial value does not trigger save`() {
        val saved = mutableListOf<Int>()
        val state = savedMutableStateOf(1) { saved.add(it) }
        assertEquals(1, state.value)
        assertEquals(emptyList(), saved)
    }

    @Test
    fun `setting a value saves it`() {
        val saved = mutableListOf<Int>()
        val state = savedMutableStateOf(1) { saved.add(it) }
        state.value = 2
        assertEquals(2, state.value)
        assertEquals(listOf(2), saved)
    }

    @Test
    fun `every set is saved including repeated values`() {
        val saved = mutableListOf<String>()
        val state = savedMutableStateOf("a") { saved.add(it) }
        state.value = "b"
        state.value = "b"
        state.value = "c"
        assertEquals("c", state.value)
        assertEquals(listOf("b", "b", "c"), saved)
    }

    @Test
    fun `destructuring exposes the getter and setter`() {
        val saved = mutableListOf<Int>()
        val state = savedMutableStateOf(1) { saved.add(it) }
        val (value, setValue) = state
        assertEquals(1, value)
        setValue(5)
        assertEquals(5, state.value)
        assertEquals(listOf(5), saved)
    }
}
