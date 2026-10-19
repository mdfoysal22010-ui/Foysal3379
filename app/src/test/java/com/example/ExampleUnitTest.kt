package com.example

import com.example.audio.SoundSynthesizer
import com.example.model.SynthPreset
import com.example.model.VoiceEffectFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testSynthesizerGeneratesPcm() {
        val babyCryPcm = SoundSynthesizer.generatePcm(SynthPreset.PICCHI_CRY, pitchFactor = 1.0f, volumeBoost = 1.0f)
        assertTrue(babyCryPcm.isNotEmpty())

        val headshotPcm = SoundSynthesizer.generatePcm(SynthPreset.FF_HEADSHOT, pitchFactor = 1.2f, volumeBoost = 1.5f)
        assertTrue(headshotPcm.isNotEmpty())

        val booyahPcm = SoundSynthesizer.generatePcm(SynthPreset.FF_BOOYAH, pitchFactor = 1.0f, volumeBoost = 1.0f)
        assertTrue(booyahPcm.isNotEmpty())
    }

    @Test
    fun testVoiceEffectFilters() {
        assertTrue(VoiceEffectFilter.PICCHI_BABY.pitchMultiplier > 1.0f)
        assertTrue(VoiceEffectFilter.DEEP_DON.pitchMultiplier < 1.0f)
        assertTrue(VoiceEffectFilter.ROBOT.isRobot)
        assertTrue(VoiceEffectFilter.MONSTER.isMonster)
    }
}
