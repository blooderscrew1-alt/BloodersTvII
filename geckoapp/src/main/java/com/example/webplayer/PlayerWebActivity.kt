package com.example.webplayer

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

/**
 * Gecko test build player. This class deliberately has no android.webkit
 * imports: the app uses GeckoView for every provider URL.
 */
class PlayerWebActivity : Activity() {
    private lateinit var geckoView: GeckoView
    private lateinit var session: GeckoSession

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PlayerWebActivityHolder.context = applicationContext
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val url = intent.getStringExtra(EXTRA_URL).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        if (url.isBlank()) {
            finish()
            return
        }

        val root = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        geckoView = GeckoView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        root.addView(geckoView)
        if (title.isNotBlank()) {
            root.addView(
                TextView(this).apply {
                    text = title
                    setTextColor(Color.WHITE)
                    textSize = 15f
                    setShadowLayer(6f, 0f, 2f, Color.BLACK)
                    setPadding(28, 22, 28, 12)
                },
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.TOP or Gravity.START
                )
            )
        }
        setContentView(root)

        session = GeckoSession().apply {
            open(getRuntime())
        }
        geckoView.setSession(session)
        session.loadUri(url)
    }

    override fun onBackPressed() {
        if (::session.isInitialized) {
            session.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        if (::session.isInitialized) {
            session.close()
        }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_REFERER = "extra_referer"
        const val EXTRA_USE_IFRAME = "extra_use_iframe"
        const val EXTRA_EPISODE_URLS = "extra_episode_urls"
        const val EXTRA_EPISODE_TITLES = "extra_episode_titles"
        const val EXTRA_EPISODE_WATCH_KEYS = "extra_episode_watch_keys"
        const val EXTRA_EPISODE_INDEX = "extra_episode_index"

        private var runtime: GeckoRuntime? = null

        @Synchronized
        private fun getRuntime(): GeckoRuntime {
            return runtime ?: GeckoRuntime.create(
                PlayerWebActivityHolder.context
            ).also { runtime = it }
        }
    }
}

private object PlayerWebActivityHolder {
    lateinit var context: android.content.Context
}
