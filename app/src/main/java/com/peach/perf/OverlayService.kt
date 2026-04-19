package com.peach.perf

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.setPadding
import com.peach.perf.data.MonitorHolder
import kotlinx.coroutines.*

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private lateinit var overlayView: LinearLayout
    private lateinit var cpuText: TextView
    private lateinit var memText: TextView
    private lateinit var tempText: TextView
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createOverlayView()
        setupDragListener()
        
        scope.launch {
            MonitorHolder.stats.collect { stats ->
                cpuText.text = "CPU: ${stats?.cpuUsage?.toInt() ?: 0}%"
                memText.text = "MEM: ${stats?.memoryUsage?.toInt() ?: 0}%"
                tempText.text = "TMP: ${stats?.temperature?.toInt() ?: 0}°C"
            }
        }
    }

    private fun createOverlayView() {
        overlayView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(0xDD1A1A2E.toInt())
            setPadding(16)
            alpha = 0.9f
        }
        
        cpuText = TextView(this).apply {
            text = "CPU: --%"
            textSize = 16f
            setTextColor(0xFF00E5FF.toInt())
        }
        memText = TextView(this).apply {
            text = "MEM: --%"
            textSize = 16f
            setTextColor(0xFF00E676.toInt())
        }
        tempText = TextView(this).apply {
            text = "TMP: --°C"
            textSize = 16f
            setTextColor(0xFFFF6D00.toInt())
        }
        
        overlayView.addView(cpuText)
        overlayView.addView(memText)
        overlayView.addView(tempText)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }
        wm.addView(overlayView, params)
    }

    private fun setupDragListener() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        overlayView.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    initialX = (overlayView.layoutParams as WindowManager.LayoutParams).x
                    initialY = (overlayView.layoutParams as WindowManager.LayoutParams).y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                android.view.MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    if (kotlin.math.abs(deltaX) > 10 || kotlin.math.abs(deltaY) > 10) isDragging = true
                    if (isDragging) {
                        (overlayView.layoutParams as WindowManager.LayoutParams).x = initialX + deltaX.toInt()
                        (overlayView.layoutParams as WindowManager.LayoutParams).y = initialY + deltaY.toInt()
                        wm.updateViewLayout(overlayView, overlayView.layoutParams)
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        try { wm.removeView(overlayView) } catch (e: Exception) {}
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
