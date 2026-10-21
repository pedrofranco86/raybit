package com.pelops.assistant

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import kotlin.math.abs

class FloatingBubbleService : Service() {
    private lateinit var windowManager: WindowManager
    private var bubble: TextView? = null
    private lateinit var params: WindowManager.LayoutParams

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        params = WindowManager.LayoutParams(58, 58, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.END; x = 24; y = 180 }
        val view = TextView(this).apply {
            text = "P"; textSize = 24f; typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(16, 24, 32)); gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.rgb(185, 242, 255)); setStroke(2, Color.WHITE) }
            setOnTouchListener(object : android.view.View.OnTouchListener {
                private var downX = 0f; private var downY = 0f; private var startX = 0; private var startY = 0; private var moved = false
                override fun onTouch(v: android.view.View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> { downX = event.rawX; downY = event.rawY; startX = params.x; startY = params.y; moved = false; return true }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = (event.rawX - downX).toInt(); val dy = (event.rawY - downY).toInt()
                            if (abs(dx) > 8 || abs(dy) > 8) moved = true
                            params.x = startX - dx; params.y = startY + dy
                            windowManager.updateViewLayout(v, params); return true
                        }
                        MotionEvent.ACTION_UP -> {
                            if (!moved) startActivity(Intent(this@FloatingBubbleService, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP))
                            return true
                        }
                    }
                    return false
                }
            })
        }
        bubble = view
        windowManager.addView(view, params)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onDestroy() { bubble?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }; bubble = null; super.onDestroy() }
    override fun onBind(intent: Intent?): IBinder? = null
}
