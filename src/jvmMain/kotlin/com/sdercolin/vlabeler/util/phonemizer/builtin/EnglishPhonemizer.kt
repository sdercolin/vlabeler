package com.sdercolin.vlabeler.util.phonemizer.builtin

// ktlint-disable
import com.sdercolin.vlabeler.util.phonemizer.DictPhonemizer
import com.sdercolin.vlabeler.util.phonemizer.PhonemizerLanguage

object EnglishPhonemizer : DictPhonemizer(
    "/g2p/en/dict.txt",
    listOf("", "", "", "", "'", "-", "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"),
    listOf("", "", "", "", "aa", "ae", "ah", "ao", "aw", "ay", "b", "ch", "d", "dh", "eh", "er", "ey", "f", "g", "hh", "ih", "iy", "jh", "k", "l", "m", "n", "ng", "ow", "oy", "p", "r", "s", "sh", "t", "th", "uh", "uw", "v", "w", "y", "z", "zh"),
    "/g2p/en/g2p.onnx",
) {
    override val id = PhonemizerLanguage.English.id
    override val displayNameKey = PhonemizerLanguage.English.displayNameKey
    override fun mapPhones(phones: List<String>) = phones
    override fun phonemize(text: String): List<String> {
        val out = mutableListOf<String>()
        for (w in text.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }) {
            val clean = w.lowercase().filter { it.isLetter() }; if (clean.isEmpty()) continue
            val d = dictionary[clean]
            if (d != null) out.addAll(d) else {
                val p = predictor.predict(clean)
                if (!p.isNullOrEmpty()) out.addAll(p) else out.addAll(ruleBasedEnglish(w))
            }
        }
        return out
    }
}
