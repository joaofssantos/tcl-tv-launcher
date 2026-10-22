package com.joaofsantos.tcllauncher

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService

/** Framework callbacks only: no timer or polling is used for media state. */
class MediaSessionListenerService : NotificationListenerService() {
    private lateinit var sessionManager: MediaSessionManager
    private var activeController: MediaController? = null
    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publishActiveSession()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publishActiveSession()
        override fun onSessionDestroyed() = publishActiveSession()
    }
    private val sessionsChanged = MediaSessionManager.OnActiveSessionsChangedListener { publish(it.orEmpty()) }

    override fun onListenerConnected() {
        super.onListenerConnected()
        sessionManager = getSystemService(MediaSessionManager::class.java)
        val component = ComponentName(this, MediaSessionListenerService::class.java)
        sessionManager.addOnActiveSessionsChangedListener(sessionsChanged, component)
        publishActiveSession()
    }

    override fun onDestroy() {
        if (::sessionManager.isInitialized) sessionManager.removeOnActiveSessionsChangedListener(sessionsChanged)
        activeController?.unregisterCallback(controllerCallback)
        super.onDestroy()
    }

    private fun publishActiveSession() {
        if (::sessionManager.isInitialized) publish(sessionManager.getActiveSessions(ComponentName(this, MediaSessionListenerService::class.java)))
    }

    private fun publish(controllers: List<MediaController>) {
        val controller = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: controllers.firstOrNull()
        if (controller !== activeController) {
            activeController?.unregisterCallback(controllerCallback)
            activeController = controller
            controller?.registerCallback(controllerCallback)
        }
        if (controller == null) { NowPlayingRepository.publish(null); return }
        val metadata = controller.metadata
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.trim().orEmpty()
        if (title.isBlank()) { NowPlayingRepository.publish(null); return }
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST) ?: controller.packageName
        NowPlayingRepository.publish(NowPlayingState(
            title = title,
            artist = artist,
            artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART) ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART),
            packageName = controller.packageName,
            isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING,
            controller = controller,
        ))
    }
}
