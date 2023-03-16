package com.sdercolin.vlabeler.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A request made by a plugin to play an audio file or a piece of audio data.
 */
@Serializable
sealed class AudioPlaybackRequest {

    /**
     * A request to play an audio file of wav format.
     *
     * @property path Audio file to play as path.
     * @property offset Starting position of the audio file to play, in milliseconds, default to 0.
     * @property duration Duration of the audio file to play, in milliseconds. If not given, the whole file will be
     *     played.
     */
    @Serializable
    @SerialName("file")
    class PlayFile(val path: String, val offset: Double = 0.0, val duration: Double? = null) : AudioPlaybackRequest()
}
