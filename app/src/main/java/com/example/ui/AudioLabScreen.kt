package com.example.ui

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsVoice
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.AudioOutputMode
import com.example.audio.SoundPlayerManager
import com.example.audio.SoundSynthesizer
import com.example.model.SoundCategory
import com.example.model.SoundItem
import com.example.model.SynthPreset
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.GamerGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StopRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.launch

@Composable
fun AudioLabScreen(
    soundPlayerManager: SoundPlayerManager
) {
    val context = LocalContext.current
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val playbackState by soundPlayerManager.playbackState.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var isTestPlaying by remember { mutableStateOf(false) }

    // Inspect hardware devices
    val devices = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        } else {
            emptyArray()
        }
    }

    val hasHeadset = devices.any {
        it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                it.type == AudioDeviceInfo.TYPE_USB_HEADSET
    }
    val hasBluetooth = devices.any {
        it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 60.dp)
    ) {
        // --- HEADER CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SettingsVoice,
                        contentDescription = "Lab",
                        tint = NeonCyan,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Audio Hardware & Voice Chat Lab",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "Inspect audio routes & game voice chat status",
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- HARDWARE ROUTE INSPECTION CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🔊 বর্তমান অডিও কানেকশন (Audio Routes)",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                RouteStatusItem(
                    icon = Icons.Default.Speaker,
                    name = "ফোন লাউডস্পিকার (Phone Loudspeaker)",
                    status = "সক্রিয় (Active - রিকমেন্ডেড)",
                    isActive = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                RouteStatusItem(
                    icon = Icons.Default.Headphones,
                    name = "ওয়্যার্ড হেডফোন (Wired Headset)",
                    status = if (hasHeadset) "কানেক্টেড (Connected)" else "ডিসকানেক্টেড (Not plugged)",
                    isActive = hasHeadset
                )

                Spacer(modifier = Modifier.height(8.dp))

                RouteStatusItem(
                    icon = Icons.Default.VolumeUp,
                    name = "ব্লুটুথ ইয়ারবাড / হেডসেট (Bluetooth)",
                    status = if (hasBluetooth) "কানেক্টেড (Connected)" else "ডিসকানেক্টেড (No device)",
                    isActive = hasBluetooth
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- AUDIO OUTPUT MODE SELECTOR ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🎮 অডিও প্লেব্যাক মোড (Audio Output Stream)",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = "ফ্রি ফায়ারে টিমমেটদের শোনানোর জন্য সঠিক মোড নির্বাচন করুন",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                for (mode in AudioOutputMode.values()) {
                    val isSelected = (playbackState.activeOutputMode == mode)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) DarkSurfaceElevated else Color.Transparent)
                            .clickable { soundPlayerManager.setOutputMode(mode) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { soundPlayerManager.setOutputMode(mode) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = NeonCyan,
                                unselectedColor = TextMuted
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${mode.titleBn} (${mode.titleEn})",
                                color = if (isSelected) NeonCyan else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                            Text(
                                text = mode.desc,
                                color = TextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- IN-GAME VOICE CHAT CHECKLIST ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "✅ Free Fire ভয়েস চ্যাট চেকলিস্ট",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                ChecklistRow(
                    text = "সাউন্ডবোর্ড ভলিউম বুস্ট: ${(playbackState.volumeBoost * 100).toInt()}% (Super Loud)",
                    isComplete = true
                )
                ChecklistRow(
                    text = "মোবাইল মিডিয়া ভলিউম: ফুল বা ৮০%+ রাখুন",
                    isComplete = true
                )
                ChecklistRow(
                    text = "Free Fire ইন-গেম মাইক: 'Open' / চালু রাখুন",
                    isComplete = true
                )
                ChecklistRow(
                    text = "Free Fire মাইক সংবেদনশীলতা (Sensitivity): ১০০%",
                    isComplete = true
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- HARDWARE SOUND TEST BUTTON ---
        Button(
            onClick = {
                if (isTestPlaying) {
                    soundPlayerManager.stopAllSounds()
                    isTestPlaying = false
                } else {
                    isTestPlaying = true
                    // Play test sound
                    val testItem = SoundItem(
                        id = "test_ping",
                        titleBn = "টেস্ট সাউন্ড",
                        titleEn = "Test Audio",
                        subtitle = "Hardware diagnostic",
                        category = SoundCategory.ALL,
                        synthPreset = SynthPreset.FF_BOOYAH
                    )
                    soundPlayerManager.playSound(testItem)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("run_audio_test_btn"),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isTestPlaying) StopRed else NeonCyan
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = if (isTestPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = "Test",
                tint = Color.Black
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isTestPlaying) "টেস্ট থামান (Stop Test)" else "সাউন্ডবোর্ড টেস্ট প্লে (Run Audio Test)",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun RouteStatusItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    name: String,
    status: String,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isActive) GamerGreen.copy(alpha = 0.2f) else DarkSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = name,
                tint = if (isActive) GamerGreen else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(
                status,
                color = if (isActive) GamerGreen else TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun ChecklistRow(text: String, isComplete: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Check,
            contentDescription = "Check",
            tint = GamerGreen,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, color = TextSecondary, fontSize = 11.sp)
    }
}
