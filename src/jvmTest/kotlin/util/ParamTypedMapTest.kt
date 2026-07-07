package util

import com.sdercolin.vlabeler.model.FileWithEncoding
import com.sdercolin.vlabeler.model.Parameter
import com.sdercolin.vlabeler.util.ParamTypedMap
import com.sdercolin.vlabeler.util.ParamTypedMap.TypedValue
import com.sdercolin.vlabeler.util.parseJson
import com.sdercolin.vlabeler.util.resolve
import com.sdercolin.vlabeler.util.stringifyJson
import com.sdercolin.vlabeler.util.toParamMap
import com.sdercolin.vlabeler.util.toParamTypedMap
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import testutil.TestLabelers
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParamTypedMapTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        tempDir = createTempDirectory("vlabeler-test").toFile()
    }

    @AfterTest
    fun teardown() {
        tempDir.deleteRecursively()
    }

    private val labeler get() = TestLabelers.utauOto

    @Test
    fun `from returns null for a null or all-default param map`() {
        assertNull(ParamTypedMap.from(null, labeler.parameterDefs))
        assertNull(ParamTypedMap.from(labeler.getDefaultParams(), labeler.parameterDefs))
    }

    @Test
    fun `from keeps only the values different from the defaults`() {
        // the default value of "useNegativeOvl" is true, of "dragBase" is "Preutterance"
        val params = (labeler.getDefaultParams() + ("useNegativeOvl" to false)).toParamMap()
        val typed = requireNotNull(ParamTypedMap.from(params, labeler.parameterDefs))
        val stored = requireNotNull(typed.get("useNegativeOvl"))
        assertEquals(Parameter.BooleanParam.Type, stored.type)
        assertEquals(false, stored.value)
        assertNull(typed.get("dragBase"))
    }

    @Test
    fun `resolve fills all missing parameters with default values`() {
        val params = (labeler.getDefaultParams() + ("useNegativeOvl" to false)).toParamMap()
        val typed = ParamTypedMap.from(params, labeler.parameterDefs)
        val resolved = typed.resolve(labeler)
        assertEquals(labeler.parameterDefs.size, resolved.size)
        assertEquals(false, resolved["useNegativeOvl"])
        assertEquals("Preutterance", resolved["dragBase"])
    }

    @Test
    fun `resolve on null gives all default values`() {
        assertEquals(labeler.getDefaultParams(), (null as ParamTypedMap?).resolve(labeler))
    }

    @Test
    fun `typed values survive a json round trip`() {
        val typed = mapOf(
            "int" to TypedValue(Parameter.IntParam.Type, 42),
            "float" to TypedValue(Parameter.FloatParam.Type, 1.5f),
            "boolean" to TypedValue(Parameter.BooleanParam.Type, true),
            "string" to TypedValue(Parameter.StringParam.Type, "hello"),
            "enum" to TypedValue(Parameter.EnumParam.Type, "option1"),
        ).toParamTypedMap()

        val parsed = typed.stringifyJson().parseJson<ParamTypedMap>()

        assertEquals(42, parsed.get("int")?.value)
        assertEquals(1.5f, parsed.get("float")?.value)
        assertEquals(true, parsed.get("boolean")?.value)
        assertEquals("hello", parsed.get("string")?.value)
        assertEquals("option1", parsed.get("enum")?.value)
        assertEquals(Parameter.IntParam.Type, parsed.get("int")?.type)
        assertEquals(Parameter.EnumParam.Type, parsed.get("enum")?.type)
    }

    @Test
    fun `stripFilePaths masks file parameters only`() {
        val typed = mapOf(
            "file" to TypedValue(Parameter.FileParam.Type, FileWithEncoding("/some/path", "UTF-8")),
            "rawFile" to TypedValue(Parameter.RawFileParam.Type, "/some/other/path"),
            "string" to TypedValue(Parameter.StringParam.Type, "keep"),
        ).toParamTypedMap()

        val stripped = typed.stripFilePaths()
        assertEquals(FileWithEncoding("*", null), stripped.get("file")?.value)
        assertEquals("*", stripped.get("rawFile")?.value)
        assertEquals("keep", stripped.get("string")?.value)
    }

    @Test
    fun `ParamMap resolves primitive values to json`() {
        val paramMap = mapOf(
            "int" to 1,
            "float" to 2.5f,
            "boolean" to true,
            "string" to "x",
        ).toParamMap()

        val resolved = paramMap.resolve(project = null, js = null)
        assertEquals(JsonPrimitive(1), resolved["int"])
        assertEquals(JsonPrimitive(2.5f), resolved["float"])
        assertEquals(JsonPrimitive(true), resolved["boolean"])
        assertEquals(JsonPrimitive("x"), resolved["string"])
    }

    @Test
    fun `ParamMap resolves a file with encoding to its text content`() {
        val file = tempDir.resolve("content.txt").apply { writeText("hello world") }
        val paramMap = mapOf(
            "existing" to FileWithEncoding(file.absolutePath, "UTF-8"),
            "missing" to FileWithEncoding(null, null),
        ).toParamMap()

        val resolved = paramMap.resolve(project = null, js = null)
        assertEquals(JsonPrimitive("hello world"), resolved["existing"])
        assertEquals(JsonNull, resolved["missing"])
    }
}
