package com.sdercolin.vlabeler.audio.conversion

import com.sdercolin.vlabeler.exception.LocalizedException
import com.sdercolin.vlabeler.ui.string.Strings

/**
 * An exception thrown when converting audio files using [WaveConverter].
 */
abstract class WaveConverterException(stringKeys: Strings, cause: Throwable?) : LocalizedException(stringKeys, cause)
