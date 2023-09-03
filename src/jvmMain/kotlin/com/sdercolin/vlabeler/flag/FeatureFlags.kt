package com.sdercolin.vlabeler.flag

/**
 * Feature flags are used to enable/disable features that are not ready for production.
 */
sealed class FeatureFlags(val key: String) {

    private fun getEnvironmentVariableKey(key: String) = "VLABELER_$key"

    fun get() = System.getenv(getEnvironmentVariableKey(key))?.toBoolean() ?: false

    /* region flags */

    object EnableOnScreenScissors : FeatureFlags("ENABLE_ON_SCREEN_SCISSORS")

    /* endregion */
}
