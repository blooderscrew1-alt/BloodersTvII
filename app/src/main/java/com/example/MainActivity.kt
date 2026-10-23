package com.example

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.data.model.PlaylistItem
import com.example.data.model.ItemType
import com.example.data.player.SmartPlayerManager
import com.example.data.repository.IptvRepository
import com.example.ui.components.AppTab
import com.example.ui.components.TopHeaderBar
import com.example.ui.screens.FullscreenPlayerScreen
import com.example.ui.screens.MoviesScreen
import com.example.ui.screens.PlaylistsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SeriesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TelevisionScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: IptvRepository
    private lateinit var playerManager: SmartPlayerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()

        repository = IptvRepository(applicationContext)
        playerManager = SmartPlayerManager(applicationContext)

        setContent {
            val themeAccent by repository.themeAccent.collectAsState()
            val primaryColor = Color(themeAccent.primaryHex)

            MyApplicationTheme {
                MainApp(
                    repository = repository,
                    playerManager = playerManager,
                    primaryColor = primaryColor
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (::playerManager.isInitialized) {
            playerManager.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::playerManager.isInitialized) {
            playerManager.releasePlayer()
        }
    }
}

@Composable
fun MainApp(
    repository: IptvRepository,
    playerManager: SmartPlayerManager,
    primaryColor: Color
) {
    var currentTab by remember { mutableStateOf(AppTab.PELICULAS) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var fullscreenItem by remember { mutableStateOf<PlaylistItem?>(null) }

    // VOD playback resumes where the watch history left it. Live channels start
    // at the live edge.
    fun startPlayback(item: PlaylistItem) {
        val resumePosition = if (item.type != ItemType.LIVE) {
            repository.history.value.firstOrNull { it.itemId == item.id }?.positionMs ?: 0L
        } else {
            0L
        }
        playerManager.playStream(item.streamUrl, resumePosition)
        fullscreenItem = item
    }

    if (fullscreenItem != null) {
        // Fullscreen video player takes over entire screen
        FullscreenPlayerScreen(
            currentItem = fullscreenItem!!,
            repository = repository,
            playerManager = playerManager,
            onClose = { fullscreenItem = null },
            primaryColor = primaryColor
        )
    } else {
        // If search is open, pressing back closes search
        if (isSearchOpen) {
            BackHandler {
                isSearchOpen = false
            }
        }

        Scaffold(
            topBar = {
                TopHeaderBar(
                    currentTab = currentTab,
                    onTabSelected = {
                        isSearchOpen = false
                        currentTab = it
                    },
                    onSearchClick = {
                        isSearchOpen = !isSearchOpen
                    },
                    primaryColor = primaryColor
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF090E13))
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFF090E13))
            ) {
                if (isSearchOpen) {
                    SearchScreen(
                        repository = repository,
                        onPlayItem = { item -> startPlayback(item) },
                        primaryColor = primaryColor
                    )
                } else {
                    // Direct switch: Crossfade kept both tab trees composed during
                    // the animation, which reloaded images and dropped frames on
                    // low-end devices.
                    val tab = currentTab
                    when (tab) {
                        AppTab.TELEVISION -> TelevisionScreen(
                            repository = repository,
                            playerManager = playerManager,
                            onEnterFullscreen = { item ->
                                fullscreenItem = item
                            },
                            onNavigateToPlaylists = {
                                currentTab = AppTab.PLAYLISTS
                            },
                            primaryColor = primaryColor
                        )
                        AppTab.PELICULAS -> MoviesScreen(
                            repository = repository,
                            onPlayItem = { item -> startPlayback(item) },
                            onNavigateToPlaylists = {
                                currentTab = AppTab.PLAYLISTS
                            },
                            primaryColor = primaryColor
                        )
                        AppTab.SERIES -> SeriesScreen(
                            repository = repository,
                            onPlayItem = { item -> startPlayback(item) },
                            onNavigateToPlaylists = {
                                currentTab = AppTab.PLAYLISTS
                            },
                            primaryColor = primaryColor
                        )
                        AppTab.PLAYLISTS -> PlaylistsScreen(
                            repository = repository,
                            primaryColor = primaryColor
                        )
                        AppTab.AJUSTES -> SettingsScreen(
                            repository = repository,
                            playerManager = playerManager,
                            primaryColor = primaryColor
                        )
                    }
                }
            }
        }
    }
}
