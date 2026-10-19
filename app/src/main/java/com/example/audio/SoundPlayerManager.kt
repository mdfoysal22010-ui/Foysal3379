package com.example.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import com.example.R
import com.example.model.SoundCategory
import com.example.model.SoundItem
import com.example.model.SynthPreset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class PlaybackState(
    val currentSoundId: String? = null,
    val isPlaying: Boolean = false,
    val volumeBoost: Float = 1.25f, // 125% boost by default for game speaker pick up
    val activeOutputMode: AudioOutputMode = AudioOutputMode.LOUDSPEAKER_BOOST
)

enum class AudioOutputMode(val titleEn: String, val titleBn: String, val desc: String) {
    LOUDSPEAKER_BOOST("Game Speaker Boost", "লাউডস্পিকার বুস্ট", "Open Mic তে টিমমেটদের সহজে শোনানোর জন্য সর্বোচ্চ লাউড সাউন্ড"),
    STANDARD_MEDIA("Standard Media", "সাধারণ মিডিয়া", "হেডফোন বা নরমাল সাউন্ডের জন্য স্ট্যান্ডার্ড ভলিউম"),
    COMMUNICATION("Game Voice Route", "ভয়েস চ্যাট রুট", "Android Voice Communication স্ট্রিম দিয়ে ডায়রেক্ট প্লে")
}

class SoundPlayerManager private constructor(private val appContext: Context) {

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var currentAudioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _soundList = MutableStateFlow<List<SoundItem>>(emptyList())
    val soundList: StateFlow<List<SoundItem>> = _soundList.asStateFlow()

    private val voiceChangerEngine = VoiceChangerEngine(appContext)

    init {
        loadInitialSounds()
    }

