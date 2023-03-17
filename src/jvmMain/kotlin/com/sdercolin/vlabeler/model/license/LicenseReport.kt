package com.sdercolin.vlabeler.model.license

import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.useResource
import com.sdercolin.vlabeler.util.Resources
import com.sdercolin.vlabeler.util.parseJson
import kotlinx.serialization.Serializable

/**
 * A serializable report of Open Source Licenses used in this project.
 *
 * @property dependencies The list of dependencies.
 */
@Serializable
@Immutable
data class LicenseReport(
    val dependencies: List<Dependency>,
) {
    @Serializable
    @Immutable
    data class Dependency(
        val moduleName: String,
        val moduleVersion: String,
        val moduleUrl: String? = null,
        val moduleLicense: String? = null,
        val moduleLicenseUrl: String? = null,
    )

    companion object {
        fun load(): LicenseReport = useResource(Resources.licensesJson) {
            it.bufferedReader().readText().parseJson()
        }
    }
}
