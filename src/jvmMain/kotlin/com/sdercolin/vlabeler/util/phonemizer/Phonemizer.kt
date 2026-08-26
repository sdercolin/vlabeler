// ktlint-disable
package com.sdercolin.vlabeler.util.phonemizer

import com.sdercolin.vlabeler.ui.string.Strings
import com.sdercolin.vlabeler.util.phonemizer.builtin.EnglishPhonemizer
import com.sdercolin.vlabeler.util.phonemizer.builtin.JapanesePhonemizer
import com.sdercolin.vlabeler.util.phonemizer.builtin.RawPhonemizer
import com.sdercolin.vlabeler.util.phonemizer.builtin.RussianPhonemizer

interface PhonemizerPlugin {
    val id: String
    val displayNameKey: Strings
    fun phonemize(text: String): List<String>
}

enum class PhonemizerLanguage(val id: String, val displayNameKey: Strings) {
    Raw("raw", Strings.PhonemizerLanguageRaw),
    Japanese("ja", Strings.PhonemizerLanguageJapanese),
    English("en", Strings.PhonemizerLanguageEnglish),
    Russian("ru", Strings.PhonemizerLanguageRussian),
}

object PhonemizerRegistry {
    private val builtIns: Map<String, PhonemizerPlugin> = mapOf(
        PhonemizerLanguage.Raw.id to RawPhonemizer,
        PhonemizerLanguage.Japanese.id to JapanesePhonemizer,
        PhonemizerLanguage.English.id to EnglishPhonemizer,
        PhonemizerLanguage.Russian.id to RussianPhonemizer,
    )
    private val custom = mutableMapOf<String, PhonemizerPlugin>()
    fun getAll(): List<PhonemizerPlugin> = builtIns.values + custom.values
    fun get(id: String): PhonemizerPlugin? = builtIns[id] ?: custom[id]
    fun getByLanguage(l: PhonemizerLanguage): PhonemizerPlugin = requireNotNull(get(l.id))
    fun register(p: PhonemizerPlugin) { require(p.id !in builtIns); custom[p.id] = p }
    fun unregister(id: String) { custom.remove(id) }
}

object Phonemizer {
    fun phonemize(text: String, language: PhonemizerLanguage = PhonemizerLanguage.Raw): List<String> {
        val t = text.trim(); if (t.isEmpty()) return emptyList()
        return PhonemizerRegistry.getByLanguage(language).phonemize(t)
    }
    fun phonemize(text: String, id: String): List<String> {
        val t = text.trim(); if (t.isEmpty()) return emptyList()
        return (PhonemizerRegistry.get(id) ?: RawPhonemizer).phonemize(t)
    }
}

abstract class DictPhonemizer(
    private val dictPath: String,
    private val graphemes: List<String>,
    private val phonemes: List<String>,
    private val modelPath: String,
) : PhonemizerPlugin {
    protected val dictionary: Map<String, List<String>> by lazy { loadDict() }
    protected val predictor by lazy { OnnxG2pPredictor(modelPath, graphemes, phonemes) }
    protected abstract fun mapPhones(phones: List<String>): List<String>
    private fun loadDict(): Map<String, List<String>> {
        val m = mutableMapOf<String, List<String>>()
        try {
            val s = Phonemizer::class.java.getResourceAsStream(dictPath) ?: return emptyMap()
            s.bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (l in lines) {
                    val t = l.trim(); if (t.isEmpty() || t.startsWith(";;;")) continue
                    val p = t.split("\\s+".toRegex()); if (p.size >= 2) m[p[0].lowercase()] = mapPhones(p.drop(1).map { it.replace("\\d+".toRegex(), "").lowercase() })
                }
            }
        } catch (_: Throwable) {}
        return m
    }
    protected fun ruleBasedEnglish(word: String): List<String> {
        val c = word.lowercase().trim(); val o = mutableListOf<String>(); var i = 0
        while (i < c.length) {
            if (i + 1 < c.length) {
                val t = c.substring(i, i + 2)
                val m = when (t) {
                    "th" -> "th"; "sh" -> "sh"; "ch" -> "ch"; "ph" -> "f"; "wh" -> "w"; "ng" -> "ng"; "ck" -> "k"
                    "ee", "ea" -> "iy"; "oo" -> "uw"; "ou", "ow" -> "aw"; "ai", "ay" -> "ey"; "oi", "oy" -> "oy" else -> null
                }
                if (m != null) { o.add(m); i += 2; continue }
            }
            when (c[i]) {
                'a' -> o.add("ae"); 'e' -> o.add("eh"); 'i' -> o.add("ih"); 'o' -> o.add("aa"); 'u' -> o.add("ah"); 'y' -> o.add("iy")
                'b' -> o.add("b"); 'c' -> o.add("k"); 'd' -> o.add("d"); 'f' -> o.add("f"); 'g' -> o.add("g"); 'h' -> o.add("hh"); 'j' -> o.add("jh")
                'k' -> o.add("k"); 'l' -> o.add("l"); 'm' -> o.add("m"); 'n' -> o.add("n"); 'p' -> o.add("p"); 'q' -> { o.add("k"); o.add("w") }
                'r' -> o.add("r"); 's' -> o.add("s"); 't' -> o.add("t"); 'v' -> o.add("v"); 'w' -> o.add("w"); 'x' -> { o.add("k"); o.add("s") }; 'z' -> o.add("z")
                else -> if (c[i].isLetter()) o.add(c[i].toString())
            }
            i++
        }
        return o
    }
}
