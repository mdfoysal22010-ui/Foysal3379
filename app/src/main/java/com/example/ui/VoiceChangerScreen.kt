package com.example.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.audio.SoundPlayerManager
import com.example.audio.SoundSynthesizer
import com.example.audio.VoiceChangerEngine
import com.example.model.SoundCategory
import com.example.model.SoundItem
import com.example.model.VoiceEffectFilter
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.GamerGreen
import com.example.ui.theme.GamerPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanDark
import com.example.ui.theme.StopRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

@Composable
fun VoiceChangerScreen(
    soundPlayerManager: SoundPlayerManager
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val voiceEngine = remember { VoiceChangerEngine(context) }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "ভয়েস রেকর্ড করতে মাইক পারমিশন প্রয়োজন", Toast.LENGTH_SHORT).show()
        }
    }

    var isRecording by remember { mutableStateOf(false) }
    var liveAmplitude by remember { mutableFloatStateOf(0f) }
    var recordedRawPcm by remember { mutableStateOf<ShortArray?>(null) }
    var processedPcm by remember { mutableStateOf<ShortArray?>(null) }

    var selectedFilter by remember { mutableStateOf(VoiceEffectFilter.PICCHI_BABY) }
    var customPitch by remember { mutableFloatStateOf(1.0f) }
    var customSpeed by remember { mutableFloatStateOf(1.0f) }

    var isPreviewPlaying by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    val recentAmplitudes = remember { mutableStateListOf<Float>() }

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
                        .background(Brush.linearGradient(listOf(GamerPink, NeonCyan))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🎙️", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Gaming Voice Changer",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "নিজের কন্ঠ পরিবর্তন করে Free Fire এ প্রাঙ্ক করুন!",
                        color = NeonCyan,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- RECORDING STUDIO CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (isRecording) StopRed else DarkBorder
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isRecording) "🔴 রেকর্ড হচ্ছে... কথা বলুন!" else if (recordedRawPcm != null) "✅ অডিও রেকর্ড সম্পন্ন!" else "নিচের বাটনে চাপ দিয়ে কন্ঠ রেকর্ড করুন",
                    color = if (isRecording) StopRed else if (recordedRawPcm != null) GamerGreen else TextSecondary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Waveform Visualizer Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    WaveformVisualizer(
                        amplitude = if (isRecording) liveAmplitude else if (isPreviewPlaying) 0.65f else 0.05f,
                        isActive = isRecording || isPreviewPlaying
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // BIG RECORD / STOP BUTTON
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) Brush.linearGradient(listOf(StopRed, StopRed)) else Brush.linearGradient(listOf(NeonCyan, NeonCyanDark))
                        )
                        .clickable {
                            if (!hasMicPermission) {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            } else {
                                if (isRecording) {
                                    // Stop Recording
                                    val raw = voiceEngine.stopRecording()
                                    isRecording = false
                                    recordedRawPcm = raw
                                    // Process with current filter
                                    coroutineScope.launch {
                                        processedPcm = voiceEngine.applyVoiceEffect(
                                            raw,
                                            selectedFilter,
                                            customPitch,
                                            customSpeed
                                        )
                                    }
                                } else {
                                    // Start Recording
                                    recordedRawPcm = null
                                    processedPcm = null
                                    isRecording = true
                                    voiceEngine.startRecording(
                                        onAmplitudeUpdate = { amp ->
                                            liveAmplitude = amp
                                        },
                                        onError = { err ->
                                            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                                            isRecording = false
                                        }
                                    )
                                }
                            }
                        }
                        .testTag("record_voice_toggle_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Record",
                        tint = Color.Black,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isRecording) "থামাতে ট্যাপ করুন (Stop)" else "রেকর্ড করতে ট্যাপ করুন (Tap to Record)",
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- VOICE EFFECT FILTERS ---
        Text(
            text = "ভয়েস ইফেক্ট ফিল্টার পছন্দ করুন:",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(VoiceEffectFilter.values()) { filter ->
                val isSelected = (selectedFilter == filter)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) NeonCyan else DarkSurface)
                        .border(
                            1.dp,
                            if (isSelected) NeonCyanDark else DarkBorder,
                            RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            selectedFilter = filter
                            recordedRawPcm?.let { raw ->
                                coroutineScope.launch {
                                    processedPcm = voiceEngine.applyVoiceEffect(
                                        raw,
                                        filter,
                                        customPitch,
                                        customSpeed
                                    )
                                }
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .testTag("filter_${filter.name.lowercase()}")
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(filter.emoji, fontSize = 22.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = filter.titleBn,
                            color = if (isSelected) Color.Black else TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                        Text(
                            text = filter.titleEn,
                            color = if (isSelected) Color(0xFF1E293B) else TextSecondary,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- FINE TUNING SLIDERS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "🎛️ ফাইন টিউন কন্ট্রোল (Fine Tuning)",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Pitch multiplier slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("কন্ঠের তীক্ষ্ণতা (Pitch Multiplier):", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = String.format("%.2fx", customPitch),
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = customPitch,
                    onValueChange = {
                        customPitch = it
                        recordedRawPcm?.let { raw ->
                            processedPcm = voiceEngine.applyVoiceEffect(raw, selectedFilter, it, customSpeed)
                        }
                    },
                    valueRange = 0.5f..1.8f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = DarkBorder
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Speed multiplier slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("কথা বলার গতি (Speed Multiplier):", color = TextSecondary, fontSize = 11.sp)
                    Text(
                        text = String.format("%.2fx", customSpeed),
                        color = ElectricAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = customSpeed,
                    onValueChange = {
                        customSpeed = it
                        recordedRawPcm?.let { raw ->
                            processedPcm = voiceEngine.applyVoiceEffect(raw, selectedFilter, customPitch, it)
                        }
                    },
                    valueRange = 0.6f..1.6f,
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricAmber,
                        activeTrackColor = ElectricAmber,
                        inactiveTrackColor = DarkBorder
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // --- PREVIEW & SAVE BUTTONS ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // LISTEN PREVIEW BUTTON
            Button(
                onClick = {
                    if (isPreviewPlaying) {
                        soundPlayerManager.stopAllSounds()
                        isPreviewPlaying = false
                    } else {
                        val toPlay = processedPcm ?: recordedRawPcm
                        if (toPlay == null || toPlay.isEmpty()) {
                            Toast.makeText(context, "প্রথমে অডিও রেকর্ড করুন", Toast.LENGTH_SHORT).show()
                        } else {
                            isPreviewPlaying = true
                            // Save temp file or play via custom item
                            val tempFile = voiceEngine.savePcmToWav(toPlay, "temp_preview")
                            val tempItem = SoundItem(
                                id = "temp_preview",
                                titleBn = "প্রিভিউ",
                                titleEn = "Preview",
                                subtitle = "Preview",
                                category = SoundCategory.CUSTOM,
                                localFilePath = tempFile.absolutePath
                            )
                            soundPlayerManager.playSound(tempItem)
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("preview_voice_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPreviewPlaying) StopRed else NeonCyan
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = if (isPreviewPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = "Preview",
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPreviewPlaying) "থামান" else "শুনে দেখুন",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // SAVE TO SOUNDBOARD BUTTON
            Button(
                onClick = {
                    val pcm = processedPcm ?: recordedRawPcm
                    if (pcm == null || pcm.isEmpty()) {
                        Toast.makeText(context, "প্রথমে অডিও রেকর্ড করুন", Toast.LENGTH_SHORT).show()
                    } else {
                        showSaveDialog = true
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("save_to_soundboard_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = GamerGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BookmarkAdd,
                    contentDescription = "Save",
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "সাউন্ডবোর্ডে সেভ",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }

    // --- SAVE TO SOUNDBOARD DIALOG ---
    if (showSaveDialog) {
        var soundTitle by remember { mutableStateOf("${selectedFilter.titleBn} প্রাঙ্ক") }
        var selectedCategoryToSave by remember { mutableStateOf(SoundCategory.CUSTOM) }

        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    "সাউন্ডবোর্ডে সেভ করুন",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column {
                    Text(
                        "এই ভয়েসটি আপনার Free Fire ফ্লোটিং সাউন্ডবোর্ডে যুক্ত হবে এবং গেম চলাকালীন বাটন ট্যাপ করে বাজাতে পারবেন।",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = soundTitle,
                        onValueChange = { soundTitle = it },
                        label = { Text("সাউন্ডের নাম (Title)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sound_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkBorder,
                            focusedContainerColor = DarkSurfaceElevated,
                            unfocusedContainerColor = DarkSurface
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pcm = processedPcm ?: recordedRawPcm
                        if (pcm != null && pcm.isNotEmpty()) {
                            val cleanFileName = "prank_${System.currentTimeMillis()}"
                            val savedFile = voiceEngine.savePcmToWav(pcm, cleanFileName)
                            val newItem = SoundItem(
                                id = "custom_${UUID.randomUUID()}",
                                titleBn = soundTitle.ifBlank { "আমার ভয়েস প্রাঙ্ক" },
                                titleEn = "Custom Prank Voice",
                                subtitle = "${selectedFilter.titleBn} ফিল্টার",
                                category = selectedCategoryToSave,
                                iconEmoji = selectedFilter.emoji,
                                localFilePath = savedFile.absolutePath,
                                durationMs = 2500,
                                isFavorite = true
                            )
                            soundPlayerManager.addCustomSound(newItem)
                            Toast.makeText(
                                context,
                                "সাউন্ডবোর্ড এবং ফ্লোটিং উইন্ডোতে সেভ হয়েছে! 🎉",
                                Toast.LENGTH_LONG
                            ).show()
                            showSaveDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Text("সেভ করুন", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("বাতিল", color = TextSecondary)
                }
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun WaveformVisualizer(
    amplitude: Float,
    isActive: Boolean
) {
    val barCount = 28
    val animProgress = remember { Animatable(0f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            animProgress.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(400, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            animProgress.snapTo(0f)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val barWidth = size.width / (barCount * 1.6f)
        val space = barWidth * 0.6f
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val offsetFactor = kotlin.math.sin((i / barCount.toFloat()) * kotlin.math.PI).toFloat()
            val dynamicHeight = if (isActive) {
                val wave = kotlin.math.abs(kotlin.math.sin(i * 0.45 + animProgress.value * 3.0)).toFloat()
                (size.height * 0.15f + size.height * 0.7f * amplitude * wave * offsetFactor).coerceAtLeast(6f)
            } else {
                6f
            }

            val x = i * (barWidth + space) + space / 2f
            val top = centerY - dynamicHeight / 2f

            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(NeonCyan, ElectricAmber)
                ),
                topLeft = Offset(x, top),
                size = Size(barWidth, dynamicHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}
