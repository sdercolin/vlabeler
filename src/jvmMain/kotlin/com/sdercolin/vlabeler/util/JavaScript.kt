package com.sdercolin.vlabeler.util

import kotlinx.serialization.Serializable
import org.graalvm.polyglot.Context
import org.graalvm.polyglot.HostAccess
import org.graalvm.polyglot.Source
import org.graalvm.polyglot.Value
import java.io.Closeable
import java.io.File
import java.util.logging.Handler

class JavaScript(logHandler: Handler? = null, currentWorkingDirectory: File? = null) : Closeable {

    private val context = Context.newBuilder()
        .allowHostAccess(HostAccess.ALL)
        .allowHostClassLookup { true }
        .run { if (logHandler != null) logHandler(logHandler) else this }
        .allowIO(true)
        .currentWorkingDirectory(currentWorkingDirectory?.toPath() ?: HomeDir.toPath())
        .build()

    private val bindings get() = context.getBindings("js")

    fun eval(source: String): Value? = context.eval("js", source)

    fun exec(sourceFileName: String, source: String) {
        context.eval(Source.newBuilder("js", source, sourceFileName).build())
    }

    /**
     * Only for primitives. For other types, use [getJson]
     */
    fun <T : Any> getOrNull(name: String, ofClass: Class<T>): T? {
        val value = bindings.getMember(name) ?: return null
        if (value.isNull) return null
        return value.`as`(ofClass)
    }

    /**
     * Only for primitives. For other types, use [getJson]
     */
    inline fun <reified T : Any> get(name: String): T {
        return requireNotNull(getOrNull(name, T::class.java))
    }

    /**
     * Only for primitives. For other types, use [getJson]
     */
    inline fun <reified T : Any> getOrNull(name: String): T? {
        return getOrNull(name, T::class.java)
    }

    /**
     * Only for primitives. For other types, use [setJson]
     */
    fun set(name: String, value: Any?) {
        bindings.putMember(name, value)
    }

    /**
     * Pass object to JavaScript via JSON serialization
     */
    inline fun <reified T : @Serializable Any> setJson(name: String, value: T) {
        set(name, toJson(value))
        eval("$name = JSON.parse($name)")
    }

    /**
     * Receive object from JavaScript via JSON deserialization
     */
    inline fun <reified T : @Serializable Any> getJson(name: String): T {
        val json = eval("JSON.stringify($name)")!!.asString()
        return parseJson(json)
    }

    /**
     * Receive object from JavaScript via JSON deserialization
     */
    inline fun <reified T : @Serializable Any> getJsonOrNull(name: String): T? {
        val undefined = eval("typeof($name) == \"undefined\" || $name == null")?.asBoolean() != false
        if (undefined) return null
        return getJson(name)
    }

    override fun close() {
        context.close()
    }
}
