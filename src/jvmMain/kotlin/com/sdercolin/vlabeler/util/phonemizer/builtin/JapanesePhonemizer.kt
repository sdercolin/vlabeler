package com.sdercolin.vlabeler.util.phonemizer.builtin

// ktlint-disable
import com.sdercolin.vlabeler.util.phonemizer.PhonemizerLanguage
import com.sdercolin.vlabeler.util.phonemizer.PhonemizerPlugin

object JapanesePhonemizer : PhonemizerPlugin {
    override val id = PhonemizerLanguage.Japanese.id
    override val displayNameKey = PhonemizerLanguage.Japanese.displayNameKey

    private val hiraganaBase = mapOf(
        "あ" to listOf("a"), "い" to listOf("i"), "う" to listOf("u"), "え" to listOf("e"), "お" to listOf("o"),
        "か" to listOf("k", "a"), "き" to listOf("k", "i"), "く" to listOf("k", "u"), "け" to listOf("k", "e"), "こ" to listOf(
            "k",
            "o",
        ),
        "さ" to listOf("s", "a"), "し" to listOf("sh", "i"), "す" to listOf("s", "u"), "せ" to listOf("s", "e"), "そ" to listOf(
            "s",
            "o",
        ),
        "た" to listOf("t", "a"), "ち" to listOf("ch", "i"), "つ" to listOf("ts", "u"), "て" to listOf("t", "e"), "と" to listOf(
            "t",
            "o",
        ),
        "な" to listOf("n", "a"), "に" to listOf("n", "i"), "ぬ" to listOf("n", "u"), "ね" to listOf("n", "e"), "の" to listOf(
            "n",
            "o",
        ),
        "は" to listOf("h", "a"), "ひ" to listOf("h", "i"), "ふ" to listOf("f", "u"), "へ" to listOf("h", "e"), "ほ" to listOf(
            "h",
            "o",
        ),
        "ま" to listOf("m", "a"), "み" to listOf("m", "i"), "む" to listOf("m", "u"), "め" to listOf("m", "e"), "も" to listOf(
            "m",
            "o",
        ),
        "や" to listOf("y", "a"), "ゆ" to listOf("y", "u"), "よ" to listOf("y", "o"),
        "ら" to listOf("r", "a"), "り" to listOf("r", "i"), "る" to listOf("r", "u"), "れ" to listOf("r", "e"), "ろ" to listOf(
            "r",
            "o",
        ),
        "わ" to listOf("w", "a"), "を" to listOf("o"), "ん" to listOf("N"),
        "が" to listOf("g", "a"), "ぎ" to listOf("g", "i"), "ぐ" to listOf("g", "u"), "げ" to listOf("g", "e"), "ご" to listOf(
            "g",
            "o",
        ),
        "ざ" to listOf("z", "a"), "じ" to listOf("j", "i"), "ず" to listOf("z", "u"), "ぜ" to listOf("z", "e"), "ぞ" to listOf(
            "z",
            "o",
        ),
        "だ" to listOf("d", "a"), "ぢ" to listOf("j", "i"), "づ" to listOf("z", "u"), "で" to listOf("d", "e"), "ど" to listOf(
            "d",
            "o",
        ),
        "ば" to listOf("b", "a"), "び" to listOf("b", "i"), "ぶ" to listOf("b", "u"), "べ" to listOf("b", "e"), "ぼ" to listOf(
            "b",
            "o",
        ),
        "ぱ" to listOf("p", "a"), "ぴ" to listOf("p", "i"), "ぷ" to listOf("p", "u"), "ぺ" to listOf("p", "e"), "ぽ" to listOf(
            "p",
            "o",
        ),
        "きゃ" to listOf("ky", "a"), "きゅ" to listOf("ky", "u"), "きょ" to listOf("ky", "o"),
        "しゃ" to listOf("sh", "a"), "しゅ" to listOf("sh", "u"), "しょ" to listOf("sh", "o"),
        "ちゃ" to listOf("ch", "a"), "ちゅ" to listOf("ch", "u"), "ちょ" to listOf("ch", "o"),
        "にゃ" to listOf("ny", "a"), "にゅ" to listOf("ny", "u"), "にょ" to listOf("ny", "o"),
        "ひゃ" to listOf("hy", "a"), "ひゅ" to listOf("hy", "u"), "ひょ" to listOf("hy", "o"),
        "みゃ" to listOf("my", "a"), "みゅ" to listOf("my", "u"), "みょ" to listOf("my", "o"),
        "りゃ" to listOf("ry", "a"), "りゅ" to listOf("ry", "u"), "りょ" to listOf("ry", "o"),
        "ぎゃ" to listOf("gy", "a"), "ぎゅ" to listOf("gy", "u"), "ぎょ" to listOf("gy", "o"),
        "じゃ" to listOf("j", "a"), "じゅ" to listOf("j", "u"), "じょ" to listOf("j", "o"),
        "びゃ" to listOf("by", "a"), "びゅ" to listOf("by", "u"), "びょ" to listOf("by", "o"),
        "ぴゃ" to listOf("py", "a"), "ぴゅ" to listOf("py", "u"), "ぴょ" to listOf("py", "o"),
        "っ" to listOf("cl"),
    )

    private fun toKatakana(h: String) = h.map { c -> if (c in '\u3041'..'\u3096') (c.code + 0x60).toChar() else c }.joinToString(
        "",
    )

