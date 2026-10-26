package com.example.webplayer

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Rational
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DesktopWindows
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.example.extensions.ExtensionEngine
import com.example.data.webepisodes.WebEpisodeWatchStore
import com.example.extensions.ExtensionNetworkFilter
import com.example.extensions.WebExtension
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

/**
 * The unified "Serchwebview" playback engine.
 *
 * NSRPlay resolves a title to one or more third-party embed pages. Those pages
 * are best played by a real WebView with an ad shield, fullscreen HTML5 video
 * and speed controls, which is exactly what Serchwebview did. Instead of
 * shipping a second app, this activity embeds that engine in BloodersTv.
 */
class PlayerWebActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_REFERER = "extra_referer"
        const val EXTRA_USE_IFRAME = "extra_use_iframe"
        const val EXTRA_EPISODE_URLS = "extra_episode_urls"
        const val EXTRA_EPISODE_TITLES = "extra_episode_titles"
        const val EXTRA_EPISODE_INDEX = "extra_episode_index"
        const val EXTRA_EPISODE_WATCH_KEYS = "extra_episode_watch_keys"

        val AUTO_NEXT_SCRIPT = """
            (function() {
                if (window.__bloodersAutoNextInstalled) return;
                window.__bloodersAutoNextInstalled = true;

                function bind(video) {
                    if (!video || video.__bloodersNextBound) return;
                    // Never advance on an ad video ending.
                    try {
                        if (video.closest(
                            '#vast-overlay, [id*="vast"], [class*="ad-overlay"], ' +
                            '[class*="video-ads"], .video-ads, .ad-showing'
                        )) return;
                        if (isFinite(video.duration) && video.duration > 0 &&
                            video.duration < 120) return;
                    } catch(e) {}
                    video.__bloodersNextBound = true;
                    video.addEventListener('ended', function() {
                        try {
                            if (window.BloodersBridge &&
                                window.BloodersBridge.onVideoEnded) {
                                window.BloodersBridge.onVideoEnded();
                            }
                        } catch(e) {}
                    }, true);
                }

                function scan() {
                    try {
                        document.querySelectorAll('video').forEach(bind);
                    } catch(e) {}
                }

                scan();
                try {
                    new MutationObserver(scan).observe(
                        document.documentElement,
                        { childList: true, subtree: true }
                    );
                } catch(e) {}
                setInterval(scan, 2000);
            })();
        """.trimIndent()

        private const val DEFAULT_REFERER = "https://nsrplay.space/"
        private fun desktopUserAgent(major: Int): String =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$major.0.0.0 Safari/537.36"
        private fun mobileUserAgent(major: Int): String =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$major.0.0.0 Mobile Safari/537.36"
        private val SPEEDS = floatArrayOf(1f, 1.25f, 1.5f, 2f)
    }

    private lateinit var webView: WebView
    private lateinit var extensionNetworkFilter: ExtensionNetworkFilter
    private var extensions: List<WebExtension> = emptyList()
    private var startUrl: String = ""
    private var referer: String = DEFAULT_REFERER
    private var useIframe: Boolean = false
    private var blockGamblingAds: Boolean = true
    private var legacyWebView: Boolean = false
    private var episodeUrls: List<String> = emptyList()
    private var episodeTitles: List<String> = emptyList()
    private var episodeWatchKeys: List<String> = emptyList()
    private var episodeIndex: Int = -1
    private var lastAutoNextAt: Long = 0L
    private var chromeMajorVersion: Int = 128
    private var extensionScriptHandler: ScriptHandler? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private var controlsVisible by mutableStateOf(true)
    private var isPip by mutableStateOf(false)
    private var isLoading by mutableStateOf(true)
    private var loadProgress by mutableIntStateOf(0)
    private var errorMessage by mutableStateOf<String?>(null)
    private var desktopMode by mutableStateOf(false)
    private var playbackSpeed by mutableFloatStateOf(1f)
    private var pageTitle by mutableStateOf("")
    private var isFullscreenVideo by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        startUrl = intent.getStringExtra(EXTRA_URL).orEmpty()
        episodeUrls = intent.getStringArrayListExtra(EXTRA_EPISODE_URLS)
            ?.filter { it.isNotBlank() }
            .orEmpty()
        episodeTitles = intent.getStringArrayListExtra(EXTRA_EPISODE_TITLES).orEmpty()
        episodeWatchKeys = intent.getStringArrayListExtra(EXTRA_EPISODE_WATCH_KEYS).orEmpty()
        episodeIndex = intent.getIntExtra(EXTRA_EPISODE_INDEX, -1)
        if (episodeIndex !in episodeUrls.indices) {
            episodeIndex = episodeUrls.indexOf(startUrl)
        }
        markCurrentEpisodeWatched()
        referer = intent.getStringExtra(EXTRA_REFERER).orEmpty().ifBlank { DEFAULT_REFERER }
        useIframe = intent.getBooleanExtra(EXTRA_USE_IFRAME, false)
        blockGamblingAds = getSharedPreferences("blooders_tv_prefs", MODE_PRIVATE)
            .getBoolean("block_gambling_ads", true)
        pageTitle = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        desktopMode = isTelevisionOrLargeScreen()

        extensions = ExtensionEngine.loadExtensions(applicationContext)
        extensionNetworkFilter = ExtensionNetworkFilter(extensions)

        webView = WebView(this)
        configureWebView()
        val chromeMajor = Regex("Chrome/(\\d+)")
            .find(WebSettings.getDefaultUserAgent(this))
            ?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        val isTv = packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_TELEVISION)
        val mode = getSharedPreferences("blooders_tv_prefs", android.content.Context.MODE_PRIVATE)
            .getString("player_mode", "auto") ?: "auto"
        val tvMode = mode == "tv" || (mode == "auto" && isTv)
        legacyWebView = tvMode && chromeMajor in 1..79
        installDocumentStartExtensions()
        setupAutoNext()

        setContent {
            MyApplicationTheme {
                PlayerWebScreen(
                    webView = webView,
                    title = pageTitle.ifBlank { "NSRPlay" },
                    isLoading = isLoading,
                    progress = loadProgress,
                    errorMessage = errorMessage,
                    desktopMode = desktopMode,
                    playbackSpeed = playbackSpeed,
                    controlsVisible = controlsVisible,
                    isPip = isPip,
                    isFullscreenVideo = isFullscreenVideo,
                    onBack = { handleBack() },
                    onReload = { retry() },
                    onToggleDesktop = { toggleDesktopMode() },
                    onCycleSpeed = { cycleSpeed() },
                    onEnterPip = { enterPipMode() },
                    onRetry = { retry() },
                    onShowControls = { controlsVisible = true },
                    onHideControls = { controlsVisible = false },
                    onClose = { finish() }
                )
            }
        }

        if (startUrl.isBlank()) {
            errorMessage = "No se recibiÃ³ un enlace de reproducciÃ³n."
            isLoading = false
        } else {
            loadStartUrl()
            // Never leave the app's loading overlay on top forever. If the
            // provider's iframe is slow, show its page so the user can retry.
            webView.postDelayed({
                if (!isFinishing && isLoading) isLoading = false
            }, 20_000)
        }
    }

    /**
     * Unlimplay validates its /f/embed page as an iframe. Loading it as a
     * top-level document returns ERR_TOKEN_EXPIRED; this wrapper is the exact
     * iframe usage that works on the provider's example page.
     */
    /**
     * Remote-control support for web players that only expose a touch play
     * button. Media keys always control the video; navigation keys are left to
     * Compose unless the WebView owns the focus.
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
                KeyEvent.KEYCODE_MEDIA_PLAY,
                KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                    if (useIframe) tapCenter()
                    else webView.evaluateJavascript(PlayerAdBlocker.TOGGLE_PLAY_SCRIPT, null)
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    webView.evaluateJavascript(PlayerAdBlocker.seekScript(10), null)
                    return true
                }
                KeyEvent.KEYCODE_MEDIA_REWIND,
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    webView.evaluateJavascript(PlayerAdBlocker.seekScript(-10), null)
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER,
                KeyEvent.KEYCODE_ENTER,
                KeyEvent.KEYCODE_NUMPAD_ENTER,
                KeyEvent.KEYCODE_SPACE -> {
                    if (webView.hasFocus()) {
                        tapCenter()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (webView.hasFocus()) {
                        webView.evaluateJavascript(PlayerAdBlocker.seekScript(-10), null)
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (webView.hasFocus()) {
                        webView.evaluateJavascript(PlayerAdBlocker.seekScript(10), null)
                        return true
                    }
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun tapCenter() {
        val width = webView.width
        val height = webView.height
        if (width <= 0 || height <= 0) return
        val now = SystemClock.uptimeMillis()
        val x = width / 2f
        val y = height / 2f
        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(now, now + 80, MotionEvent.ACTION_UP, x, y, 0)
        webView.dispatchTouchEvent(down)
        webView.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
    }

    private fun tapSkipArea() {
        val width = webView.width
        val height = webView.height
        if (width <= 0 || height <= 0) return
        val now = SystemClock.uptimeMillis()
        val x = width * 0.88f
        val y = height * 0.93f
        val down = MotionEvent.obtain(now, now, MotionEvent.ACTION_DOWN, x, y, 0)
        val up = MotionEvent.obtain(now, now + 80, MotionEvent.ACTION_UP, x, y, 0)
        webView.dispatchTouchEvent(down)
        webView.dispatchTouchEvent(up)
        down.recycle()
        up.recycle()
    }

    @SuppressLint("JavascriptInterface")
    private fun setupAutoNext() {
        if (episodeIndex !in 0 until episodeUrls.lastIndex) return
        webView.addJavascriptInterface(AutoNextBridge(), "BloodersBridge")
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(
                webView,
                AUTO_NEXT_SCRIPT,
                setOf("*")
            )
        }
    }

    private inner class AutoNextBridge {
        @JavascriptInterface
        fun onVideoEnded() {
            runOnUiThread { playNextEpisode() }
        }
    }

    private fun markCurrentEpisodeWatched() {
        val key = episodeWatchKeys.getOrNull(episodeIndex).orEmpty()
        if (key.isNotBlank()) {
            WebEpisodeWatchStore.mark(this, key)
        }
    }

    private fun playNextEpisode() {
        if (episodeIndex !in 0 until episodeUrls.lastIndex) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastAutoNextAt < 2_500L) return
        lastAutoNextAt = now

        episodeIndex++
        markCurrentEpisodeWatched()
        startUrl = episodeUrls[episodeIndex]
        pageTitle = episodeTitles.getOrNull(episodeIndex)
            ?: pageTitle
        isLoading = true
        loadProgress = 0
        errorMessage = null
        loadStartUrl()
    }

    private fun installDocumentStartExtensions() {
        if (extensions.isEmpty()) return
        // Injecting the full extension bundle into every frame overwhelms
        // low-RAM TV boxes and older WebViews. They keep the top-level
        // injection that worked before.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val activityManager = getSystemService(ActivityManager::class.java)
        if (activityManager?.isLowRamDevice == true) return
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return
        val script = ExtensionEngine.generateAllFramesScript(extensions)
        if (script.isBlank()) return
        extensionScriptHandler = WebViewCompat.addDocumentStartJavaScript(
            webView,
            script,
            setOf("*")
        )
    }

    private fun loadStartUrl() {
        if (useIframe) {
            val safeUrl = startUrl
                .replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
            val html = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <style>
                    html, body { margin:0; padding:0; width:100%; height:100%; background:#000; overflow:hidden; }
                    iframe { position:fixed; inset:0; width:100%; height:100%; border:0; }
                  </style>
                </head>
                <body>
                  <iframe src="$safeUrl"
                    width="100%" height="100%"
                    allow="autoplay; fullscreen; encrypted-media; picture-in-picture"
                    allowfullscreen
                    referrerpolicy="origin"></iframe>
                </body>
                </html>
            """.trimIndent()
            webView.loadDataWithBaseURL(referer, html, "text/html", "UTF-8", null)
        } else {
            webView.loadUrl(startUrl, mapOf("Referer" to referer))
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureWebView() {
        chromeMajorVersion = Regex("Chrome/(\\d+)")
            .find(WebSettings.getDefaultUserAgent(this))
            ?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 128
        webView.setBackgroundColor(AndroidColor.BLACK)
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = when {
                useIframe -> mobileUserAgent(chromeMajorVersion)
                desktopMode -> desktopUserAgent(chromeMajorVersion)
                else -> null
            }
            textZoom = 100
        }

        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                setAcceptThirdPartyCookies(webView, true)
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                val url = request?.url?.toString() ?: return null
                if (legacyWebView && request != null &&
                    LegacyWebPatcher.shouldPatch(request)
                ) {
                    LegacyWebPatcher.responseFor(request)?.let { return it }
                }
                val blockedByExtension = request?.isForMainFrame == false &&
                    extensionNetworkFilter.shouldBlock(url)
                if (PlayerAdBlocker.isAdUrl(url, blockGamblingAds) || blockedByExtension) {
                    return WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        PlayerAdBlocker.createEmptyResponse()
                    )
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return true
                if (!url.startsWith("http://") && !url.startsWith("https://")) {
                    // intent://, market://, tg:// ... are almost always ad redirects.
                    return true
                }
                if (PlayerAdBlocker.isAdUrl(url, blockGamblingAds)) return true
                if (extensionNetworkFilter.shouldBlock(url)) return true

                // Extension redirect rules apply to top-level navigations.
                val redirect = extensionNetworkFilter.redirectFor(url)
                if (redirect != null && redirect != url) {
                    view?.loadUrl(redirect)
                    return true
                }
                if (extensionNetworkFilter.shouldUpgradeToHttps(url)) {
                    view?.loadUrl(url.replaceFirst("http://", "https://"))
                    return true
                }
                return false
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                isLoading = true
                loadProgress = 5
                errorMessage = null
            }

            override fun onPageCommitVisible(view: WebView?, url: String?) {
                super.onPageCommitVisible(view, url)
                if (url != null) {
                    val extensionScript = ExtensionEngine.generateInjectionScript(extensions, url)
                    if (extensionScript.isNotBlank()) {
                        view?.evaluateJavascript(extensionScript, null)
                    }
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isLoading = false
                loadProgress = 100
                view?.evaluateJavascript(PlayerAdBlocker.PAGE_SCRIPT, null)
                view?.evaluateJavascript(PlayerAdBlocker.AUTOPLAY_SCRIPT, null)
                if (episodeIndex in 0 until episodeUrls.lastIndex) {
                    view?.evaluateJavascript(AUTO_NEXT_SCRIPT, null)
                }
                view?.evaluateJavascript(
                    PlayerAdBlocker.playbackSpeedScript(playbackSpeed),
                    null
                )
                view?.requestFocus()
                if (useIframe) {
                    // Cross-origin iframes cannot be scripted; a synthetic tap
                    // in the centre presses the provider's own play overlay.
                    webView.postDelayed({ tapCenter() }, 1_800)
                    // Tap the usual lower-right "Saltar anuncio" position
                    // without touching the central play/pause area.
                    listOf(3_500L, 4_200L, 4_900L, 5_600L, 6_300L, 7_000L, 7_700L)
                        .forEach { delayMs ->
                            webView.postDelayed({ tapSkipArea() }, delayMs)
                        }
                }
                if (url != null) {
                    val extensionScript = ExtensionEngine.generateInjectionScript(extensions, url)
                    if (extensionScript.isNotBlank()) {
                        view?.evaluateJavascript(extensionScript, null)
                    }
                }
                val title = view?.title
                if (!title.isNullOrBlank() && !title.startsWith("http")) {
                    pageTitle = title
                }
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    isLoading = false
                    errorMessage = "No se pudo abrir el reproductor. Revisa tu conexiÃ³n e intÃ©ntalo de nuevo."
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                loadProgress = newProgress
                isLoading = newProgress < 100
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                if (!title.isNullOrBlank() && !title.startsWith("http")) pageTitle = title
            }

            override fun onShowCustomView(view: View?, callback: WebChromeClient.CustomViewCallback?) {
                if (customView != null || view == null) {
                    callback?.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                isFullscreenVideo = true
                controlsVisible = false
                hideSystemBars()

                val decor = window.decorView as? FrameLayout
                decor?.addView(
                    view,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                view.setBackgroundColor(AndroidColor.BLACK)
            }

            override fun onHideCustomView() {
                val view = customView ?: return
                (view.parent as? ViewGroup)?.removeView(view)
                customView = null
                isFullscreenVideo = false
                controlsVisible = true
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                webView.visibility = View.VISIBLE
                hideSystemBars()
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                // Never allow provider pop-unders to create a second window.
                return false
            }

            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.deny()
            }
        }
    }

    private fun handleBack() {
        when {
            customView != null -> {
                val view = customView
                if (view != null) {
                    (view.parent as? ViewGroup)?.removeView(view)
                }
                customView = null
                isFullscreenVideo = false
                customViewCallback?.onCustomViewHidden()
                customViewCallback = null
                controlsVisible = true
            }
            webView.canGoBack() -> webView.goBack()
            else -> finish()
        }
    }

    private fun retry() {
        errorMessage = null
        if (startUrl.isNotBlank()) {
            loadStartUrl()
        }
    }

    private fun toggleDesktopMode() {
        desktopMode = !desktopMode
        webView.settings.userAgentString = when {
            useIframe -> mobileUserAgent(chromeMajorVersion)
            desktopMode -> desktopUserAgent(chromeMajorVersion)
            else -> null
        }
        webView.reload()
    }

    private fun cycleSpeed() {
        val currentIndex = SPEEDS.indexOfFirst { it == playbackSpeed }.coerceAtLeast(0)
        playbackSpeed = SPEEDS[(currentIndex + 1) % SPEEDS.size]
        webView.evaluateJavascript(
            PlayerAdBlocker.playbackSpeedScript(playbackSpeed),
            null
        )
    }

    private fun enterPipMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) return
        try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            enterPictureInPictureMode(params)
        } catch (_: Throwable) {
            // Device without PiP support: keep playing in the normal window.
        }
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    private fun isTelevisionOrLargeScreen(): Boolean {
        val uiMode = resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK
        return uiMode == Configuration.UI_MODE_TYPE_TELEVISION ||
            resources.configuration.screenWidthDp >= 840
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isPip = isInPictureInPictureMode
        controlsVisible = !isInPictureInPictureMode
    }

    override fun onStop() {
        super.onStop()
        // onStop is not called while in Picture-in-Picture, so video audio keeps
        // playing there. Anywhere else we release WebView timers to save battery.
        val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode
        if (!inPip && ::webView.isInitialized) {
            webView.onPause()
            webView.pauseTimers()
        }
    }

    override fun onStart() {
        super.onStart()
        if (::webView.isInitialized) {
            webView.onResume()
            webView.resumeTimers()
        }
    }

    override fun onDestroy() {
        try {
            customView?.let { view -> (view.parent as? ViewGroup)?.removeView(view) }
            customView = null
            if (::webView.isInitialized) {
                webView.stopLoading()
                webView.destroy()
            }
        } catch (_: Throwable) {
            // Nothing else to release.
        }
        super.onDestroy()
    }
}

@Composable
private fun PlayerWebScreen(
    webView: WebView,
    title: String,
    isLoading: Boolean,
    progress: Int,
    errorMessage: String?,
    desktopMode: Boolean,
    playbackSpeed: Float,
    controlsVisible: Boolean,
    isPip: Boolean,
    isFullscreenVideo: Boolean,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onToggleDesktop: () -> Unit,
    onCycleSpeed: () -> Unit,
    onEnterPip: () -> Unit,
    onRetry: () -> Unit,
    onShowControls: () -> Unit,
    onHideControls: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onBack() }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(5000)
            onHideControls()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { webView },
            modifier = Modifier
                .fillMaxSize()
                .testTag("nsrplay_webview")
        )

        if (isLoading && progress in 1..99) {
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter),
                color = Color(0xFFCCFF00),
                trackColor = Color(0x33000000)
            )
        }

        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .width(320.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0F1722))
                        .padding(20.dp)
                ) {
                    Text(
                        text = "No se pudo cargar el reproductor",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFF9FB0C0),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onClose,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1E2833),
                                contentColor = Color(0xFFC0CAD5)
                            )
                        ) {
                            Text("Cerrar", fontSize = 12.sp)
                        }
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFCCFF00),
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Reintentar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (!isPip && !isFullscreenVideo) {
            if (controlsVisible) {
                PlayerTopBar(
                    title = title,
                    desktopMode = desktopMode,
                    playbackSpeed = playbackSpeed,
                    onBack = onBack,
                    onReload = onReload,
                    onToggleDesktop = onToggleDesktop,
                    onCycleSpeed = onCycleSpeed,
                    onEnterPip = onEnterPip,
                    onHide = onHideControls,
                    onClose = onClose
                )
            } else {
                // Tiny always-available handle: tap / D-pad center shows controls.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0x66000000))
                        .clickable(onClick = onShowControls)
                        .focusable()
                        .testTag("player_controls_handle"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Mostrar controles",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        if (isPip) {
            // Keep PiP clean; the system overlay handles going back.
            Spacer(modifier = Modifier.size(0.dp))
        } else if (isLoading && progress == 0) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color(0xFFCCFF00)
            )
        }
    }
}

@Composable
private fun PlayerTopBar(
    title: String,
    desktopMode: Boolean,
    playbackSpeed: Float,
    onBack: () -> Unit,
    onReload: () -> Unit,
    onToggleDesktop: () -> Unit,
    onCycleSpeed: () -> Unit,
    onEnterPip: () -> Unit,
    onHide: () -> Unit,
    onClose: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xE6000000), Color(0x99000000), Color.Transparent)
                )
            )
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ControlIcon(
            icon = Icons.Default.ArrowBack,
            description = "Volver",
            onClick = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Reproductor NSRPlay",
                color = Color(0xFFB8C6D3),
                fontSize = 10.sp,
                maxLines = 1
            )
        }

        ControlIcon(
            icon = Icons.Default.Refresh,
            description = "Recargar",
            onClick = onReload
        )

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .clickable(onClick = onCycleSpeed)
                .focusable()
                .testTag("player_speed_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (playbackSpeed == playbackSpeed.toInt().toFloat()) {
                    "${playbackSpeed.toInt()}x"
                } else {
                    "${playbackSpeed}x"
                },
                color = Color(0xFFCCFF00),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        ControlIcon(
            icon = Icons.Default.DesktopWindows,
            description = "Modo escritorio",
            tint = if (desktopMode) Color(0xFFCCFF00) else Color.White,
            onClick = onToggleDesktop
        )

        ControlIcon(
            icon = Icons.Default.PictureInPicture,
            description = "Picture in picture",
            onClick = onEnterPip
        )

        ControlIcon(
            icon = Icons.Default.Close,
            description = "Cerrar reproductor",
            onClick = onClose
        )
    }
}

@Composable
private fun ControlIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .focusable(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}

