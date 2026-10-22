package com.joaofsantos.tcllauncher

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/** MediaSession integration hook. Hidden until the system integration has data. */
class NowPlayingCard(context: Context) : LinearLayout(context) {
    private val cover = ImageView(context).apply { setImageDrawable(ColorDrawable(Color.DKGRAY)); scaleType = ImageView.ScaleType.CENTER_CROP }
    private val title = TextView(context).apply { setTextColor(Color.WHITE); textSize = 18f; isSingleLine = true }
    private val artist = TextView(context).apply { setTextColor(Color.LTGRAY); textSize = 14f; isSingleLine = true }
    private val playPause = TextView(context).apply { setTextColor(Color.WHITE); textSize = 23f; gravity = Gravity.CENTER }
    private val next = TextView(context).apply { text = "⏭"; setTextColor(Color.WHITE); textSize = 22f; gravity = Gravity.CENTER }
    private var current: NowPlayingState? = null
    private val observer: (NowPlayingState?) -> Unit = ::render
    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(20, 8, 20, 8)
        setBackgroundColor(Color.rgb(35, 41, 52))
        addView(cover, LayoutParams(68, 68))
        addView(LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 0, 16, 0)
            addView(title, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            addView(artist, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        }, LayoutParams(0, LayoutParams.MATCH_PARENT, 1f))
        addView(playPause, LayoutParams(58, LayoutParams.MATCH_PARENT).apply { playPause.setOnClickListener { toggle() } })
        addView(next, LayoutParams(58, LayoutParams.MATCH_PARENT).apply { next.setOnClickListener { current?.controller?.transportControls?.skipToNext() } })
        setOnClickListener { current?.controller?.sessionActivity?.send() }
        visibility = View.GONE
    }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); NowPlayingRepository.addListener(observer) }
    override fun onDetachedFromWindow() { NowPlayingRepository.removeListener(observer); super.onDetachedFromWindow() }
    private fun render(state: NowPlayingState?) {
        current = state
        visibility = if (state == null) View.GONE else View.VISIBLE
        requestLayout()
        parent?.requestLayout()
        state ?: return
        title.text = state.title
        artist.text = state.artist
        if (state.artwork != null) cover.setImageBitmap(state.artwork) else cover.setImageDrawable(runCatching { context.packageManager.getApplicationIcon(state.packageName) }.getOrNull())
        playPause.text = if (state.isPlaying) "❚❚" else "▶"
    }
    private fun toggle() { current?.let { if (it.isPlaying) it.controller.transportControls.pause() else it.controller.transportControls.play() } }
}
