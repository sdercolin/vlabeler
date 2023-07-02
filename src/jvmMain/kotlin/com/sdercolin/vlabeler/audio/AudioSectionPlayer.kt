package com.sdercolin.vlabeler.audio

/**
 * An interface for playing audio section used by UI layer.
 */
fun interface AudioSectionPlayer {

    /**
     * Play audio section.
     *
     * @param startFrame start frame of the section
     * @param endFrame end frame of the section, null as the end of the audio
     * @param repeat whether to repeat the section forever
     */
    fun playSection(startFrame: Float, endFrame: Float?, repeat: Boolean)

    fun playSection(startFrame: Float, endFrame: Float?) = playSection(startFrame, endFrame, false)
}
