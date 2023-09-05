package com.sdercolin.vlabeler.util

import com.sdercolin.vlabeler.env.Log
import java.awt.Desktop
import java.net.URI

object Url {
    const val HomePage = "https://vlabeler.com"
    const val ProjectGitHub = "https://github.com/sdercolin/vlabeler"
    const val LatestRelease = "https://github.com/sdercolin/vlabeler/releases/latest"
    const val DiscordInvitation = "https://discord.gg/yrTqG2SrRd"
    const val GithubApiRoot = "https://api.github.com/repos/sdercolin/vlabeler"
    const val TrackingDocument = "https://github.com/sdercolin/vlabeler/blob/main/readme/tracking.md"
    const val EntrySelectorScriptDocument =
        "https://github.com/sdercolin/vlabeler/blob/main/readme/plugin-entry-selector-script.md"

    fun open(url: String) = runCatching { Desktop.getDesktop().browse(URI(url)) }
        .onFailure { Log.error(it) }
}