    private fun loadInitialSounds() {
        val defaultSounds = mutableListOf(
            // PICCHI SOUNDS
            SoundItem(
                id = "picchi_cry_1",
                titleBn = "বাচ্চা কান্নার প্র্যাঙ্ক",
                titleEn = "Baby Crying Prank",
                subtitle = "গেমের মাঝে বাচ্চার কান্নার আওয়াজ",
                category = SoundCategory.PICCHI,
                imageDrawableRes = R.drawable.meme_picchi_baby_1792410298707,
                iconEmoji = "👶",
                synthPreset = SynthPreset.PICCHI_CRY,
                durationMs = 2400,
                isFavorite = true
            ),
            SoundItem(
                id = "picchi_laugh_1",
                titleBn = "পিচ্চি হাসির শব্দ",
                titleEn = "Picchi Cute Laugh",
                subtitle = "মিষ্টি বাচ্চার হাসির শব্দ",
                category = SoundCategory.PICCHI,
                imageDrawableRes = R.drawable.meme_picchi_baby_1792410298707,
                iconEmoji = "😂",
                synthPreset = SynthPreset.PICCHI_LAUGH,
                durationMs = 2000,
                isFavorite = true
            ),

            // BANGLA MEMES
            SoundItem(
                id = "cid_drama_1",
                titleBn = "C.I.D. - আরে যা...",
                titleEn = "C.I.D. Mystery Sting",
                subtitle = "দয়া দরজা ভাঙ্গো সাসপেন্স সাউন্ড",
                category = SoundCategory.BANGLA_MEME,
                imageDrawableRes = R.drawable.meme_cid_avatar_1792410281191,
                iconEmoji = "🕶️",
                synthPreset = SynthPreset.CID_DRAMA,
                durationMs = 2500,
                isFavorite = true
            ),
            SoundItem(
                id = "dipjol_punch_1",
                titleBn = "ডিপজল বাজু - ঘুষি",
                titleEn = "Dipjol Punch Dialogue",
                subtitle = "হেই বাজু! কার লগে লাগতে আইছো?",
                category = SoundCategory.BANGLA_MEME,
                imageDrawableRes = R.drawable.meme_dipjol_villain_1792410471528,
                iconEmoji = "🥊",
                synthPreset = SynthPreset.DIPJOL_PUNCH,
                durationMs = 1500,
                isFavorite = true
            ),
            SoundItem(
                id = "taheri_beat_1",
                titleBn = "তাহেরী - বলব কি?",
                titleEn = "Taheri Viral Beat",
                subtitle = "বসো বইসা যাও! ঢেলে দেই?",
                category = SoundCategory.BANGLA_MEME,
                imageDrawableRes = null,
                iconEmoji = "☕",
                synthPreset = SynthPreset.TAHERI_BEAT,
                durationMs = 2200,
                isFavorite = true
            ),
            SoundItem(
                id = "salyara_tune_1",
                titleBn = "Salyara TT Song",
                titleEn = "Salyara Funny Tune",
                subtitle = "ভাইরাল টিকটক বানি হুইসেল",
                category = SoundCategory.BANGLA_MEME,
                imageDrawableRes = null,
                iconEmoji = "🎶",
                synthPreset = SynthPreset.SALYARA_TUNE,
                durationMs = 2000,
                isFavorite = true
            ),

            // FREE FIRE SPECIALS
            SoundItem(
                id = "ff_booyah_1",
                titleBn = "Free Fire Booyah!",
                titleEn = "FF Booyah Fanfare",
                subtitle = "বুয়াহ ট্রায়াম্ফ বিজয়ী ট্রাম্পেট",
                category = SoundCategory.FREE_FIRE,
                imageDrawableRes = null,
                iconEmoji = "🏆",
                synthPreset = SynthPreset.FF_BOOYAH,
                durationMs = 2500,
                isFavorite = true
            ),
            SoundItem(
                id = "ff_headshot_1",
                titleBn = "FF হেডশট শব্দ",
                titleEn = "Headshot Ding",
                subtitle = "পারফেক্ট রেড নম্বর হেডশট সাউন্ড",
                category = SoundCategory.FREE_FIRE,
                imageDrawableRes = null,
                iconEmoji = "🎯",
                synthPreset = SynthPreset.FF_HEADSHOT,
                durationMs = 1000,
                isFavorite = true
            ),
            SoundItem(
                id = "ff_awm_1",
                titleBn = "AWM স্নাইপার ফায়ার",
                titleEn = "AWM Sniper Shot",
                subtitle = "ওয়ান শট ডেড স্নাইপার ফায়ার",
                category = SoundCategory.FREE_FIRE,
                imageDrawableRes = null,
                iconEmoji = "💥",
                synthPreset = SynthPreset.FF_AWM_SNIPER,
                durationMs = 2200,
                isFavorite = false
            ),

            // GAMER GIRL VOICE PRANKS
            SoundItem(
                id = "girl_hello_1",
                titleBn = "মেয়ে কন্ঠ - হ্যালো ব্রো",
                titleEn = "Girl Voice - Hello Bro",
                subtitle = "আমার সাথে কাস্টম খেলবা ভাইয়া?",
                category = SoundCategory.GIRL_VOICE,
                imageDrawableRes = R.drawable.meme_gamer_girl_1792410316944,
                iconEmoji = "👧",
                synthPreset = SynthPreset.GIRL_GIGGLE_HELLO,
                durationMs = 1800,
                isFavorite = true
            ),
            SoundItem(
                id = "girl_custom_match",
                titleBn = "মেয়ে কন্ঠ - রিভাইভ দাও",
                titleEn = "Girl Voice - Revive Me!",
                subtitle = "প্লিজ ভাইয়া জলদি রিভাইভ দাও!",
                category = SoundCategory.GIRL_VOICE,
                imageDrawableRes = R.drawable.meme_gamer_girl_1792410316944,
                iconEmoji = "💖",
                synthPreset = SynthPreset.GIRL_GIGGLE_HELLO,
                defaultPitch = 1.35f,
                durationMs = 1800,
                isFavorite = true
            ),

            // MONSTER & ROBOT
            SoundItem(
                id = "robot_alert_1",
                titleBn = "হ্যাকার সাইবার রোবট",
                titleEn = "Hacker Robot Voice",
                subtitle = "সিস্টেম হ্যাকড! ইউ আর গান",
                category = SoundCategory.CYBER_MONSTER,
                imageDrawableRes = null,
                iconEmoji = "🤖",
                synthPreset = SynthPreset.ROBOT_ALERT,
                durationMs = 1600,
                isFavorite = false
            ),
            SoundItem(
                id = "monster_roar_1",
                titleBn = "ভয়ংকর দানব গর্জন",
                titleEn = "Demon Monster Roar",
                subtitle = "ডিপ সাব-বাস হরর ক্রাই",
                category = SoundCategory.CYBER_MONSTER,
                imageDrawableRes = null,
                iconEmoji = "👹",
                synthPreset = SynthPreset.MONSTER_ROAR,
                durationMs = 2400,
                isFavorite = false
            ),
            SoundItem(
                id = "air_horn_1",
                titleBn = "MLG এয়ার হর্ন",
                titleEn = "Triple Air Horn",
                subtitle = "হেভি লাউড প্রাঙ্ক ট্রিপল হর্ন",
                category = SoundCategory.FREE_FIRE,
                imageDrawableRes = null,
                iconEmoji = "📢",
                synthPreset = SynthPreset.AIR_HORN,
                durationMs = 1800,
                isFavorite = true
            ),
            SoundItem(
                id = "walkie_radio_1",
                titleBn = "স্কোয়াড ওয়াকি-টকি",
                titleEn = "Walkie-Talkie Radio",
                subtitle = "মিলিটারি রেডিও স্ট্যাটিক সাউন্ড",
                category = SoundCategory.CYBER_MONSTER,
                imageDrawableRes = null,
                iconEmoji = "📻",
                synthPreset = SynthPreset.RADIO_STATIC_WALKIE,
                durationMs = 1200,
                isFavorite = false
            ),
            SoundItem(
                id = "ghost_scream_1",
                titleBn = "ভূত ডাইনির চিৎকার",
                titleEn = "Ghost Screamer",
                subtitle = "ভূতুড়ে চিৎকারে বন্ধুদের চমকে দিন",
                category = SoundCategory.CYBER_MONSTER,
                imageDrawableRes = null,
                iconEmoji = "👻",
                synthPreset = SynthPreset.GHOST_SCREAM,
                durationMs = 2200,
                isFavorite = false
            )
        )

        // Load any previously saved user custom wav files
        val customDir = File(appContext.filesDir, "custom_sounds")
        if (customDir.exists()) {
            val wavFiles = customDir.listFiles { _, name -> name.endsWith(".wav") } ?: emptyArray()
            for (file in wavFiles) {
                val cleanName = file.nameWithoutExtension.replace('_', ' ')
                defaultSounds.add(
                    0,
                    SoundItem(
                        id = "custom_${file.name}",
                        titleBn = cleanName,
                        titleEn = cleanName,
                        subtitle = "ব্যবহারকারীর রেকর্ড করা অডিও",
                        category = SoundCategory.CUSTOM,
                        imageDrawableRes = null,
                        iconEmoji = "🎙️",
                        localFilePath = file.absolutePath,
                        durationMs = 2000,
                        isFavorite = true
                    )
                )
            }
        }

        _soundList.value = defaultSounds
    }

