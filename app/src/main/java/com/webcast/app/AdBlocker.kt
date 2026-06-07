package com.webcast.app

import android.content.Context
import android.webkit.WebResourceResponse
import java.io.ByteArrayInputStream

class AdBlocker(context: Context) {

    private val blockedDomains = HashSet<String>(512)

    // URL substring patterns — if any of these appear anywhere in the URL, block it
    private val blockedPatterns = listOf(
        "doubleclick", "googlesyndication", "googleadservices",
        "adservice.google", "pagead2", "adnxs", "adroll", "adsrvr",
        "outbrain", "taboola", "criteo", "popads", "popcash",
        "propellerads", "exoclick", "trafficjunky", "trafficstars",
        "juicyads", "adsterra", "hilltopads", "plugrush", "adcash",
        "revcontent", "mgid.com", "clkmon", "clkrev", "trkmon",
        "/ads/", "/ad/", "/adserver/", "/adserve/", "/advert/",
        "/banner/", "/banners/", "/popunder/", "/popup/",
        "/preroll/", "/midroll/", "/interstitial/",
        "cdn.adnxs.com", "securepubads", "ad.doubleclick",
        "googletagmanager", "googletagservices", "google-analytics",
        "hotjar.com", "mixpanel.com", "scorecard", "quantserve"
    )

    init {
        context.assets.open("ad_hosts.txt").bufferedReader().use { reader ->
            reader.forEachLine { line ->
                val trimmed = line.trim().lowercase()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                    blockedDomains.add(trimmed)
                }
            }
        }
    }

    fun shouldBlock(url: String): Boolean {
        val lower = url.lowercase()

        // Fast pattern check first
        if (blockedPatterns.any { lower.contains(it) }) return true

        // Domain check
        return try {
            val host = java.net.URL(url).host?.lowercase()?.removePrefix("www.") ?: return false
            blockedDomains.contains(host) || blockedDomains.any { host.endsWith(".$it") }
        } catch (e: Exception) {
            false
        }
    }

    fun emptyResponse(): WebResourceResponse =
        WebResourceResponse("text/plain", "utf-8", ByteArrayInputStream(ByteArray(0)))
}
