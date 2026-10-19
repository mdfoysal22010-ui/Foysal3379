package com.example.model

enum class SoundCategory(val displayNameEn: String, val displayNameBn: String, val emoji: String) {
    ALL("All", "সবগুলো", "🔥"),
    PICCHI("Picchi / Baby", "পিচ্চি / বাচ্চা", "👶"),
    BANGLA_MEME("Bangla Memes", "বাংলা মিম", "🎭"),
    FREE_FIRE("Free Fire FX", "ফ্রি ফায়ার স্পেশাল", "🎯"),
    GIRL_VOICE("Gamer Girl", "মেয়ে কন্ঠ", "👧"),
    CYBER_MONSTER("Monster & Bot", "রোবট ও মনস্টার", "🤖"),
    CUSTOM("Custom Recorded", "আমার রেকর্ড", "🎙️")
}

enum class SynthPreset {
    PICCHI_CRY,
    PICCHI_LAUGH,
    CID_DRAMA,
    DIPJOL_PUNCH,
    TAHERI_BEAT,
    SALYARA_TUNE,
    FF_BOOYAH,
    FF_HEADSHOT,
    FF_AWM_SNIPER,
    GIRL_GIGGLE_HELLO,
    ROBOT_ALERT,
    MONSTER_ROAR,
    AIR_HORN,
    VICTORY_TRUMPET,
    RADIO_STATIC_WALKIE,
    GHOST_SCREAM
}

data class SoundItem(
    val id: String,
    val titleBn: String,
    val titleEn: String,
    val subtitle: String,
    val category: SoundCategory,
    val imageDrawableRes: Int? = null,
    val iconEmoji: String = "🎵",
    val synthPreset: SynthPreset? = null,
    val localFilePath: String? = null,
    val durationMs: Int = 2000,
    val isFavorite: Boolean = false,
    val defaultPitch: Float = 1.0f,
    val defaultSpeed: Float = 1.0f
)

enum class VoiceEffectFilter(
    val titleEn: String,
    val titleBn: String,
    val emoji: String,
    val pitchMultiplier: Float,
    val speedMultiplier: Float,
    val isRobot: Boolean = false,
    val isAlien: Boolean = false,
    val isEcho: Boolean = false,
    val isTelephone: Boolean = false,
    val isMonster: Boolean = false
) {
    ORIGINAL("Original", "আসল কন্ঠ", "🎙️", 1.0f, 1.0f),
    PICCHI_BABY("Picchi / Baby", "পিচ্চি বাচ্চা", "👶", 1.65f, 1.15f),
    GAMER_GIRL("Gamer Girl", "মেয়ে কন্ঠ", "👧", 1.35f, 1.05f),
    DEEP_DON("Deep Boss / Don", "ভারী ডন কন্ঠ", "🕶️", 0.72f, 0.95f),
    CHIPMUNK("Fast Chipmunk", "কার্টুন কাঠবিড়ালি", "🐿️", 1.85f, 1.35f),
    ROBOT("Cyber Robot", "সাইবার রোবট", "🤖", 1.0f, 1.0f, isRobot = true),
    ALIEN("Space Alien", "এলিয়েন কন্ঠ", "👽", 1.25f, 1.0f, isAlien = true),
    MONSTER("Dark Demon", "দানব মনস্টার", "👹", 0.55f, 0.9f, isMonster = true),
    WALKIE_TALKIE("Radio / Walkie", "ওয়াকি-টকি", "📻", 1.0f, 1.0f, isTelephone = true),
    BIG_ECHO("Grand Echo", "প্রতিধ্বনি / ইকো", "🔊", 1.0f, 1.0f, isEcho = true)
}