    fun playSound(soundItem: SoundItem, customPitch: Float? = null) {
        stopAllSounds()

        val boost = _playbackState.value.volumeBoost
        val pitch = customPitch ?: soundItem.defaultPitch

        _playbackState.value = _playbackState.value.copy(
            currentSoundId = soundItem.id,
            isPlaying = true
        )

        playbackJob = scope.launch {
            try {
                val pcmData: ShortArray = if (soundItem.localFilePath != null) {
                    val file = File(soundItem.localFilePath)
                    val raw = voiceChangerEngine.loadWavToPcm(file)
                    if (raw.isNotEmpty()) {
                        SoundSynthesizer.generatePcm(SynthPreset.PICCHI_CRY, pitch, boost) // fallback or process raw
                        // boost and pitch shift raw
                        applyBoostAndPitch(raw, pitch, boost)
                    } else {
                        ShortArray(0)
                    }
                } else if (soundItem.synthPreset != null) {
                    SoundSynthesizer.generatePcm(soundItem.synthPreset, pitch, boost)
                } else {
                    ShortArray(0)
                }

                if (pcmData.isNotEmpty()) {
                    playPcmTrack(pcmData)
                }
            } catch (e: Exception) {
                Log.e("SoundPlayerManager", "Error playing sound", e)
            } finally {
                _playbackState.value = _playbackState.value.copy(
                    currentSoundId = null,
                    isPlaying = false
                )
            }
        }
    }

