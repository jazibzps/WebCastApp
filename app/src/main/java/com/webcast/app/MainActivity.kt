package com.webcast.app

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.ProgressBar
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var castFab: FloatingActionButton
    private lateinit var castManager: CastManager

    private var latestVideoUrl: String? = null
    private lateinit var detectorJs: String
    private lateinit var adBlockJs: String
    private lateinit var adBlocker: AdBlocker

    private val fabColorIdle    = Color.parseColor("#757575")  // grey  — no video yet
    private val fabColorReady   = Color.parseColor("#4CAF50")  // green — video found, tap to cast

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        webView     = findViewById(R.id.webView)
        urlBar      = findViewById(R.id.urlBar)
        progressBar = findViewById(R.id.progressBar)
        castFab     = findViewById(R.id.castFab)

        detectorJs  = assets.open("video_detector.js").bufferedReader().use { it.readText() }
        adBlockJs   = assets.open("ad_block.js").bufferedReader().use { it.readText() }
        adBlocker   = AdBlocker(this)
        castManager = CastManager(this)

        setupWebView()
        setupUrlBar()
        setupCastFab()
        setupBackHandler()

        webView.loadUrl("https://www.google.com")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        with(webView.settings) {
            javaScriptEnabled        = true
            domStorageEnabled        = true
            useWideViewPort          = true
            loadWithOverviewMode     = true
            mediaPlaybackRequiresUserGesture = false
            userAgentString          = userAgentString.replace("wv", "")  // look like full Chrome
        }

        webView.addJavascriptInterface(VideoJsBridge { url -> onVideoDetected(url) }, "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                urlBar.setText(url)
                injectDetector()
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest) = false

            // Network-level ad blocking + video sniffing (runs on background thread)
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString()

                // Block ad/tracker requests before they load
                if (adBlocker.shouldBlock(url)) return adBlocker.emptyResponse()

                // Detect video URLs (including inside iframes)
                if (url.contains(".m3u8", ignoreCase = true) ||
                    url.contains(".mpd",  ignoreCase = true) ||
                    (url.contains(".mp4", ignoreCase = true) && !url.contains("thumb"))
                ) {
                    runOnUiThread { onVideoDetected(url) }
                }
                return null
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress   = newProgress
                progressBar.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
                if (newProgress == 100) injectDetector()
            }
        }
    }

    private fun injectDetector() {
        webView.evaluateJavascript(adBlockJs, null)
        webView.evaluateJavascript(detectorJs, null)
    }

    private fun onVideoDetected(url: String) {
        if (url == latestVideoUrl) return
        latestVideoUrl = url
        castFab.backgroundTintList = ColorStateList.valueOf(fabColorReady)
    }

    private fun setupUrlBar() {
        urlBar.setOnEditorActionListener { _, actionId, event ->
            val isGo = actionId == EditorInfo.IME_ACTION_GO
            val isEnter = event?.keyCode == KeyEvent.KEYCODE_ENTER &&
                          event.action == KeyEvent.ACTION_DOWN
            if (isGo || isEnter) {
                navigateTo(urlBar.text.toString().trim())
                true
            } else false
        }
    }

    private fun navigateTo(input: String) {
        if (input.isBlank()) return
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(".")                                         -> "https://$input"
            else -> "https://www.google.com/search?q=${java.net.URLEncoder.encode(input, "UTF-8")}"
        }
        latestVideoUrl = null
        castFab.backgroundTintList = ColorStateList.valueOf(fabColorIdle)
        webView.loadUrl(url)
    }

    private fun setupCastFab() {
        castFab.backgroundTintList = ColorStateList.valueOf(fabColorIdle)

        castFab.setOnClickListener {
            val url = latestVideoUrl
            if (url != null) {
                castManager.castVideo(url)
            }
        }

        castFab.setOnLongClickListener {
            if (castManager.isConnected) castManager.togglePlayback()
            true
        }
    }

    private fun setupBackHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) webView.goBack()
                else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        CastButtonFactory.setUpMediaRouteButton(applicationContext, menu, R.id.media_route_menu_item)
        return true
    }
}
