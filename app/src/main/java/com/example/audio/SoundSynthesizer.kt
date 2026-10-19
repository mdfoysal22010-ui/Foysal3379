package com.example.audio

import com.example.model.SynthPreset
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

object SoundSynthesizer {

    private const val SAMPLE_RATE = 44100

    fun generatePcm(preset: SynthPreset, pitchFactor: Float = 1.0f, volumeBoost: Float = 1.0f): ShortArray {
        val basePcm = when (preset) {
            SynthPreset.PICCHI_CRY -> generateBabyCry()
            SynthPreset.PICCHI_LAUGH -> generateBabyLaugh()
            SynthPreset.CID_DRAMA -> generateCidDramaticSting()
            SynthPreset.DIPJOL_PUNCH -> generateDipjolPunch()
            SynthPreset.TAHERI_BEAT -> generateTaheriRhythm()
            SynthPreset.SALYARA_TUNE -> generateSalyaraTune()
            SynthPreset.FF_BOOYAH -> generateBooyahFanfare()
            SynthPreset.FF_HEADSHOT -> generateHeadshotPing()
            SynthPreset.FF_AWM_SNIPER -> generateAwmSniper()
            SynthPreset.GIRL_GIGGLE_HELLO -> generateGirlChimeHello()
            SynthPreset.ROBOT_ALERT -> generateRobotAlert()
            SynthPreset.MONSTER_ROAR -> generateMonsterRoar()
            SynthPreset.AIR_HORN -> generateAirHorn()
            SynthPreset.VICTORY_TRUMPET -> generateVictoryTrumpet()
            SynthPreset.RADIO_STATIC_WALKIE -> generateWalkieSquelch()
            SynthPreset.GHOST_SCREAM -> generateGhostScream()
        }

        // Apply pitch shifting if not 1.0f
        val pitchShifted = if (abs(pitchFactor - 1.0f) > 0.05f) {
            resamplePitch(basePcm, pitchFactor)
        } else {
            basePcm
        }

        // Apply volume boost (scaled with soft clipping limiter)
        val boosted = ShortArray(pitchShifted.size)
        val boost = volumeBoost.coerceIn(0.1f, 2.0f)
        for (i in pitchShifted.indices) {
            val sample = pitchShifted[i] * boost
            boosted[i] = sample.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toInt().toShort()
        }

        return boosted
    }

