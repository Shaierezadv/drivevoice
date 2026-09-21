package com.drivevoice.assistant.overlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

/**
 * Optional full-screen cover so WhatsApp is not visible while auto-sending.
 * Requires SYSTEM_ALERT_WINDOW (draw over other apps).
 */
object SendCoverOverlay {

    private var view: TextView? = null
    private val main = Handler(Looper.getMainLooper())

    fun canDraw(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context)

    fun show(context: Context, message: String) {
        if (!canDraw(context)) return
        main.post {
            hideLocked(context)
            val app = context.applicationContext
            val tv = TextView(app).apply {
                text = message
                textSize = 22f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#F20A1929"))
                gravity = Gravity.CENTER
                setPadding(48, 48, 48, 48)
            }
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            try {
                (app.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                    .addView(tv, params)
                view = tv
                main.postDelayed({ hide(app) }, 12_000)
            } catch (_: Exception) {
                view = null
            }
        }
    }

    fun hide(context: Context) {
        main.post { hideLocked(context) }
    }

    private fun hideLocked(context: Context) {
        val v = view ?: return
        try {
            (context.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
                .removeView(v)
        } catch (_: Exception) {
        }
        view = null
    }
}
