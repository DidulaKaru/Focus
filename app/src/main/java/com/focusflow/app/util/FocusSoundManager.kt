package com.focusflow.app.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.focusflow.app.repository.AmbientSound
import java.util.Random
import kotlin.math.sin

object FocusSoundManager {

    @Volatile
    private var isRunning = false

    private var audioTrack: AudioTrack? = null
    @Volatile
    private var playbackThread: Thread? = null
    private var currentSound: AmbientSound? = null

    fun playSound(context: Context, sound: AmbientSound) {
        if (sound == AmbientSound.OFF) {
            stopSound()
            return
        }

        synchronized(this) {
            if (isRunning && currentSound == sound) {
                return
            }
            stopSoundLocked()

            currentSound = sound
            isRunning = true

            playbackThread = Thread {
                runAudioLoop(sound)
            }.apply {
                name = "FocusSoundThread"
                start()
            }
        }
    }

    fun stopSound() {
        synchronized(this) {
            stopSoundLocked()
        }
    }

    fun isPlaying(): Boolean {
        synchronized(this) {
            return isRunning && audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING
        }
    }

    private fun stopSoundLocked() {
        isRunning = false
        currentSound = null
        try {
            audioTrack?.let { track ->
                if (track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    track.stop()
                }
                track.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        audioTrack = null
        playbackThread?.interrupt()
        playbackThread = null
    }

    private fun runAudioLoop(sound: AmbientSound) {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, 44100 * 2)

        val track = try {
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        synchronized(this) {
            if (!isRunning || playbackThread !== Thread.currentThread()) {
                track.release()
                return
            }
            audioTrack = track
        }

        val buffer = ShortArray(44100)
        var sampleIndex = 0L
        var lastOutput = 0.0f
        val random = Random()

        // Fade envelope parameters (1 sec fade in)
        val fadeSamples = 44100

        try {
            track.play()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        while (isRunning && playbackThread === Thread.currentThread() && !Thread.currentThread().isInterrupted) {
            for (i in buffer.indices) {
                val fadeGain = if (sampleIndex < fadeSamples) {
                    sampleIndex.toFloat() / fadeSamples
                } else {
                    1.0f
                }

                val sampleValue: Short = when (sound) {
                    AmbientSound.WHITE_NOISE -> {
                        val raw = random.nextFloat() * 2f - 1f
                        (raw * 0.12f * fadeGain * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }

                    AmbientSound.RAIN -> {
                        val raw = random.nextFloat() * 2f - 1f
                        val alpha = 0.15f
                        lastOutput = alpha * raw + (1f - alpha) * lastOutput
                        (lastOutput * 0.20f * fadeGain * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }

                    AmbientSound.FOREST -> {
                        val raw = random.nextFloat() * 2f - 1f
                        val alpha = 0.08f
                        lastOutput = alpha * raw + (1f - alpha) * lastOutput

                        val timeSec = sampleIndex / sampleRate.toDouble()
                        val windModulation = 0.5 + 0.5 * sin(2 * Math.PI * 0.15 * timeSec)
                        val breeze = lastOutput * windModulation * 0.18

                        val chirpCycle = timeSec % 4.0
                        val chirp = if (chirpCycle < 0.12) {
                            val freq = 2200.0 + 800.0 * sin(2 * Math.PI * 15.0 * chirpCycle)
                            sin(2 * Math.PI * freq * chirpCycle) * 0.04 * sin(Math.PI * chirpCycle / 0.12)
                        } else 0.0

                        ((breeze + chirp) * fadeGain * Short.MAX_VALUE).toInt()
                            .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }

                    AmbientSound.OFF -> 0.toShort()
                }
                buffer[i] = sampleValue
                sampleIndex++
            }

            val result = track.write(buffer, 0, buffer.size)
            if (result < 0) {
                break
            }
        }
    }
}
