package com.sdercolin.vlabeler.audio

import androidx.compose.runtime.Stable
import com.sdercolin.vlabeler.env.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import kotlin.math.roundToInt

@Stable
class Player(
    private val coroutineScope: CoroutineScope,
    private val state: PlayerState
) {
    private var file: File? = null
    private var format: AudioFormat? = null
    private val AudioFormat.sampleSize: Int
        get() {
            val channelNumber = channels
            val frameByteSize = sampleSizeInBits / 8
            return channelNumber * frameByteSize
        }
    private var line: SourceDataLine? = null
    private var data: ByteArray? = null
    private var openJob: Job? = null
    private var countingJob: Job? = null
    private var writingJob: Job? = null

    suspend fun load(newFile: File) {
        openJob?.cancelAndJoin()
        file = newFile
        openJob = coroutineScope.launch(Dispatchers.IO) {
            Log.info("Player.load(\"${newFile.absolutePath}\")")
            val line = AudioSystem.getAudioInputStream(newFile).use { stream ->
                format = stream.format
                data = stream.readAllBytes().also {
                    Log.info("Player.load: read ${it.size} bytes")
                }
                AudioSystem.getSourceDataLine(stream.format)
            }
            this@Player.line = line
            line.open()
        }
    }

    fun toggle() {
        file ?: return
        coroutineScope.launch {
            if (state.isPlaying) stop() else play()
        }
    }

    private suspend fun awaitLoad() {
        openJob?.join()
    }

    private fun play() {
        coroutineScope.launch {
            Log.info("Player.play()")
            awaitLoad()
            state.startPlaying()
            startWriting()
            startCounting()
        }
    }

    private suspend fun startWriting(startFrame: Int = 0, endFrame: Int? = null) {
        writingJob?.cancelAndJoin()
        val line = line ?: return
        val format = format ?: return
        val data = data ?: return
        line.start()
        writingJob = coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                val offset = startFrame * format.sampleSize
                val length = endFrame?.let { (it - startFrame) * format.sampleSize } ?: (data.size - offset)
                line.write(data, offset, length)
                line.drain()
                if (state.isPlaying) {
                    println("stopping after drain")
                    stop()
                }
            }.onFailure {
                if (it is CancellationException) {
                    Log.info(it.message ?: "Player::writingJob is Cancelled")
                } else {
                    Log.error(it)
                }
            }
        }
    }

    private suspend fun startCounting(startFrame: Int = 0) {
        countingJob?.cancelAndJoin()
        countingJob = coroutineScope.launch {
            val line = line ?: return@launch
            state.resetFramePosition(line.framePosition.toFloat(), startFrame.toFloat())
            while (true) {
                delay(PlayingTimeInterval)
                if(!state.isPlaying) break
                state.setFramePositionRelatively(line.framePosition.toFloat())
            }
        }
    }

    fun playSection(startFramePosition: Float, endFramePosition: Float) {
        coroutineScope.launch {
            if (state.isPlaying) {
                println("stopping before playSection")
                stop()
            }
            Log.info("Player.playSection($startFramePosition, $endFramePosition)")
            awaitLoad()
            val startFrame = startFramePosition.roundToInt()
            val endFrame = endFramePosition.roundToInt()
            state.startPlaying()
            startWriting(startFrame, endFrame)
            startCounting(startFrame)
        }
    }

    private suspend fun stop() {
        Log.info("Player.stop()")
        state.stopPlaying()
        countingJob?.cancelAndJoin()
        countingJob = null
        line?.run {
            stop()
            flush()
        }
        writingJob?.cancelAndJoin()
        writingJob = null
    }

    fun close() {
        Log.info("Player.close()")
        openJob?.cancel()
        countingJob?.cancel()
        line?.run {
            stop()
            flush()
            close()
        }
    }

    companion object {
        private const val PlayingTimeInterval = 5L
    }
}
