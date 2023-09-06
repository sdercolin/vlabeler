package com.sdercolin.vlabeler.util

import com.sdercolin.vlabeler.env.Log
import java.awt.Desktop
import java.net.URI

object Url {
    const val HOME_PAGE = "https://vlabeler.com"
    const val PROJECT_GIT_HUB = "https://github.com/sdercolin/vlabeler"
    const val LATEST_RELEASE = "https://github.com/sdercolin/vlabeler/releases/latest"
    const val DISCORD_INVITATION = "https://discord.gg/yrTqG2SrRd"
    const val GITHUB_API_ROOT = "https://api.github.com/repos/sdercolin/vlabeler"
    const val TRACKING_DOCUMENT = "https://github.com/sdercolin/vlabeler/blob/main/readme/tracking.md"
    const val ENTRY_SELECTOR_SCRIPT_DOCUMENT =
        "https://github.com/sdercolin/vlabeler/blob/main/readme/plugin-entry-selector-script.md"

    fun open(url: String) = runCatching { Desktop.getDesktop().browse(URI(url)) }
        .onFailure { Log.error(it) }
}
