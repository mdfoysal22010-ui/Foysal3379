package com.example.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import com.example.model.VoiceEffectFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

class VoiceChangerEngine(private val context: Context) {

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var isRecording = false

    private val recordedBuffer = ByteArrayOutputStream()

    fun isRecordingNow(): Boolean = isRecording

    fun startRecording(onAmplitudeUpdate: (Float) -> Unit, onError: (String) -> Unit) {
        if (isRecording) return

        try {
            val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
            val bufferSize = (minBufSize * 2).coerceAtLeast(4096)

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                onError("Failed to initialize audio recording hardware")
                audioRecord?.release()
                audioRecord = null
                return
            }

            recordedBuffer.reset()
            audioRecord?.startRecording()
            isRecording = true

            recordingJob = CoroutineScope(Dispatchers.IO).launch {
                val readChunk = ByteArray(2048)
                while (isActive && isRecording) {
                    val bytesRead = audioRecord?.read(readChunk, 0, readChunk.size) ?: -1
                    if (bytesRead > 0) {
                        synchronized(recordedBuffer) {
                            recordedBuffer.write(readChunk, 0, bytesRead)
                        }

                        // Calculate RMS amplitude for UI audio visualizer
                        var sum = 0.0
                        val shortsCount = bytesRead / 2
                        val shortBuffer = ByteBuffer.wrap(readChunk, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                        for (i in 0 until shortsCount) {
                            val sample = shortBuffer.get(i).toDouble()
                            sum += sample * sample
                        }
                        val rms = sqrt(sum / shortsCount.coerceAtLeast(1))
                        val normalized = (rms / 32768.0).toFloat().coerceIn(0f, 1f)
                        onAmplitudeUpdate(normalized)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceChangerEngine", "Error starting recording", e)
            onError(e.message ?: "Recording error")
            stopRecording()
        }
    }

    fun stopRecording(): ShortArray {
        isRecording = false
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w("VoiceChangerEngine", "Error stopping AudioRecord", e)
        }
        audioRecord = null

        val rawBytes = synchronized(recordedBuffer) { recordedBuffer.toByteArray() }
        if (rawBytes.isEmpty()) return ShortArray(0)

        val shortCount = rawBytes.size / 2
        val pcmData = ShortArray(shortCount)
        ByteBuffer.wrap(rawBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pcmData)
        return pcmData
    }

    fun applyVoiceEffect(
        rawPcm: ShortArray,
        filter: VoiceEffectFilter,
        customPitch: Float = 1.0f,
        customSpeed: Float = 1.0f
    ): ShortArray {
        if (rawPcm.isEmpty()) return rawPcm

        // Combined pitch multiplier
        val targetPitch = filter.pitchMultiplier * customPitch
        val targetSpeed = filter.speedMultiplier * customSpeed

        var processed = rawPcm

        // 1. Pitch shift / Time stretch
        if (kotlin.math.abs(targetPitch - 1.0f) > 0.02f) {
            processed = resamplePcm(processed, targetPitch)
        }

        // 2. Specialized modulation filters
        if (filter.isRobot) {
            processed = applyRobotEffect(processed)
        }
        if (filter.isAlien) {
            processed = applyAlienEffect(processed)
        }
        if (filter.isMonster) {
            processed = applyMonsterEffect(processed)
        }
        if (filter.isTelephone) {
            processed = applyTelephoneFilter(processed)
        }
        if (filter.isEcho) {
            processed = applyEchoEffect(processed, delayMs = 180, decay = 0.5f)
        }

        return processed
    }

    private fun resamplePcm(input: ShortArray, pitchFactor: Float): ShortArray {
        if (input.isEmpty() || pitchFactor <= 0.1f) return input
        val newLength = (input.size / pitchFactor).toInt().coerceAtLeast(1)
        val output = ShortArray(newLength)

        for (i in 0 until newLength) {
            val srcIdx = (i * pitchFactor).toInt()
            if (srcIdx < input.size) {
                output[i] = input[srcIdx]
            }
        }
        return output
    }

    private fun applyRobotEffect(input: ShortArray): ShortArray {
        val output = ShortArray(input.size)
        val carrierFreq = 55.0 // Hz
        for (i in input.indices) {
            val t = i.toDouble() / sampleRate
            val carrier = sin(2 * PI * carrierFreq * t)
            val sample = input[i] * carrier
            output[i] = sample.coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toInt().toShort()
        }
        return output
    }

    private fun applyAlienEffect(input: ShortArray): ShortArray {
        val output = ShortArray(input.size)
        for (i in input.indices) {
            val t = i.toDouble() / sampleRate
            val wobble = sin(2 * PI * 14.0 * t) * 0.4
            val sample = input[i] * (0.8 + wobble)
            output[i] = sample.coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toInt().toShort()
        }
        return output
    }

    private fun applyMonsterEffect(input: ShortArray): ShortArray {
        val output = ShortArray(input.size)
        for (i in input.indices) {
            val t = i.toDouble() / sampleRate
            // Sub-harmonic octave generator & saturation
            val subOctave = if (i > 1) input[i / 2] * 0.4 else 0.0
            val combined = (input[i] * 0.8 + subOctave)
            // Soft-clipping distortion
            val normalized = combined / 32768.0
            val distorted = Math.tanh(normalized * 1.8) * 32767.0
            output[i] = distorted.coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toInt().toShort()
        }
        return output
    }

    private fun applyTelephoneFilter(input: ShortArray): ShortArray {
        val output = ShortArray(input.size)
        // Simple IIR low-pass & high-pass combination
        var prevHigh = 0.0
        var prevLow = 0.0
        for (i in input.indices) {
            val sample = input[i].toDouble()
            // High-pass ~ 400Hz
            val high = 0.92 * (prevHigh + sample - (if (i > 0) input[i - 1].toDouble() else 0.0))
            prevHigh = high
            // Low-pass ~ 3200Hz
            val low = prevLow + 0.35 * (high - prevLow)
            prevLow = low
            // Add radio grit
            val grit = Math.tanh(low / 20000.0) * 22000.0
            output[i] = grit.coerceIn(Short.MIN_VALUE.toDouble(), Short.MAX_VALUE.toDouble()).toInt().toShort()
        }
        return output
    }

    private fun applyEchoEffect(input: ShortArray, delayMs: Int, decay: Float): ShortArray {
        val delaySamples = (sampleRate * (delayMs / 1000.0)).toInt()
        val output = ShortArray(input.size + delaySamples)

        for (i in input.indices) {
            output[i] = input[i]
        }

        for (i in delaySamples until output.size) {
            val echoSource = i - delaySamples
            if (echoSource < input.size) {
                val mixed = output[i] + (input[echoSource] * decay).toInt()
                output[i] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
            }
        }
        return output
    }

    fun savePcmToWav(pcm: ShortArray, fileName: String): File {
        val dir = File(context.filesDir, "custom_sounds")
        if (!dir.exists()) dir.mkdirs()

        val wavFile = File(dir, if (fileName.endsWith(".wav")) fileName else "$fileName.wav")
        FileOutputStream(wavFile).use { fos ->
            writeWavHeader(fos, pcm.size * 2, sampleRate, 1, 16)
            val byteBuffer = ByteBuffer.allocate(pcm.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            for (sample in pcm) {
                byteBuffer.putShort(sample)
            }
            fos.write(byteBuffer.array())
        }
        return wavFile
    }

    fun loadWavToPcm(file: File): ShortArray {
        if (!file.exists()) return ShortArray(0)
        FileInputStream(file).use { fis ->
            // Skip 44 bytes header
            fis.skip(44)
            val bytes = fis.readBytes()
            val shortCount = bytes.size / 2
            val pcm = ShortArray(shortCount)
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(pcm)
            return pcm
        }
    }

    private fun writeWavHeader(
        out: FileOutputStream,
        totalAudioLen: Int,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalDataLen = totalAudioLen + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8

        val header = ByteArray(44)
        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // Subchunk1Size for PCM
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // AudioFormat 1 = PCM
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (channels * bitsPerSample / 8).toByte() // block align
        header[33] = 0
        header[34] = bitsPerSample.toByte()
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        out.write(header, 0, 44)
    }
}
