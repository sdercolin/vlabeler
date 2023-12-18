package com.sdercolin.vlabeler.util

import java.text.Normalizer

/**
 * This class is used to convert Unicode characters to NFC form.
 */
object UnicodeNormalizer {

    fun convertToNfc(input: String): String {
        return Normalizer.normalize(input, Normalizer.Form.NFC)
    }
}
