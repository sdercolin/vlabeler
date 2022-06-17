package com.sdercolin.vlabeler.audio

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableState
import com.sdercolin.vlabeler.util.update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.sound.sampled.AudioSystem

@Immutable
data class PlayerState(
    val isPlaying: Boolean = false,
    val framePosition: Int = 0
) {
    fun onStartPlaying() = copy(isPlaying = true)
    fun onStopPlaying() = copy(isPlaying = false)
    fun onChangeFramePosition(position: Int) = copy(framePosition = position)
}

class Player(
    private val coroutineScope: CoroutineScope,
    private val playerState: MutableState<PlayerState>
) {
    private var file: File? = null
    private val clip = AudioSystem.getClip()
    private var isPlaying: Boolean
        get() = playerState.value.isPlaying
        set(value) {
            playerState.update { if (value) onStartPlaying() else onStopPlaying() }
        }
    private var framePosition: Int
        get() = playerState.value.framePosition
        set(value) {
            playerState.update { onChangeFramePosition(value) }
        }
    private var countingJob: Job? = null

    fun load(file: File) {
        this.file = file
        clip.open(AudioSystem.getAudioInputStream(file))
    }

    fun toggle() {
        if (isPlaying) stop() else {
            reset()
            play()
        }
    }

    private fun play(untilPosition: Int? = null) {
        println("Player::play()")
        countingJob = coroutineScope.launch {
            while (true) {
                delay(PlayingTimeInterval)
                framePosition = clip.framePosition
                if (!clip.isRunning) {
                    isPlaying = false
                    return@launch
                }
                if (untilPosition != null && framePosition >= untilPosition) {
                    stop()
                    return@launch
                }
            }
        }
        isPlaying = true
        clip.start()
    }

    fun playSection(startPosition: Float, endPosition: Float) {
        println("Player::playSection($startPosition, $endPosition)")
        reset()
        clip.framePosition = startPosition.toInt()
        play(untilPosition = endPosition.toInt())
    }

    private fun stop() {
        println("Player::stop()")
        clip.stop()
        countingJob?.cancel()
        countingJob = null
        isPlaying = false
    }

    private fun reset() {
        println("Player::reset()")
        clip.close()
        clip.open(AudioSystem.getAudioInputStream(file))
        clip.framePosition = 0
        framePosition = 0
    }

    companion object {
        private const val PlayingTimeInterval = 50L
    }
}
