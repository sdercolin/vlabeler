package com.sdercolin.vlabeler.flag

/**
 * Feature flags are used to enable/disable features that are not ready for production. You can add a line in your
 * `local.properties` file with `${key}=true` to enable a flag.
 *
 * @property key The key of the flag. Should always start with `flag.`.
 */
sealed class FeatureFlags(val key: String) {

    fun get() = System.getProperty(key)?.toBoolean() ?: false

    /* region flags */

    object UseOnScreenScissors : FeatureFlags("flag.use.on.screen.scissors")

    /* endregion */

    companion object {
        val all = listOf(UseOnScreenScissors)
    }
}
