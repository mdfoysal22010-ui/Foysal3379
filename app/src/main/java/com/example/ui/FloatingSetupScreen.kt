package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.service.FloatingSoundboardService
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.ElectricAmber
import com.example.ui.theme.ElectricOrange
import com.example.ui.theme.GamerGreen
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.StopRed
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun FloatingSetupScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasOverlayPermission by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }
    var isServiceRunning by remember { mutableStateOf(false) }

    // Recheck permission when app returns from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        hasOverlayPermission = Settings.canDrawOverlays(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .padding(bottom = 60.dp)
    ) {
        // --- HERO STATUS CARD ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(
                1.5.dp,
                if (hasOverlayPermission) GamerGreen else ElectricAmber
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(
                                    if (hasOverlayPermission) GamerGreen.copy(alpha = 0.2f)
                                    else ElectricAmber.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (hasOverlayPermission) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = "Status",
                                tint = if (hasOverlayPermission) GamerGreen else ElectricAmber,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ফ্লোটিং উইন্ডো পারমিশন",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (hasOverlayPermission) "পারমিশন চালু আছে ✅" else "অনুমতি দেওয়া হয়নি ⚠️",
                                color = if (hasOverlayPermission) GamerGreen else ElectricAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Free Fire খেলার সময় যাতে গেমের স্ক্রিনের উপরে ছোট্ট একটি ভাসমান বাটন থাকে এবং ট্যাপ করলেই সাউন্ডবোর্ড ওপেন হয়, সেজন্য 'Draw over other apps' অনুমতি দিন।",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (!hasOverlayPermission) {
                    Button(
                        onClick = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            overlayPermissionLauncher.launch(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("request_overlay_permission_btn")
                    ) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = "Permission",
                            tint = Color.Black
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "পারমিশন অন করুন (Allow Display Over Apps)",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    // Start / Stop Floating Service buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                FloatingSoundboardService.start(context)
                                isServiceRunning = true
                                Toast.makeText(
                                    context,
                                    "ফ্লোটিং বাটন গেমের উপরে চালু হয়েছে! 🎮",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GamerGreen),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("start_floating_service_btn")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start", tint = Color.Black)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("চালু করুন", color = Color.Black, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                FloatingSoundboardService.stop(context)
                                isServiceRunning = false
                                Toast.makeText(context, "ফ্লোটিং উইন্ডো বন্ধ করা হয়েছে", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StopRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                                .testTag("stop_floating_service_btn")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("বন্ধ করুন", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- STEP-BY-STEP FREE FIRE GAMING GUIDE ---
        Text(
            text = "🔥 Free Fire এ কিভাবে প্রাঙ্ক করবেন? (Setup Guide)",
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        SetupStepCard(
            stepNumber = "১",
            title = "ফ্লোটিং উইন্ডো অন করুন",
            description = "উপরের বাটনে ক্লিক করে 'চালু করুন'। স্ক্রিনে একটি ছোট গোল আইকন দেখতে পাবেন যা টেনে যে কোনো জায়গায় নেওয়া যায়।"
        )

        Spacer(modifier = Modifier.height(10.dp))

        SetupStepCard(
            stepNumber = "২",
            title = "Free Fire গেম ওপেন করুন",
            description = "ফ্রি ফায়ারে ঢুকুন। গেমের মাঝখানেও এই ফ্লোটিং আইকনটি ভাসবে, তাই আপনার গেমের ফায়ার বাটনের পথে আসবে না।"
        )

        Spacer(modifier = Modifier.height(10.dp))

        SetupStepCard(
            stepNumber = "৩",
            title = "গেমের মাইক ও স্পিকার সেটআপ",
            description = "Free Fire Settings ➜ Sound এ গিয়ে: \n• Microphone Sensitivity: ১০০% করে দিন। \n• Voice Channel: 'Squad' বা 'All' অন রাখুন। \n• স্পিকার অন রাখুন যাতে স্কোয়াড মেম্বাররা আপনার ভয়েস স্পষ্ট শোনে।"
        )

        Spacer(modifier = Modifier.height(10.dp))

        SetupStepCard(
            stepNumber = "৪",
            title = "ম্যাচের মধ্যে ট্যাপ করে প্রাঙ্ক শুরু করুন!",
            description = "লবিতে বা ম্যাচের মধ্যে ফ্লোটিং বাবলে ট্যাপ করলেই সাউন্ডবোর্ড ওপেন হবে! পিচ্চি কান্না, মেয়ে কন্ঠ বা ডিপজলের ডায়লগে ট্যাপ করলেই আপনার ফোন থেকে লাউড সাউন্ড বেজে উঠবে এবং আপনার মাইক দিয়ে সরাসরি স্কোয়াডের সবার কানে যাবে!"
        )

        Spacer(modifier = Modifier.height(16.dp))

        // --- PRO TIP BOX ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text("💡", fontSize = 20.sp)
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "প্রো টিপস (Pro Tip):",
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "আমাদের সাউন্ডবোর্ডে 'Volume Boost' স্লাইডার ১২৫% - ১৫০% এ সেট করে রাখুন। এতে মোবাইল স্পিকার থেকে খুব জোরে সাউন্ড বাজবে এবং ফ্রি ফায়ারের ইন-গেম মাইক সেটা হাই কোয়ালিটিতে ক্যাপচার করে স্কোয়াডের সবার কাছে ব্রডকাস্ট করবে!",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
fun SetupStepCard(
    stepNumber: String,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(NeonCyan),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stepNumber,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
