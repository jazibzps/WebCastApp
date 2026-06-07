package com.webcast.app

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

class VideoJsBridge(private val callback: (String) -> Unit) {

    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun onVideoFound(url: String) {
        if (url.isNotBlank()) {
            mainHandler.post { callback(url) }
        }
    }
}