    private fun generateBabyCry(): ShortArray {
        val durationSec = 2.4
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // 3 cry bursts
            val burstCycle = (t % 0.8) / 0.8
            val envelope = sin(burstCycle * PI).coerceAtLeast(0.0)

            // Baby cry frequency sweeps from 480 Hz to 680 Hz with tremor
            val tremolo = 1.0 + 0.12 * sin(2 * PI * 18 * t)
            val baseFreq = 500.0 + 150.0 * sin(burstCycle * PI)
            val sampleVal = sin(2 * PI * baseFreq * t * tremolo) +
                    0.4 * sin(4 * PI * baseFreq * t) +
                    0.2 * (Random.nextDouble() - 0.5)

            buffer[i] = (sampleVal * envelope * 24000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateBabyLaugh(): ShortArray {
        val durationSec = 2.0
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        // 6 cheerful baby giggle syllables
        val syllables = 7
        val syllDuration = durationSec / syllables

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val syllTime = (t % syllDuration) / syllDuration
            val env = sin(syllTime * PI).coerceAtLeast(0.0)

            // Chirp upwards
            val freq = 750.0 + 350.0 * syllTime
            val sample = sin(2 * PI * freq * t) + 0.3 * sin(4 * PI * freq * t)
            buffer[i] = (sample * env * 25000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateCidDramaticSting(): ShortArray {
        val durationSec = 2.5
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        // Initial timpani/boom + harsh dramatic suspense brass chords
        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = if (t < 0.08) t / 0.08 else Math.exp(-(t - 0.08) * 1.5)

            // Boom bass
            val boom = sin(2 * PI * (75.0 - t * 15.0) * t) * Math.exp(-t * 3.0)

            // Dramatic minor diminished chord: 330, 392, 466, 587 Hz (E, G, Bb, D)
            val brass = (sin(2 * PI * 330 * t) +
                    sin(2 * PI * 392 * t) +
                    sin(2 * PI * 466 * t) +
                    sin(2 * PI * 587 * t)) * 0.25

            val sample = boom * 0.5 + brass * 0.5
            buffer[i] = (sample * env * 28000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateDipjolPunch(): ShortArray {
        val durationSec = 1.4
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = Math.exp(-t * 4.5)

            // Punch impact: 140 Hz downward sweep to 45 Hz + white noise crunch
            val freq = (140.0 - t * 80.0).coerceAtLeast(40.0)
            val thud = sin(2 * PI * freq * t)
            val noise = (Random.nextDouble() - 0.5) * Math.exp(-t * 18.0) * 1.5

            val sample = (thud * 0.7 + noise * 0.5) * env
            buffer[i] = (sample * 29000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateTaheriRhythm(): ShortArray {
        val durationSec = 2.2
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        val beatCount = 6
        val beatInterval = durationSec / beatCount

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val beatT = (t % beatInterval)
            val env = Math.exp(-beatT * 12.0)

            // Clap noise + resonant tabla drum tone
            val clap = (Random.nextDouble() - 0.5) * Math.exp(-beatT * 30.0)
            val drum = sin(2 * PI * 180.0 * beatT) * Math.exp(-beatT * 8.0)

            val sample = clap * 0.6 + drum * 0.4
            buffer[i] = (sample * env * 27000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateSalyaraTune(): ShortArray {
        val durationSec = 2.0
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        // Slide whistle bouncy notes
        val notes = doubleArrayOf(350.0, 520.0, 700.0, 480.0, 880.0, 600.0)
        val noteDur = durationSec / notes.size

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIdx = (t / noteDur).toInt().coerceIn(0, notes.lastIndex)
            val noteT = (t % noteDur) / noteDur
            val env = sin(noteT * PI).coerceAtLeast(0.0)

            val baseFreq = notes[noteIdx]
            val slideFreq = baseFreq + 80.0 * noteT
            val sample = sin(2 * PI * slideFreq * t) + 0.2 * sin(4 * PI * slideFreq * t)

            buffer[i] = (sample * env * 25000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateBooyahFanfare(): ShortArray {
        val durationSec = 2.5
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        // Major chord arpeggio: C4 (261), E4 (329), G4 (392), C5 (523), E5 (659), G5 (784)
        val freqs = doubleArrayOf(261.63, 329.63, 392.00, 523.25, 659.25, 784.00)
        val noteDur = 0.22

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIdx = (t / noteDur).toInt()
            val sample = if (noteIdx < freqs.size - 1) {
                val f = freqs[noteIdx]
                val localT = (t % noteDur) / noteDur
                val env = sin(localT * PI).coerceAtLeast(0.0)
                (sin(2 * PI * f * t) + 0.3 * sin(4 * PI * f * t)) * env
            } else {
                // Final triumphant chord sustain
                val holdT = (t - (freqs.size - 1) * noteDur).coerceAtLeast(0.0)
                val env = Math.exp(-holdT * 0.9)
                val chord = sin(2 * PI * 523.25 * t) +
                        sin(2 * PI * 659.25 * t) * 0.8 +
                        sin(2 * PI * 784.00 * t) * 0.7
                chord * 0.4 * env
            }
            buffer[i] = (sample * 27000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateHeadshotPing(): ShortArray {
        val durationSec = 1.0
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = Math.exp(-t * 6.0)

            // Sharp metallic "DING" at 2600Hz + high chime 3900Hz
            val ping = sin(2 * PI * 2600.0 * t) + 0.5 * sin(2 * PI * 3900.0 * t) + 0.25 * sin(2 * PI * 5200.0 * t)
            buffer[i] = (ping * env * 28000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateAwmSniper(): ShortArray {
        val durationSec = 2.2
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE

            // High pressure gunshot crack
            val crack = (Random.nextDouble() - 0.5) * Math.exp(-t * 35.0) * 2.0
            // Supersonic whistle snap
            val whistle = sin(2 * PI * (3200.0 - t * 1800.0) * t) * Math.exp(-t * 22.0)
            // Heavy sub bass shockwave
            val sub = sin(2 * PI * (80.0 - t * 30.0) * t) * Math.exp(-t * 4.0)

            val sample = crack * 0.5 + whistle * 0.25 + sub * 0.6
            buffer[i] = (sample * 29000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateGirlChimeHello(): ShortArray {
        val durationSec = 1.8
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        // Sweet melodic chime notes: 880 Hz, 1046 Hz, 1318 Hz
        val notes = doubleArrayOf(880.0, 1046.5, 1318.5, 1567.9)
        val noteDur = durationSec / notes.size

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val idx = (t / noteDur).toInt().coerceIn(0, notes.lastIndex)
            val localT = (t % noteDur) / noteDur
            val env = sin(localT * PI).coerceAtLeast(0.0)

            val f = notes[idx]
            val sample = (sin(2 * PI * f * t) + 0.2 * sin(4 * PI * f * t)) * env
            buffer[i] = (sample * 24000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateRobotAlert(): ShortArray {
        val durationSec = 1.6
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // 4 square wave robotic frequency hops
            val step = (t * 5.0).toInt() % 4
            val f = when (step) {
                0 -> 440.0
                1 -> 880.0
                2 -> 660.0
                else -> 1100.0
            }
            // Ring modulated cyber voice
            val carrier = sin(2 * PI * f * t)
            val modulator = sin(2 * PI * 60.0 * t)
            val sample = carrier * modulator

            buffer[i] = (sample * 25000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateMonsterRoar(): ShortArray {
        val durationSec = 2.4
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = sin((t / durationSec) * PI)

            // Deep distorted throat growl (60 Hz fundamental + throat flutter)
            val f = 65.0 + 20.0 * sin(2 * PI * 5.0 * t)
            val growl = sin(2 * PI * f * t) +
                    0.5 * sin(4 * PI * f * t) +
                    0.3 * (Random.nextDouble() - 0.5)

            // Distort / soft clip
            val distorted = Math.tanh(growl * 2.5)
            buffer[i] = (distorted * env * 28000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateAirHorn(): ShortArray {
        val durationSec = 1.8
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        // 3 rapid MLG air horn blasts
        val blasts = 3
        val blastDur = durationSec / blasts

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val bTime = (t % blastDur) / blastDur
            val env = if (bTime < 0.7) 1.0 else (1.0 - (bTime - 0.7) / 0.3)

            // Air horn tri-tone: 466 Hz, 554 Hz, 622 Hz
            val horn = (sin(2 * PI * 466.16 * t) +
                    sin(2 * PI * 554.37 * t) +
                    sin(2 * PI * 622.25 * t)) / 3.0

            buffer[i] = (horn * env * 28000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateVictoryTrumpet(): ShortArray {
        val durationSec = 2.0
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        val notes = doubleArrayOf(392.0, 523.25, 659.25, 783.99)
        val noteDur = durationSec / notes.size

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val idx = (t / noteDur).toInt().coerceIn(0, notes.lastIndex)
            val localT = (t % noteDur) / noteDur
            val env = sin(localT * PI).coerceAtLeast(0.0)

            val f = notes[idx]
            val sample = sin(2 * PI * f * t) + 0.35 * sin(4 * PI * f * t)
            buffer[i] = (sample * env * 26000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateWalkieSquelch(): ShortArray {
        val durationSec = 1.2
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val sample = if (t < 0.15) {
                // Radio click & burst
                (Random.nextDouble() - 0.5) * 1.5
            } else if (t < 0.8) {
                // Bandpassed static and beep
                sin(2 * PI * 1750.0 * t) * 0.4 + (Random.nextDouble() - 0.5) * 0.3
            } else {
                // End squelch
                (Random.nextDouble() - 0.5) * Math.exp(-(t - 0.8) * 8.0)
            }
            buffer[i] = (sample * 24000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun generateGhostScream(): ShortArray {
        val durationSec = 2.2
        val totalSamples = (SAMPLE_RATE * durationSec).toInt()
        val buffer = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = sin((t / durationSec) * PI)

            // Rising eerie howl with chaotic vibrato
            val base = 400.0 + 800.0 * (t / durationSec)
            val wobble = 60.0 * sin(2 * PI * 12.0 * t)
            val f = base + wobble
            val sample = sin(2 * PI * f * t) + 0.3 * (Random.nextDouble() - 0.5)

            buffer[i] = (sample * env * 27000.0).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return buffer
    }

    private fun resamplePitch(input: ShortArray, pitchFactor: Float): ShortArray {
        if (input.isEmpty() || pitchFactor <= 0.1f) return input
        val newLength = (input.size / pitchFactor).toInt().coerceAtLeast(1)
        val output = ShortArray(newLength)

        for (i in 0 until newLength) {
            val srcIndex = (i * pitchFactor).toInt()
            if (srcIndex < input.size) {
                output[i] = input[srcIndex]
            }
        }
        return output
    }
}
