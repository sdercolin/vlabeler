package com.sdercolin.vlabeler.util.phonemizer.builtin

// ktlint-disable
import com.sdercolin.vlabeler.util.phonemizer.DictPhonemizer
import com.sdercolin.vlabeler.util.phonemizer.PhonemizerLanguage

object RussianPhonemizer : DictPhonemizer(
    "/g2p/ru/dict.txt",
    listOf("", "", "", "", "-", "а", "б", "в", "г", "д", "е", "ж", "з", "и", "й", "к", "л", "м", "н", "о", "п", "р", "с", "т", "у", "ф", "х", "ц", "ч", "ш", "щ", "ъ", "ы", "ь", "э", "ю", "я", "ё"),
    listOf("", "", "", "", "a", "aa", "ay", "b", "bb", "c", "ch", "d", "dd", "ee", "f", "ff", "g", "gg", "h", "hh", "i", "ii", "j", "ja", "je", "jo", "ju", "k", "kk", "l", "ll", "m", "mm", "n", "nn", "oo", "p", "pp", "r", "rr", "s", "sch", "sh", "ss", "t", "tt", "u", "uj", "uu", "v", "vv", "y", "yy", "z", "zh", "zz"),
    "/g2p/ru/g2p.onnx",
) {
    override val id = PhonemizerLanguage.Russian.id
    override val displayNameKey = PhonemizerLanguage.Russian.displayNameKey
    private fun single(s: String) = when (s) {
        "bb" -> "bj"; "vv" -> "vj"; "gg" -> "gj"; "dd" -> "dj"; "zz" -> "zj"; "kk" -> "kj"; "ll" -> "lj"; "mm" -> "mj"; "nn" -> "nj"; "pp" -> "pj"; "rr" -> "rj"; "ss" -> "sj"; "tt" -> "tj"; "ff" -> "fj"; "hh" -> "hj"; "sch" -> "shj"
        "a" -> "A"; "aa" -> "a"; "i" -> "E"; "ii" -> "i"; "y" -> "Y"; "yy" -> "y"
        "ee" -> "e"; "oo" -> "o"; "uu" -> "u"; else -> s
    }
    override fun mapPhones(phones: List<String>): List<String> {
        val m = phones.map { single(it) }; val r = mutableListOf<String>()
        for (ph in m) {
            val prev = r.lastOrNull(); val soft = prev != null && prev.endsWith("j") && prev.length > 1
            when (ph) {
                "je" -> if (soft) r.add("e") else { r.add("j"); r.add("e") }
                "ja" -> if (soft) r.add("a") else { r.add("j"); r.add("a") }
                "jo" -> if (soft) r.add("o") else { r.add("j"); r.add("o") }
                "ju" -> if (soft) r.add("u") else { r.add("j"); r.add("u") }
                "ay" -> { r.add("a"); r.add("j") }; "uj" -> { r.add("u"); r.add("j") }; else -> r.add(ph)
            }
        }
        return r
    }
    private val hard = mapOf('б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'ж' to "zh", 'з' to "z", 'й' to "j", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t", 'ф' to "f", 'х' to "h", 'ц' to "c", 'ч' to "ch", 'ш' to "sh", 'щ' to "shj")
    private val softMap = mapOf('б' to "bj", 'в' to "vj", 'г' to "gj", 'д' to "dj", 'ж' to "zhj", 'з' to "zj", 'й' to "j", 'к' to "kj", 'л' to "lj", 'м' to "mj", 'н' to "nj", 'п' to "pj", 'р' to "rj", 'с' to "sj", 'т' to "tj", 'ф' to "fj", 'х' to "hj", 'ц' to "cj", 'ч' to "ch", 'ш' to "shj", 'щ' to "shj")
    private val pal = setOf('е', 'ё', 'и', 'ю', 'я', 'ь')
    override fun phonemize(text: String): List<String> {
        val out = mutableListOf<String>()
        for (w in text.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }) {
            val clean = w.lowercase().filter { it in '\u0400'..'\u04FF' }; if (clean.isEmpty()) continue
            val d = dictionary[clean]
            if (d != null) out.addAll(d) else {
                val p = predictor.predict(clean)?.let { mapPhones(it) }
                if (!p.isNullOrEmpty()) out.addAll(p) else out.addAll(ruleBasedRu(w))
            }
        }
        return out
    }
    private fun ruleBasedRu(text: String): List<String> {
        val r = mutableListOf<String>(); val s = text.trim(); var i = 0
        while (i < s.length) {
            val raw = s[i]; val c = raw.lowercaseChar()
            if (raw.isWhitespace() || raw == '-' || raw == '_') { i++; continue }
            if (hard.containsKey(c)) {
                val nxt = if (i + 1 < s.length) s[i + 1].lowercaseChar() else null
                val palatal = nxt != null && pal.contains(nxt)
                r.add(if (palatal) softMap[c] ?: hard[c]!! else hard[c]!!)
                if (nxt == 'ь' || nxt == 'ъ') { i += 2; continue }; i++; continue
            }
            val prev = if (i > 0) s[i - 1].lowercaseChar() else null
            val after = prev != null && hard.containsKey(prev)
            when (c) {
                'а' -> r.add("a"); 'о' -> r.add("o"); 'у' -> r.add("u"); 'э' -> r.add("e"); 'ы' -> r.add("y"); 'и' -> r.add(
                    "i",
                )
                'я' -> if (after) r.add("a") else { r.add("j"); r.add("a") }
                'е' -> if (after) r.add("e") else { r.add("j"); r.add("e") }
                'ё' -> if (after) r.add("o") else { r.add("j"); r.add("o") }
                'ю' -> if (after) r.add("u") else { r.add("j"); r.add("u") }
                'ь', 'ъ' -> Unit else -> if (raw.isLetter()) r.add(raw.toString())
            }
            i++
        }
        return r
    }
}
