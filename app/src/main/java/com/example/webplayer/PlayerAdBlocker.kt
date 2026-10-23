package com.example.webplayer

import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Locale

/**
 * Ad/tracker shield ported from Serchwebview and focused on embed players.
 *
 * NSRPlay's providers monetise their pages with pop-unders and redirects. The
 * WebView alone cannot stop those, so every request is filtered before it is
 * loaded and a small cosmetic/skip script runs on every page.
 */
object PlayerAdBlocker {

    private val BLOCKED_HOST_SUFFIXES = hashSetOf(
        "doubleclick.net",
        "googleadservices.com",
        "googlesyndication.com",
        "adservice.google.com",
        "adcolony.com",
        "applovin.com",
        "unityads.unity3d.com",
        "vungle.com",
        "taboola.com",
        "outbrain.com",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "propellerpops.com",
        "scorecardresearch.com",
        "quantserve.com",
        "adroll.com",
        "criteo.com",
        "rubiconproject.com",
        "pubmatic.com",
        "openx.net",
        "advertising.com",
        "adnxs.com",
        "admob.com",
        "exponential.com",
        "chartbeat.com",
        "clicksor.com",
        "infolinks.com",
        "buysellads.com",
        "revcontent.com",
        "zergnet.com",
        "mgid.com",
        "yieldmo.com",
        "smartadserver.com",
        "teads.tv",
        "casale-media.com",
        "bidswitch.net",
        "spotxchange.com",
        "lijit.com",
        "sovrn.com",
        "adblade.com",
        "content.ad",
        "disqusads.com",
        "amazon-adsystem.com",
        "flashtalking.com",
        "adsafeprotected.com",
        "moatads.com",
        "serving-sys.com",
        "adform.net",
        "fastclick.net",
        "trafficjunky.com",
        "exoclick.com",
        "juicyads.com",
        "adxcore.ch",
        "adtrue.com",
        "adsterra.com",
        "hilltopads.net",
        "clickadu.com",
        "adcash.com",
        "popmyads.com",
        "onclickads.net",
        "reviversoft.com",
        "adf.ly",
        "shorte.st",
        "linkvertise.com"
    )

    private val BLOCKED_KEYWORDS = listOf(
        "/googleads/",
        "/pagead/",
        "/ad_status.js",
        "/ads/api/",
        "api/stats/ads",
        "/doubleclick/",
        "/adserver/",
        "/ads.js",
        "/ad-banner",
        "/banner_ad",
        "/popunder",
        "video-ads",
        "/pre-roll-ad",
        "/pop.js",
        "onclickads",
        "adsterra",
        "hilltopads",
        "clickadu"
    )

    fun isAdUrl(url: String): Boolean {
        if (url.isBlank()) return false
        val uri = try {
            Uri.parse(url)
        } catch (_: Exception) {
            return false
        }

        val host = uri.host?.lowercase(Locale.ROOT) ?: return false
        for (suffix in BLOCKED_HOST_SUFFIXES) {
            if (host == suffix || host.endsWith(".$suffix")) return true
        }

        val lowerUrl = url.lowercase(Locale.ROOT)
        for (keyword in BLOCKED_KEYWORDS) {
            if (lowerUrl.contains(keyword)) return true
        }
        return false
    }

    fun createEmptyResponse(): InputStream = ByteArrayInputStream(ByteArray(0))

    val AD_HIDING_CSS: String = """
        [id*="ad-"], [id*="ad_"], [id*="ads-"], [id*="ads_"],
        [class*="ad-"], [class*="ad_"], [class*="ads-"], [class*="ads_"],
        .ytp-ad-module, .ytp-ad-overlay-container, .ytp-ad-player-overlay,
        .video-ads, .ad-container, .ad-wrapper, .ad-unit, .adsbygoogle,
        iframe[src*="doubleclick"], iframe[src*="googleadservices"], iframe[src*="ads"],
        .taboola, .outbrain, .sponsor-box, .promoted-content,
        #cookie-notice, #cookie-banner, .cookie-consent-banner,
        .videoAdUi, .videoAdUiSkipButton {
            display: none !important;
            visibility: hidden !important;
            height: 0 !important;
            width: 0 !important;
            opacity: 0 !important;
            pointer-events: none !important;
        }
        video { max-width: 100% !important; }
    """.trimIndent().replace("\n", " ")

    val PAGE_SCRIPT: String = """
        (function() {
            try {
                if (!window.__bloodersPopupGuard) {
                    window.__bloodersPopupGuard = true;
                    window.open = function() { return null; };
                    window.alert = function() {};
                    document.addEventListener('click', function(e) {
                        var a = e.target && e.target.closest ? e.target.closest('a') : null;
                        if (a && a.target === '_blank') { a.target = '_self'; }
                    }, true);
                }
            } catch(e) {}

            try {
                if (!document.getElementById('blooders-adblock-style')) {
                    var style = document.createElement('style');
                    style.id = 'blooders-adblock-style';
                    style.textContent = '$AD_HIDING_CSS';
                    (document.head || document.documentElement).appendChild(style);
                }
            } catch(e) {}

            function cleanVideoAds() {
                try {
                    var skipButtons = document.querySelectorAll(
                        '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, .videoAdUiSkipButton, [class*="skip-button"], button[id*="skip"]'
                    );
                    for (var i = 0; i < skipButtons.length; i++) {
                        skipButtons[i].click();
                    }

                    var isAdShowing = document.querySelector('.ad-showing, .ytp-ad-player-overlay, .video-ads .ad-container');
                    if (isAdShowing) {
                        var videos = document.querySelectorAll('video');
                        for (var v = 0; v < videos.length; v++) {
                            var video = videos[v];
                            if (video && !video.paused && video.duration > 0) {
                                video.currentTime = video.duration;
                            }
                        }
                    }

                    var overlayCloses = document.querySelectorAll('.ytp-ad-overlay-close-button, [class*="ad-close"]');
                    for (var c = 0; c < overlayCloses.length; c++) {
                        overlayCloses[c].click();
                    }
                } catch(e) {}
            }

            cleanVideoAds();
            if (!window.__bloodersAdBlockInterval) {
                window.__bloodersAdBlockInterval = setInterval(cleanVideoAds, 700);
            }
        })();
    """.trimIndent()

    fun playbackSpeedScript(speed: Float): String = """
        (function() {
            var videos = document.querySelectorAll('video');
            for (var i = 0; i < videos.length; i++) {
                videos[i].playbackRate = $speed;
            }
        })();
    """.trimIndent()
}
