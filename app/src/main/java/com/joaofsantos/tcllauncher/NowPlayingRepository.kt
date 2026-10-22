package com.joaofsantos.tcllauncher

import android.graphics.Bitmap
import android.media.session.MediaController
import android.os.Handler
import android.os.Looper

data class NowPlayingState(val title: String, val artist: String, val artwork: Bitmap?, val packageName: String, val isPlaying: Boolean, val controller: MediaController)

/** In-process fan-out for NotificationListenerService callbacks. */
object NowPlayingRepository {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = linkedSetOf<(NowPlayingState?) -> Unit>()
    private var state: NowPlayingState? = null
    fun addListener(listener: (NowPlayingState?) -> Unit) { listeners += listener; listener(state) }
    fun removeListener(listener: (NowPlayingState?) -> Unit) { listeners -= listener }
    fun publish(newState: NowPlayingState?) = mainHandler.post {
        state = newState
        listeners.toList().forEach { it(newState) }
    }
}