    private val kanaMap: Map<String, List<String>> = buildMap {
        putAll(hiraganaBase); hiraganaBase.forEach { (k, v) -> put(toKatakana(k), v) }; put("ッ", listOf("cl")); put(
            "ー",
            listOf("-"),
        ); put("ン", listOf("N")); put("ヲ", listOf("o"))
    }

    private val romajiMap = mapOf(
        "kya" to listOf("ky", "a"), "kyu" to listOf("ky", "u"), "kyo" to listOf("ky", "o"),
        "sha" to listOf("sh", "a"), "shu" to listOf("sh", "u"), "sho" to listOf("sh", "o"),
        "cha" to listOf("ch", "a"), "chu" to listOf("ch", "u"), "cho" to listOf("ch", "o"),
        "nya" to listOf("ny", "a"), "nyu" to listOf("ny", "u"), "nyo" to listOf("ny", "o"),
        "hya" to listOf("hy", "a"), "hyu" to listOf("hy", "u"), "hyo" to listOf("hy", "o"),
        "mya" to listOf("my", "a"), "myu" to listOf("my", "u"), "myo" to listOf("my", "o"),
        "rya" to listOf("ry", "a"), "ryu" to listOf("ry", "u"), "ryo" to listOf("ry", "o"),
        "gya" to listOf("gy", "a"), "gyu" to listOf("gy", "u"), "gyo" to listOf("gy", "o"),
        "bya" to listOf("by", "a"), "byu" to listOf("by", "u"), "byo" to listOf("by", "o"),
        "pya" to listOf("py", "a"), "pyu" to listOf("py", "u"), "pyo" to listOf("py", "o"),
        "ja" to listOf("j", "a"), "ju" to listOf("j", "u"), "jo" to listOf("j", "o"),
        "tsu" to listOf("ts", "u"), "shi" to listOf("sh", "i"), "chi" to listOf("ch", "i"),
        "ka" to listOf("k", "a"), "ki" to listOf("k", "i"), "ku" to listOf("k", "u"), "ke" to listOf("k", "e"), "ko" to listOf(
            "k",
            "o",
        ),
        "sa" to listOf("s", "a"), "su" to listOf("s", "u"), "se" to listOf("s", "e"), "so" to listOf("s", "o"),
        "ta" to listOf("t", "a"), "te" to listOf("t", "e"), "to" to listOf("t", "o"),
        "na" to listOf("n", "a"), "ni" to listOf("n", "i"), "nu" to listOf("n", "u"), "ne" to listOf("n", "e"), "no" to listOf(
            "n",
            "o",
        ),
        "ha" to listOf("h", "a"), "hi" to listOf("h", "i"), "fu" to listOf("f", "u"), "he" to listOf("h", "e"), "ho" to listOf(
            "h",
            "o",
        ),
        "ma" to listOf("m", "a"), "mi" to listOf("m", "i"), "mu" to listOf("m", "u"), "me" to listOf("m", "e"), "mo" to listOf(
            "m",
            "o",
        ),
        "ya" to listOf("y", "a"), "yu" to listOf("y", "u"), "yo" to listOf("y", "o"),
        "ra" to listOf("r", "a"), "ri" to listOf("r", "i"), "ru" to listOf("r", "u"), "re" to listOf("r", "e"), "ro" to listOf(
            "r",
            "o",
        ),
        "wa" to listOf("w", "a"), "wo" to listOf("o"), "nn" to listOf("N"), "n" to listOf("N"),
        "ga" to listOf("g", "a"), "gi" to listOf("g", "i"), "gu" to listOf("g", "u"), "ge" to listOf("g", "e"), "go" to listOf(
            "g",
            "o",
        ),
        "za" to listOf("z", "a"), "ji" to listOf("j", "i"), "zu" to listOf("z", "u"), "ze" to listOf("z", "e"), "zo" to listOf(
            "z",
            "o",
        ),
        "da" to listOf("d", "a"), "di" to listOf("j", "i"), "du" to listOf("z", "u"), "de" to listOf("d", "e"), "do" to listOf(
            "d",
            "o",
        ),
        "ba" to listOf("b", "a"), "bi" to listOf("b", "i"), "bu" to listOf("b", "u"), "be" to listOf("b", "e"), "bo" to listOf(
            "b",
            "o",
        ),
        "pa" to listOf("p", "a"), "pi" to listOf("p", "i"), "pu" to listOf("p", "u"), "pe" to listOf("p", "e"), "po" to listOf(
            "p",
            "o",
        ),
        "a" to listOf("a"), "i" to listOf("i"), "u" to listOf("u"), "e" to listOf("e"), "o" to listOf("o"),
    )

    override fun phonemize(text: String): List<String> {
        val out = mutableListOf<String>(); var i = 0; val s = text.trim()
        while (i < s.length) {
            val r = s.length - i
            if (r >= 2) { val v = kanaMap[s.substring(i, i + 2)]; if (v != null) { out.addAll(v); i += 2; continue } }
            if (r >= 3) { val v = romajiMap[s.substring(i, i + 3).lowercase()]; if (v != null) { out.addAll(v); i += 3; continue } }
            if (r >= 2) { val v = romajiMap[s.substring(i, i + 2).lowercase()]; if (v != null) { out.addAll(v); i += 2; continue } }
            val one = s.substring(i, i + 1); val v1 = kanaMap[one]; if (v1 != null) { out.addAll(v1); i++; continue }
            val v2 = romajiMap[one.lowercase()]; if (v2 != null) { out.addAll(v2); i++; continue }
            if (one.isNotBlank() && one != "-" && one != "_") out.add(one.lowercase()); i++
        }
        return out
    }
}
