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

    private val BLOCKED_KEYWORDS = listOf(        "/googleads/",
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

    /**
     * Betting/casino brands that abuse video embeds with redirects and
     * pop-unders. Matching is done against the host so numeric clones such as
     * "1xbet-es.com" or "melbet-123.com" are covered by the brand token.
     */
    private val GAMBLING_HOST_TOKENS = listOf(
        "sapphirebet", "1xbet", "melbet", "betandyou", "megapari", "linebet",
        "22bet", "888starz", "helabet", "mostbet", "ivibet", "1win",
        "vulkanvegas", "vulkan.bet", "betwinner", "betano", "bet365", "bwin",
        "betway", "pin-up", "pinup", "parimatch", "leonbet", "marathonbet",
        "fonbet", "betfair", "williamhill", "draftkings", "fanduel", "betmgm",
        "caesars", "pokerstars", "betsson", "unibet", "nordicbet", "betclic",
        "sportingbet", "bet9ja", "1xbit", "joycasino", "betmaster", "betobet",
        "betpawa", "pixbet", "vaidebet", "betesporte", "betnacional", "blaze.bet",
        "leovegas", "casumo", "jackpotcity", "royalvegas", "betcris",
        "betplay", "codere", "caliente.mx", "playdoit", "strendus", "ganabet",
        "aliexpress", "temu.com", "shein.com", "dhgate", "banggood", "gearbest"
    )

    private val GAMBLING_HOST_SUFFIXES = setOf(
        "bc.game", "stake.com", "gg.bet", "blaze.com", "blaze.bet"
    )

    fun isAdUrl(url: String, blockGamblingAds: Boolean = true): Boolean {
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
        if (blockGamblingAds) {
            for (suffix in GAMBLING_HOST_SUFFIXES) {
                if (host == suffix || host.endsWith(".$suffix")) return true
            }
            for (token in GAMBLING_HOST_TOKENS) {
                if (host.contains(token)) return true
            }
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

            function isVisible(el) {
                try {
                    var r = el.getBoundingClientRect();
                    var s = window.getComputedStyle(el);
                    return r.width > 0 && r.height > 0 &&
                        s.display !== 'none' && s.visibility !== 'hidden' && s.opacity !== '0';
                } catch(e) { return false; }
            }

            function cleanVideoAds() {
                try {
                    var skipButtons = document.querySelectorAll(
                        '.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button, ' +
                        '.videoAdUiSkipButton, [class*="skip-button"], [class*="skipButton"], ' +
                        '[class*="btn-skip"], [id*="skip"], [aria-label*="skip" i], ' +
                        '[aria-label*="saltar" i]'
                    );
                    for (var i = 0; i < skipButtons.length; i++) {
                        if (isVisible(skipButtons[i])) skipButtons[i].click();
                    }

                    // Providers often render "Saltar anuncio" as a plain div/span.
                    var candidates = document.querySelectorAll('button, a, div, span, [role="button"]');
                    for (var c = 0; c < candidates.length; c++) {
                        var el = candidates[c];
                        var text = (el.textContent || '').trim().toLowerCase();
                        if (text.length > 40) continue;
                        if ((text.indexOf('saltar anuncio') >= 0 ||
                             text.indexOf('omitir anuncio') >= 0 ||
                             text.indexOf('skip ad') >= 0) && isVisible(el)) {
                            el.click();
                        }
                    }

                    var adVideos = document.querySelectorAll(
                        '#vast-video, video[id*="vast"], video[class*="ad"], .ad-showing video'
                    );
                    for (var v = 0; v < adVideos.length; v++) {
                        var video = adVideos[v];
                        if (video && video.duration > 0) {
                            if (!video.paused) video.currentTime = video.duration;
                            video.style.display = 'none';
                        }
                    }
                    var vastOverlay = document.getElementById('vast-overlay');
                    if (vastOverlay) vastOverlay.style.display = 'none';

                    var isAdShowing = document.querySelector(
                        '.ad-showing, .ytp-ad-player-overlay, .video-ads .ad-container'
                    );
                    if (isAdShowing) {
                        var videos = document.querySelectorAll('video');
                        for (var a = 0; a < videos.length; a++) {
                            var mainVideo = videos[a];
                            if (mainVideo && !mainVideo.paused && mainVideo.duration > 0) {
                                mainVideo.currentTime = mainVideo.duration;
                            }
                        }
                    }

                    var overlayCloses = document.querySelectorAll(
                        '.ytp-ad-overlay-close-button, [class*="ad-close"], ' +
                        '[class*="ad_overlay"] [class*="close"], ' +
                        '[id*="ad-overlay"] [class*="close"]'
                    );
                    for (var d = 0; d < overlayCloses.length; d++) {
                        overlayCloses[d].click();
                    }
                } catch(e) {}
            }

            cleanVideoAds();
            if (!window.__bloodersAdBlockInterval) {
                window.__bloodersAdBlockInterval = setInterval(cleanVideoAds, 600);
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

    val AUTOPLAY_SCRIPT: String = """
        (function() {
            function startVideo() {
                try {
                    var videos = document.querySelectorAll('video');
                    var hasVideo = videos.length > 0;
                    for (var i = 0; i < videos.length; i++) {
                        var v = videos[i];
                        v.muted = false;
                        var p = v.play();
                        if (p && p.catch) p.catch(function() {});
                    }
                    var selectors = [
                        '.vjs-big-play-button',
                        '.jw-display-icon-container',
                        '.plyr__control[data-plyr="play"]',
                        'button[aria-label*="play" i]',
                        'button[aria-label*="reproducir" i]',
                        '[role="button"][aria-label*="play" i]',
                        '[title*="play" i]',
                        '[class*="play-button"]',
                        '[class*="playButton"]'
                    ];
                    var first = document.querySelector('video');
                    if (!first || first.paused) {
                        for (var s = 0; s < selectors.length; s++) {
                            var button = document.querySelector(selectors[s]);
                            if (button) { button.click(); break; }
                        }
                    }
                    return hasVideo;
                } catch (e) { return false; }
            }
            [350, 1000, 2200, 4500].forEach(function(ms) {
                setTimeout(startVideo, ms);
            });
        })();
    """.trimIndent()

    val TOGGLE_PLAY_SCRIPT: String = """
        (function() {
            try {
                var videos = document.querySelectorAll('video');
                if (videos.length > 0) {
                    var v = videos[0];
                    if (v.paused) {
                        var p = v.play();
                        if (p && p.catch) p.catch(function() {});
                    } else {
                        v.pause();
                    }
                    return;
                }
                var button = document.querySelector(
                    '.vjs-big-play-button, .jw-display-icon-container, .plyr__control[data-plyr="play"], [aria-label*="play" i]'
                );
                if (button) button.click();
            } catch (e) {}
        })();
    """.trimIndent()

    fun seekScript(deltaSeconds: Int): String = """
        (function() {
            try {
                var videos = document.querySelectorAll('video');
                for (var i = 0; i < videos.length; i++) {
                    var v = videos[i];
                    var max = isFinite(v.duration) ? v.duration : v.currentTime + $deltaSeconds;
                    v.currentTime = Math.max(0, Math.min(max, v.currentTime + $deltaSeconds));
                }
            } catch (e) {}
        })();
    """.trimIndent()
}
