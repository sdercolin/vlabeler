package com.sdercolin.vlabeler.util

import java.net.URI

object Url {
    const val ProjectGitHub = "https://github.com/sdercolin/vlabeler"
    const val DiscordInvitation = "https://discord.gg/yrTqG2SrRd"
}

fun String.toUri() = URI(this)
