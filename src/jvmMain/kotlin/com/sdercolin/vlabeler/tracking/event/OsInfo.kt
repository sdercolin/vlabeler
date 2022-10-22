package com.sdercolin.vlabeler.tracking.event

import com.sdercolin.vlabeler.env.osInfo
import com.sdercolin.vlabeler.env.osName
import com.sdercolin.vlabeler.env.osNameWithVersion
import kotlinx.serialization.Serializable

@Serializable
data class OsInfo(
    val name: String,
    val nameVer: String,
    val nameVerArch: String,
) {

    companion object {

        fun get() = OsInfo(
            name = osName,
            nameVer = osNameWithVersion,
            nameVerArch = osInfo,
        )
    }
}
