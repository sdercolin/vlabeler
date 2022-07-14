package com.sdercolin.vlabeler.audio

import androidx.compose.runtime.Stable
import com.sdercolin.vlabeler.env.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.io.File
import javax.sound.sampled.AudioSystem

@Stable
class Player(
    private val coroutineScope: CoroutineScope,
    private val state: PlayerState
) {
    private var file: File? = null
    private val clip = AudioSystem.getClip()

    private var openJob: Job? = null
    private var countingJob: Job? = null

    suspend fun load(newFile: File) {
        openJob?.cancelAndJoin()
        openJob = coroutineScope.launch(Dispatchers.IO) {
            Log.info("Player.load(\"${newFile.absolutePath}\")")
            if (file != null) {
                clip.flush()
                clip.close()
            }
            yield()
            file = newFile
            AudioSystem.getAudioInputStream(newFile).use { clip.open(it) }
        }
    }

    fun toggle() {
        file ?: return
        if (state.isPlaying) stop() else {
            reset()
            play()
        }
    }

    private fun play(untilPosition: Int? = null) {
        Log.info("Player.play()")
        countingJob = coroutineScope.launch {
            while (true) {
                delay(PlayingTimeInterval)
                state.changeFramePosition(clip.framePosition)
                if (!clip.isRunning) {
                    state.stopPlaying()
                    return@launch
                }
                if (untilPosition != null && state.framePosition >= untilPosition) {
                    stop()
                    return@launch
                }
            }
        }
        state.startPlaying()
        clip.start()
    }

    fun playSection(startPosition: Float, endPosition: Float) {
        file ?: return
        Log.info("Player.playSection($startPosition, $endPosition)")
        reset()
        clip.framePosition = startPosition.toInt()
        play(untilPosition = endPosition.toInt())
    }

    private fun stop() {
        Log.info("Player.stop()")
        clip.stop()
        countingJob?.cancel()
        countingJob = null
        state.stopPlaying()
    }

    private fun reset() {
        Log.info("Player.reset()")
        clip.close()
        AudioSystem.getAudioInputStream(file).use { clip.open(it) }
        clip.framePosition = 0
        state.changeFramePosition(0)
    }

    fun close() {
        Log.info("Player.close()")
        openJob?.cancel()
        countingJob?.cancel()
        clip.close()
    }

    companion object {
        private const val PlayingTimeInterval = 50L
    }
}
