package com.webcast.app

import android.content.Context
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener

class CastManager(context: Context) {

    val castContext: CastContext = CastContext.getSharedInstance(context)
    private var castSession: CastSession? = null

    init {
        castContext.sessionManager.addSessionManagerListener(
            object : SessionManagerListener<CastSession> {
                override fun onSessionStarted(s: CastSession, id: String)          { castSession = s }
                override fun onSessionResumed(s: CastSession, wasSuspended: Boolean) { castSession = s }
                override fun onSessionEnded(s: CastSession, error: Int)             { castSession = null }
                override fun onSessionStarting(s: CastSession)                     {}
                override fun onSessionStartFailed(s: CastSession, error: Int)       {}
                override fun onSessionEnding(s: CastSession)                       {}
                override fun onSessionResuming(s: CastSession, id: String)         {}
                override fun onSessionResumeFailed(s: CastSession, error: Int)     {}
                override fun onSessionSuspended(s: CastSession, reason: Int)       {}
            },
            CastSession::class.java
        )
    }

    fun castVideo(url: String, title: String = "Stream") {
        val mimeType = when {
            url.contains(".m3u8", ignoreCase = true) -> "application/x-mpegURL"
            url.contains(".mpd",  ignoreCase = true) -> "application/dash+xml"
            url.contains(".webm", ignoreCase = true) -> "video/webm"
            else                                     -> "video/mp4"
        }

        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, title)
        }

        val mediaInfo = MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(mimeType)
            .setMetadata(metadata)
            .build()

        val session = castSession ?: castContext.sessionManager.currentCastSession
        session?.remoteMediaClient?.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setAutoplay(true)
                .build()
        )
    }

    fun togglePlayback() {
        val client = remoteClient ?: return
        if (client.isPlaying) client.pause() else client.play()
    }

    fun stopCasting() {
        castContext.sessionManager.endCurrentSession(true)
    }

    val isConnected: Boolean
        get() = castContext.sessionManager.currentCastSession?.isConnected == true

    private val remoteClient
        get() = (castSession ?: castContext.sessionManager.currentCastSession)?.remoteMediaClient
}