    private fun applyBoostAndPitch(input: ShortArray, pitch: Float, boost: Float): ShortArray {
        var result = input
        if (kotlin.math.abs(pitch - 1.0f) > 0.05f) {
            val newLen = (input.size / pitch).toInt().coerceAtLeast(1)
            val resampled = ShortArray(newLen)
            for (i in 0 until newLen) {
                val src = (i * pitch).toInt()
                if (src < input.size) resampled[i] = input[src]
            }
            result = resampled
        }
        val boosted = ShortArray(result.size)
        for (i in result.indices) {
            val s = result[i] * boost
            boosted[i] = s.coerceIn(Short.MIN_VALUE.toFloat(), Short.MAX_VALUE.toFloat()).toInt().toShort()
        }
        return boosted
    }

    private fun playPcmTrack(pcmData: ShortArray) {
        val sampleRate = 44100
        val channelConfig = AudioFormat.CHANNEL_OUT_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufSize = (minBufSize * 2).coerceAtLeast(pcmData.size * 2)

        val usage = when (_playbackState.value.activeOutputMode) {
            AudioOutputMode.LOUDSPEAKER_BOOST -> AudioAttributes.USAGE_MEDIA
            AudioOutputMode.COMMUNICATION -> AudioAttributes.USAGE_VOICE_COMMUNICATION
            AudioOutputMode.STANDARD_MEDIA -> AudioAttributes.USAGE_MEDIA
        }

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(usage)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        val format = AudioFormat.Builder()
            .setSampleRate(sampleRate)
            .setChannelMask(channelConfig)
            .setEncoding(audioFormat)
            .build()

        val track = AudioTrack.Builder()
            .setAudioAttributes(audioAttributes)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bufSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        currentAudioTrack = track

        try {
            track.play()
            // Set max volume
            track.setVolume(1.0f)
            val chunkSize = 2048
            var offset = 0
            while (offset < pcmData.size && playbackJob?.isActive == true) {
                val toWrite = minOf(chunkSize, pcmData.size - offset)
                track.write(pcmData, offset, toWrite)
                offset += toWrite
            }
        } catch (e: Exception) {
            Log.e("SoundPlayerManager", "AudioTrack playback error", e)
        } finally {
            try {
                track.stop()
                track.release()
            } catch (e: Exception) {
                Log.w("SoundPlayerManager", "AudioTrack release error", e)
            }
            currentAudioTrack = null
        }
    }

    fun stopAllSounds() {
        playbackJob?.cancel()
        playbackJob = null
        try {
            currentAudioTrack?.stop()
            currentAudioTrack?.release()
        } catch (e: Exception) {
            Log.w("SoundPlayerManager", "Stop error", e)
        }
        currentAudioTrack = null
        _playbackState.value = _playbackState.value.copy(
            currentSoundId = null,
            isPlaying = false
        )
    }

    fun setVolumeBoost(boost: Float) {
        _playbackState.value = _playbackState.value.copy(volumeBoost = boost.coerceIn(0.5f, 2.0f))
    }

    fun setOutputMode(mode: AudioOutputMode) {
        _playbackState.value = _playbackState.value.copy(activeOutputMode = mode)
    }

    fun toggleFavorite(soundId: String) {
        _soundList.value = _soundList.value.map { item ->
            if (item.id == soundId) item.copy(isFavorite = !item.isFavorite) else item
        }
    }

    fun addCustomSound(soundItem: SoundItem) {
        val current = _soundList.value.toMutableList()
        current.add(0, soundItem)
        _soundList.value = current
    }

    fun deleteSound(soundId: String) {
        val target = _soundList.value.find { it.id == soundId }
        if (target?.localFilePath != null) {
            try {
                File(target.localFilePath).delete()
            } catch (e: Exception) {
                Log.w("SoundPlayerManager", "Error deleting file", e)
            }
        }
        _soundList.value = _soundList.value.filter { it.id != soundId }
    }

    companion object {
        @Volatile
        private var instance: SoundPlayerManager? = null

        fun getInstance(context: Context): SoundPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: SoundPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
