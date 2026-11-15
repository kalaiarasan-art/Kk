package com.example.ui.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * Procedural audio synthesizer (§10):
 * Generates crisp sound effects in memory with zero network bytes and zero bundled audio files.
 * Includes a global mute toggle.
 */
object ProceduralSoundEngine {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
  @Volatile var isMuted: Boolean = false

  fun playTick() {
    if (isMuted) return
    scope.launch { playToneSequence(listOf(ToneStep(880.0, 45, 0.25))) }
  }

  fun playLowTimeTick() {
    if (isMuted) return
    scope.launch { playToneSequence(listOf(ToneStep(1174.66, 65, 0.35))) }
  }

  fun playCorrectGuess() {
    if (isMuted) return
    scope.launch {
      playToneSequence(
        listOf(
          ToneStep(523.25, 70, 0.35), // C5
          ToneStep(659.25, 70, 0.35), // E5
          ToneStep(783.99, 90, 0.40), // G5
          ToneStep(1046.50, 160, 0.45) // C6
        )
      )
    }
  }

  fun playCloseGuess() {
    if (isMuted) return
    scope.launch {
      playToneSequence(
        listOf(
          ToneStep(440.0, 80, 0.30),
          ToneStep(466.16, 110, 0.35)
        )
      )
    }
  }

  fun playTurnStart() {
    if (isMuted) return
    scope.launch {
      playToneSequence(
        listOf(
          ToneStep(392.00, 80, 0.30), // G4
          ToneStep(523.25, 90, 0.35), // C5
          ToneStep(659.25, 140, 0.40) // E5
        )
      )
    }
  }

  fun playPodiumFanfare() {
    if (isMuted) return
    scope.launch {
      playToneSequence(
        listOf(
          ToneStep(523.25, 90, 0.35),
          ToneStep(659.25, 90, 0.35),
          ToneStep(783.99, 90, 0.40),
          ToneStep(1046.50, 220, 0.45)
        )
      )
    }
  }

  fun playReactionPop() {
    if (isMuted) return
    scope.launch {
      playToneSequence(listOf(ToneStep(698.46, 55, 0.25)))
    }
  }

  private data class ToneStep(val freqHz: Double, val durationMs: Int, val volume: Double)

  private fun playToneSequence(steps: List<ToneStep>) {
    try {
      val sampleRate = 22050
      val totalSamples = steps.sumOf { (sampleRate * it.durationMs) / 1000 }
      if (totalSamples <= 0) return
      val pcm = ShortArray(totalSamples)
      var offset = 0
      for (step in steps) {
        val count = (sampleRate * step.durationMs) / 1000
        for (i in 0 until count) {
          val t = i.toDouble() / sampleRate
          val envelope = exp(-4.5 * (i.toDouble() / count))
          val sample = sin(2.0 * PI * step.freqHz * t) * step.volume * envelope
          pcm[offset + i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        offset += count
      }

      val track = AudioTrack.Builder()
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .setAudioFormat(
          AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        )
        .setBufferSizeInBytes(pcm.size * 2)
        .setTransferMode(AudioTrack.MODE_STATIC)
        .build()

      track.write(pcm, 0, pcm.size)
      track.play()
      Thread.sleep((steps.sumOf { it.durationMs } + 40).toLong())
      track.release()
    } catch (_: Throwable) {
      // Ignore audio hardware availability errors on headless test runners
    }
  }
}
