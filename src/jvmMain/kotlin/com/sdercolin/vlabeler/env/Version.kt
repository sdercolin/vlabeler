package com.sdercolin.vlabeler.env

import com.sdercolin.vlabeler.repository.update.model.UpdateChannel
import java.util.Properties

/**
 * The version of the application.
 */
val appVersion: Version by lazy {
    val stream = Thread.currentThread().contextClassLoader.getResource("app.properties")?.openStream()
    val properties = Properties().apply { load(requireNotNull(stream)) }
    requireNotNull(Version.from(properties.getProperty("app.version")))
}

/**
 * A data class representing a version.
 *
 * @property major The major version number.
 * @property minor The minor version number.
 * @property patch The patch version number.
 * @property stage The [VersionStage] of the version. If null, the version is a stable version.
 * @property stageVersion The version number of the stage. The nullability is the same as [stage].
 */
data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val stage: VersionStage? = null,
    val stageVersion: Int? = null,
) : Comparable<Version> {
    override fun toString(): String = "$major.$minor.$patch" +
        (stage?.let { "-${it.serialName}" } ?: "") +
        (stageVersion?.toString() ?: "")

    override operator fun compareTo(other: Version): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor
        if (patch != other.patch) return patch - other.patch
        if (stage != other.stage) {
            if (stage == null) return 1
            if (other.stage == null) return -1
            return stage.ordinal - other.stage.ordinal
        }
        if (stageVersion != other.stageVersion) return (stageVersion ?: 0) - (other.stageVersion ?: 0)
        return 0
    }

    val isStable: Boolean
        get() = stage == null

    val isBeta: Boolean
        get() = stage == VersionStage.Beta

    val isAlpha: Boolean
        get() = stage == VersionStage.Alpha

    fun isMajorNewerThan(other: Version): Boolean = major > other.major

    fun isMinorNewerThan(other: Version): Boolean =
        isMajorNewerThan(other) || (major == other.major && minor > other.minor)

    fun isPatchNewerThan(other: Version): Boolean =
        isMinorNewerThan(other) || (major == other.major && minor == other.minor && patch > other.patch)

    fun isInChannel(channel: UpdateChannel): Boolean = when (channel) {
        UpdateChannel.Stable -> isStable
        UpdateChannel.Preview -> isBeta
    }

    companion object {

        val zero = Version(0, 0, 0)

        fun from(versionText: String): Version? = runCatching {
            val sections = versionText.split(".", "-")
            val major = sections[0].toInt()
            val minor = sections[1].toInt()
            val patch = sections[2].toInt()
            val stageText = sections.getOrNull(3)
            val stage = if (stageText != null) {
                VersionStage.entries.find { stageText.startsWith(it.serialName) }
            } else null
            val stageVersion = stageText?.drop(stage?.serialName?.length ?: 0)?.toInt()

            Version(
                major = major,
                minor = minor,
                patch = patch,
                stage = stage,
                stageVersion = stageVersion,
            )
        }.getOrElse {
            Log.error(it)
            null
        }
    }
}

enum class VersionStage(val serialName: String) {
    Alpha("alpha"),
    Beta("beta"),
}
