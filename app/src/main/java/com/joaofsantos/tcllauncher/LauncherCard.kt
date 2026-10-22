package com.joaofsantos.tcllauncher

import android.content.Context
import android.graphics.Color
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.VectorDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

class LauncherCard(context: Context, title: String, subtitle: String = "") : LinearLayout(context) {
    private val normal = GradientDrawable().apply { cornerRadius = 18f; setColor(Color.rgb(35, 43, 55)) }
    private val density = resources.displayMetrics.density
    private val focused = GradientDrawable().apply { cornerRadius = 18f; setColor(Color.rgb(53, 64, 80)); setStroke((4 * density).toInt(), Color.WHITE) }
    private val titleView = TextView(context).apply {
        text = title; textSize = 19f; setTextColor(Color.WHITE); gravity = Gravity.CENTER
        maxLines = 1; ellipsize = TextUtils.TruncateAt.END
    }
    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_VERTICAL
        isFocusable = true
        isClickable = true
        background = normal
        setPadding((16 * density).toInt(), (7 * density).toInt(), (16 * density).toInt(), (7 * density).toInt())
        addView(titleView, LayoutParams(-1, -2))
        if (subtitle.isNotEmpty()) addView(TextView(context).apply { text = subtitle; textSize = 13f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER; maxLines = 1; ellipsize = TextUtils.TruncateAt.END }, LayoutParams(-1, -2))
        onFocusChangeListener = OnFocusChangeListener { view, hasFocus ->
            view.background = if (hasFocus) focused else normal
            view.animate().scaleX(if (hasFocus) 1.06f else 1f).scaleY(if (hasFocus) 1.06f else 1f).setInterpolator(DecelerateInterpolator()).setDuration(150).start()
        }
    }
    fun setArtwork(drawable: Drawable?, large: Boolean = false, hideLabel: Boolean = false) {
        if (drawable == null) return
        if (hideLabel) titleView.visibility = View.GONE
        val height = ((if (large) 120 else 40) * density).toInt()
        val width = ((if (large) 240 else 96) * density).toInt()
        if (!large) titleView.textSize = 13f
        val image = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setImageDrawable(if (drawable is VectorDrawable) drawable else {
                // Rasterize at tile size while keeping the source aspect ratio (banners are 16:9).
                val srcW = drawable.intrinsicWidth.takeIf { it > 0 } ?: width
                val srcH = drawable.intrinsicHeight.takeIf { it > 0 } ?: height
                val scale = minOf(width.toFloat() / srcW, height.toFloat() / srcH)
                val w = (srcW * scale).toInt().coerceAtLeast(1)
                val h = (srcH * scale).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                Canvas(bitmap).also { canvas -> drawable.setBounds(0, 0, w, h); drawable.draw(canvas) }
                BitmapDrawable(resources, bitmap)
            })
        }
        addView(image, 0, LayoutParams(-1, height).apply { bottomMargin = (4 * density).toInt() })
    }
    fun setBrandColor(color: Int) { normal.setColor(color); focused.setColor(color) }
    fun setBrandGradient(start: Int, end: Int) { normal.setColors(intArrayOf(start, end)); focused.setColors(intArrayOf(start, end)) }
}
