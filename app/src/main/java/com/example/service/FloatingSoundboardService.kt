package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.audio.SoundPlayerManager
import com.example.model.SoundCategory
import com.example.model.SoundItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class FloatingSoundboardService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var soundPlayerManager: SoundPlayerManager

    private var floatingBubbleView: View? = null
    private var expandedSoundboardView: View? = null

    private var bubbleLayoutParams: WindowManager.LayoutParams? = null
    private var expandedLayoutParams: WindowManager.LayoutParams? = null

    private var isExpanded = false
    private val scope = CoroutineScope(Dispatchers.Main)
    private var soundListJob: Job? = null
    private var playbackStateJob: Job? = null

    private var selectedCategory: SoundCategory = SoundCategory.ALL
    private var cachedSounds: List<SoundItem> = emptyList()

    private var soundGrid: GridLayout? = null
    private var playingIndicatorText: TextView? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        soundPlayerManager = SoundPlayerManager.getInstance(this)

        startAsForeground()

        if (Settings.canDrawOverlays(this)) {
            initFloatingViews()
            observeSoundData()
        } else {
            stopSelf()
        }
    }

    private fun startAsForeground() {
        val channelId = "floating_soundboard_channel"
        val channelName = "Free Fire Floating Soundboard"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating soundboard control over Free Fire"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("FF Voice Prank Active")
            .setContentText("ফ্লোটিং সাউন্ডবোর্ড গেমের উপরে চালু আছে")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun initFloatingViews() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        // 1. Create Draggable Floating Bubble
        bubbleLayoutParams = WindowManager.LayoutParams(
            dpToPx(60),
            dpToPx(60),
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 350
        }

        floatingBubbleView = createBubbleView()
        windowManager.addView(floatingBubbleView, bubbleLayoutParams)

        // 2. Create Expanded Soundboard Layout
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.92f).toInt().coerceAtMost(dpToPx(380))
        val height = (displayMetrics.heightPixels * 0.65f).toInt().coerceAtMost(dpToPx(520))

        expandedLayoutParams = WindowManager.LayoutParams(
            width,
            height,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        expandedSoundboardView = createExpandedSoundboardView()
    }

    private fun createBubbleView(): View {
        val bubble = FrameLayout(this)

        val bg = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.parseColor("#0F172A"))
            setStroke(dpToPx(2), Color.parseColor("#00F0FF"))
        }
        bubble.background = bg
        bubble.elevation = dpToPx(8).toFloat()

        val iconText = TextView(this).apply {
            text = "⚡"
            textSize = 26f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#00F0FF"))
        }
        val textParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        bubble.addView(iconText, textParams)

        // Touch listener for dragging & click
        bubble.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View?, event: MotionEvent): Boolean {
                val params = bubbleLayoutParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                            isDragging = true
                            params.x = initialX + dx
                            params.y = initialY + dy
                            windowManager.updateViewLayout(floatingBubbleView, params)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDragging) {
                            // Clicked: Expand Soundboard
                            expandSoundboard()
                        }
                        return true
                    }
                }
                return false
            }
        })

        return bubble
    }

    private fun createExpandedSoundboardView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(16).toFloat()
                setColor(Color.parseColor("#10172A"))
                setStroke(dpToPx(2), Color.parseColor("#00F0FF"))
            }
            background = bg
            setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(12))
        }

        // --- TOP HEADER BAR ---
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(8))
        }

        val title = TextView(this).apply {
            text = "PICCHI SOUND ⚡"
            setTextColor(Color.parseColor("#00F0FF"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        header.addView(title)

        // STOP BUTTON (RED)
        val stopBtn = TextView(this).apply {
            text = "■ STOP"
            setTextColor(Color.WHITE)
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
            val stopBg = GradientDrawable().apply {
                cornerRadius = dpToPx(8).toFloat()
                setColor(Color.parseColor("#EF4444"))
            }
            background = stopBg
            setOnClickListener {
                soundPlayerManager.stopAllSounds()
            }
        }
        val stopParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, dpToPx(6), 0)
        }
        header.addView(stopBtn, stopParams)

        // MINIMIZE BUTTON (_)
        val minBtn = TextView(this).apply {
            text = " — "
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
            setOnClickListener {
                collapseSoundboard()
            }
        }
        header.addView(minBtn)

        // CLOSE BUTTON (X)
        val closeBtn = TextView(this).apply {
            text = " ✕ "
            setTextColor(Color.parseColor("#EF4444"))
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(6), dpToPx(4), dpToPx(6), dpToPx(4))
            setOnClickListener {
                stopSelf()
            }
        }
        header.addView(closeBtn)

        root.addView(header)

        // --- VOLUME BOOST ROW ---
        val volumeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(4), 0, dpToPx(6))
        }

        val volIcon = TextView(this).apply {
            text = "🔊"
            textSize = 14f
        }
        volumeRow.addView(volIcon)

        val volLabel = TextView(this).apply {
            text = "125% BOOST"
            setTextColor(Color.parseColor("#FF9E00"))
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(dpToPx(6), 0, dpToPx(6), 0)
        }

        val seekBar = SeekBar(this).apply {
            max = 100 // 50% to 150%
            progress = 75 // 125%
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    val boost = 0.5f + (progress / 100f) * 1.0f // 0.5f to 1.5f
                    soundPlayerManager.setVolumeBoost(boost)
                    volLabel.text = "${(boost * 100).toInt()}% BOOST"
                }
                override fun onStartTrackingTouch(sb: SeekBar?) {}
                override fun onStopTrackingTouch(sb: SeekBar?) {}
            })
        }
        volumeRow.addView(seekBar)
        volumeRow.addView(volLabel)
        root.addView(volumeRow)

        // --- PLAYING STATUS INDICATOR ---
        playingIndicatorText = TextView(this).apply {
            text = "Ready to Prank in Free Fire! 🎮"
            setTextColor(Color.parseColor("#10B981"))
            textSize = 11f
            setPadding(0, 0, 0, dpToPx(6))
        }
        root.addView(playingIndicatorText)

        // --- CATEGORY CHIPS SCROLL ROW ---
        val catScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, 0, 0, dpToPx(8))
        }
        val catRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val categories = listOf(
            SoundCategory.ALL,
            SoundCategory.PICCHI,
            SoundCategory.BANGLA_MEME,
            SoundCategory.FREE_FIRE,
            SoundCategory.GIRL_VOICE,
            SoundCategory.CYBER_MONSTER
        )

        for (cat in categories) {
            val chip = TextView(this).apply {
                text = "${cat.emoji} ${cat.displayNameEn}"
                textSize = 11f
                setPadding(dpToPx(10), dpToPx(5), dpToPx(10), dpToPx(5))
                setTextColor(if (cat == selectedCategory) Color.BLACK else Color.parseColor("#94A3B8"))
                val chipBg = GradientDrawable().apply {
                    cornerRadius = dpToPx(12).toFloat()
                    setColor(if (cat == selectedCategory) Color.parseColor("#00F0FF") else Color.parseColor("#1E293B"))
                }
                background = chipBg
                setOnClickListener {
                    selectedCategory = cat
                    // Refresh chip styles
                    for (i in 0 until catRow.childCount) {
                        val c = catRow.getChildAt(i) as? TextView
                        val isSel = (c?.text?.contains(cat.displayNameEn) == true)
                        c?.setTextColor(if (isSel) Color.BLACK else Color.parseColor("#94A3B8"))
                        (c?.background as? GradientDrawable)?.setColor(
                            if (isSel) Color.parseColor("#00F0FF") else Color.parseColor("#1E293B")
                        )
                    }
                    populateSoundGrid()
                }
            }
            val chipParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, dpToPx(6), 0)
            }
            catRow.addView(chip, chipParams)
        }
        catScroll.addView(catRow)
        root.addView(catScroll)

        // --- SCROLLABLE SOUND GRID ---
        val gridScroll = android.widget.ScrollView(this).apply {
            isVerticalScrollBarEnabled = true
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        soundGrid = GridLayout(this).apply {
            columnCount = 2
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }
        gridScroll.addView(soundGrid)
        root.addView(gridScroll)

        return root
    }

    private fun populateSoundGrid() {
        val grid = soundGrid ?: return
        grid.removeAllViews()

        val filtered = if (selectedCategory == SoundCategory.ALL) {
            cachedSounds
        } else {
            cachedSounds.filter { it.category == selectedCategory }
        }

        val itemWidth = (expandedLayoutParams?.width ?: dpToPx(340)) / 2 - dpToPx(18)

        for (sound in filtered) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
                val cardBg = GradientDrawable().apply {
                    cornerRadius = dpToPx(10).toFloat()
                    setColor(Color.parseColor("#1E293B"))
                    setStroke(dpToPx(1), Color.parseColor("#2E3D5C"))
                }
                background = cardBg

                setOnClickListener {
                    soundPlayerManager.playSound(sound)
                }
            }

            // Thumbnail / Icon
            if (sound.imageDrawableRes != null) {
                val iv = ImageView(this).apply {
                    setImageResource(sound.imageDrawableRes)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    layoutParams = LinearLayout.LayoutParams(dpToPx(48), dpToPx(48)).apply {
                        gravity = Gravity.CENTER_HORIZONTAL
                        setMargins(0, 0, 0, dpToPx(4))
                    }
                }
                card.addView(iv)
            } else {
                val emojiText = TextView(this).apply {
                    text = sound.iconEmoji
                    textSize = 28f
                    gravity = Gravity.CENTER
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(0, 0, 0, dpToPx(2))
                    }
                }
                card.addView(emojiText)
            }

            // Title
            val titleText = TextView(this).apply {
                text = sound.titleBn
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.parseColor("#F8FAFC"))
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            card.addView(titleText)

            // Subtitle
            val subText = TextView(this).apply {
                text = sound.titleEn
                textSize = 10f
                setTextColor(Color.parseColor("#94A3B8"))
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            card.addView(subText)

            val params = GridLayout.LayoutParams().apply {
                width = itemWidth
                height = GridLayout.LayoutParams.WRAP_CONTENT
                setMargins(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
            }
            grid.addView(card, params)
        }
    }

    private fun observeSoundData() {
        soundListJob = scope.launch {
            soundPlayerManager.soundList.collectLatest { list ->
                cachedSounds = list
                populateSoundGrid()
            }
        }

        playbackStateJob = scope.launch {
            soundPlayerManager.playbackState.collectLatest { state ->
                if (state.isPlaying && state.currentSoundId != null) {
                    val current = cachedSounds.find { it.id == state.currentSoundId }
                    playingIndicatorText?.text = "▶ Playing: ${current?.titleBn ?: "Sound..."} (Loud)"
                    playingIndicatorText?.setTextColor(Color.parseColor("#00F0FF"))
                } else {
                    playingIndicatorText?.text = "Ready to Prank in Free Fire! 🎮"
                    playingIndicatorText?.setTextColor(Color.parseColor("#10B981"))
                }
            }
        }
    }

    private fun expandSoundboard() {
        if (!isExpanded && expandedSoundboardView != null) {
            isExpanded = true
            floatingBubbleView?.visibility = View.GONE
            windowManager.addView(expandedSoundboardView, expandedLayoutParams)
            populateSoundGrid()
        }
    }

    private fun collapseSoundboard() {
        if (isExpanded && expandedSoundboardView != null) {
            isExpanded = false
            try {
                windowManager.removeView(expandedSoundboardView)
            } catch (e: Exception) {
                // Ignore if not attached
            }
            floatingBubbleView?.visibility = View.VISIBLE
        }
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundListJob?.cancel()
        playbackStateJob?.cancel()
        collapseSoundboard()
        if (floatingBubbleView != null) {
            try {
                windowManager.removeView(floatingBubbleView)
            } catch (e: Exception) {
                // Ignore
            }
            floatingBubbleView = null
        }
        expandedSoundboardView = null
    }

    companion object {
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, FloatingSoundboardService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingSoundboardService::class.java)
            context.stopService(intent)
        }
    }
}
