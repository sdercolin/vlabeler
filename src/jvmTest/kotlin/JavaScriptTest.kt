import com.sdercolin.vlabeler.util.JavaScript
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for [JavaScript].
 */
class JavaScriptTest {

    private lateinit var js: JavaScript

    @BeforeTest
    fun setUp() {
        js = JavaScript(System.out)
    }

    @Test
    fun testDouble() {
        js.use {
            it.eval("a=1\nb=2.1")
            assertEquals(2.1, it.get("b"))
            assertEquals(3.1, it.eval("a+b")?.asDouble())
        }
    }

    @Test
    fun testUnicodeString() {
        js.use {
            it.set("a", "あ")
            assertEquals("あ", it.get("a"))
        }
    }

    @Test
    fun testNull() {
        js.use {
            it.set("a", null)
            it.eval("b=null")
            assert(it.getOrNull<Any>("a") == null)
            assert(it.getOrNull<Any>("b") == null)
        }
    }

    @Test
    fun testObjectNull() {
        js.use {
            it.set("a", null)
            assert(it.getJsonOrNull<Pair<String, Int>>("a") == null)
        }
    }

    @Test
    fun testUndefined() {
        js.use {
            assert(it.getOrNull<Any>("a") == null)
        }
    }

    @Test
    fun testObjectUndefined() {
        js.use {
            assert(it.getJsonOrNull<Pair<String, Int>>("a") == null)
        }
    }

    @Test
    fun testList() {
        js.use {
            val list = listOf(1, 2, 3)
            it.setJson("list", list)
            it.eval("list[0]=3\nlist.push(4)")
            assertEquals(listOf(3, 2, 3, 4), it.getJson("list"))
        }
    }

    @Test
    fun testMap() {
        js.use {
            val map = mapOf("a" to 1, "b" to 2)
            it.setJson("map", map)
            it.eval("map[\"a\"]=3\nmap[\"c\"]=1")
            assertEquals(mapOf("a" to 3, "b" to 2, "c" to 1), it.getJson("map"))
        }
    }

    @Test
    fun testObject() {
        js.use {
            val pair = Pair("c", 1)
            it.setJson("pair", pair)
            it.eval("pair.first = \"a\"")
            assertEquals(Pair("a", 1), it.getJson("pair"))
        }
    }

    @Test
    fun testObjectList() {
        js.use {
            val list = listOf("c" to 1, "d" to 2)
            it.setJson("list", list)
            it.eval(
                """
                class Pair {
                    constructor(first, second) {
                        this.first = first
                        this.second = second
                    }
                }
                let newPair = new Pair("e", 3)
                list.push(newPair)
                """.trimIndent(),
            )
            val expected = listOf("c" to 1, "d" to 2, "e" to 3)
            assertEquals(expected, it.getJson("list"))
        }
    }

    @Test
    fun testRawObjectList() {
        js.use {
            val list = listOf(File("a"), File("b"))
            it.setArray("list", list)
            assertEquals(list, it.getArray("list"))
            assertEquals(list, it.getArrayOrNull("list"))
        }
    }
}
