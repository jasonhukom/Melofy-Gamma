package com.melofy.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.melofy.app.data.TrackRepository
import com.melofy.app.model.Track
import com.melofy.app.player.MelofyPlayerManager
import com.melofy.app.ui.components.BottomNavTab
import com.melofy.app.ui.components.FullPlayerModal
import com.melofy.app.ui.components.MelofyBottomBar
import com.melofy.app.ui.components.MiniPlayer
import com.melofy.app.ui.screens.DownloadScreen
import com.melofy.app.ui.screens.HomeScreen
import com.melofy.app.ui.screens.ProfileScreen
import com.melofy.app.ui.screens.SearchScreen
import com.melofy.app.ui.screens.TrendingScreen
import com.melofy.app.ui.theme.BgVoid
import com.melofy.app.ui.theme.MelofyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var repository: TrackRepository
    private lateinit var playerManager: MelofyPlayerManager

    private var isLoggedInState = mutableStateOf(false)
    private var userNameState = mutableStateOf("Anonymous Listener")
    private var userAvatarState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = TrackRepository(applicationContext)
        playerManager = MelofyPlayerManager(applicationContext)

        handleDeepLink(intent)

        setContent {
            MelofyTheme {
                MelofyApp(
                    repository = repository,
                    playerManager = playerManager,
                    isLoggedIn = isLoggedInState.value,
                    userName = userNameState.value,
                    userAvatar = userAvatarState.value,
                    onConnectYouTube = {
                        launchYouTubeOAuth()
                    },
                    onToggleLoginState = {
                        if (isLoggedInState.value) {
                            isLoggedInState.value = false
                            userNameState.value = "Anonymous Listener"
                            userAvatarState.value = null
                            Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
                        } else {
                            launchYouTubeOAuth()
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri: Uri = intent?.data ?: return
        if (uri.scheme == "melofy" && uri.host == "auth") {
            val code = uri.getQueryParameter("code")
            if (!code.isNullOrEmpty()) {
                isLoggedInState.value = true
                userNameState.value = "YouTube Premium Listener"
                userAvatarState.value = "https://picsum.photos/id/64/200/200"
                Toast.makeText(this, "Successfully connected YouTube account!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun launchYouTubeOAuth() {
        try {
            val authUrl = Uri.parse("https://melofy-gamma.vercel.app/api/auth/google")
            CustomTabsIntent.Builder().build().launchUrl(this, authUrl)
        } catch (e: Exception) {
            // Fallback for demo sign-in
            isLoggedInState.value = true
            userNameState.value = "Alex Melofy"
            userAvatarState.value = "https://picsum.photos/id/64/200/200"
            Toast.makeText(this, "Signed in as Alex Melofy", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        playerManager.release()
        super.onDestroy()
    }
}

@Composable
fun MelofyApp(
    repository: TrackRepository,
    playerManager: MelofyPlayerManager,
    isLoggedIn: Boolean,
    userName: String,
    userAvatar: String?,
    onConnectYouTube: () -> Unit,
    onToggleLoginState: () -> Unit
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    val tracks by repository.tracksFlow.collectAsState()
    val likedTrackIds by repository.likedTrackIds.collectAsState()
    val downloadingTrackIds by repository.downloadingTrackIds.collectAsState()
    val playerState by playerManager.uiState.collectAsState()

    var isFullPlayerVisible by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: BottomNavTab.Home.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            Column(modifier = Modifier.background(BgVoid)) {
                // Mini Player
                if (playerState.currentTrack != null && !isFullPlayerVisible) {
                    MiniPlayer(
                        track = playerState.currentTrack,
                        isPlaying = playerState.isPlaying,
                        progressMs = playerState.positionMs,
                        durationMs = playerState.durationMs,
                        onPlayPauseToggle = { playerManager.togglePlayPause() },
                        onSkipNext = { playerManager.skipNext() },
                        onMiniPlayerClick = { isFullPlayerVisible = true }
                    )
                }

                // Bottom Navbar
                MelofyBottomBar(
                    currentRoute = currentRoute,
                    onTabSelected = { tab ->
                        navController.navigate(tab.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    isUserLoggedIn = isLoggedIn,
                    userProfileUrl = userAvatar
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BgVoid)
        ) {
            NavHost(
                navController = navController,
                startDestination = BottomNavTab.Home.route,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(BottomNavTab.Home.route) {
                    HomeScreen(
                        tracks = tracks,
                        currentTrack = playerState.currentTrack,
                        downloadingTrackIds = downloadingTrackIds,
                        onTrackClick = { track ->
                            playerManager.playTrack(track, tracks)
                        },
                        onToggleLike = { trackId ->
                            repository.toggleLike(trackId)
                        },
                        onDownloadClick = { track ->
                            scope.launch {
                                repository.downloadTrack(track)
                            }
                        }
                    )
                }

                composable(BottomNavTab.Trending.route) {
                    TrendingScreen(
                        tracks = tracks,
                        currentTrack = playerState.currentTrack,
                        downloadingTrackIds = downloadingTrackIds,
                        onTrackClick = { track ->
                            playerManager.playTrack(track, tracks)
                        },
                        onToggleLike = { trackId ->
                            repository.toggleLike(trackId)
                        },
                        onDownloadClick = { track ->
                            scope.launch {
                                repository.downloadTrack(track)
                            }
                        }
                    )
                }

                composable(BottomNavTab.Search.route) {
                    SearchScreen(
                        tracks = tracks,
                        currentTrack = playerState.currentTrack,
                        downloadingTrackIds = downloadingTrackIds,
                        onTrackClick = { track ->
                            playerManager.playTrack(track, tracks)
                        },
                        onToggleLike = { trackId ->
                            repository.toggleLike(trackId)
                        },
                        onDownloadClick = { track ->
                            scope.launch {
                                repository.downloadTrack(track)
                            }
                        }
                    )
                }

                composable(BottomNavTab.Download.route) {
                    DownloadScreen(
                        tracks = tracks,
                        currentTrack = playerState.currentTrack,
                        downloadingTrackIds = downloadingTrackIds,
                        onTrackClick = { track ->
                            playerManager.playTrack(track, tracks.filter { it.isDownloaded })
                        },
                        onPlayAllOffline = { offlineTracks ->
                            if (offlineTracks.isNotEmpty()) {
                                playerManager.playTrack(offlineTracks.first(), offlineTracks)
                            }
                        },
                        onShuffleOffline = { offlineTracks ->
                            if (offlineTracks.isNotEmpty()) {
                                val shuffled = offlineTracks.shuffled()
                                playerManager.playTrack(shuffled.first(), shuffled)
                            }
                        },
                        onToggleLike = { trackId ->
                            repository.toggleLike(trackId)
                        },
                        onRemoveDownloadClick = { track ->
                            scope.launch {
                                repository.removeDownload(track.id)
                            }
                        }
                    )
                }

                composable(BottomNavTab.Profile.route) {
                    ProfileScreen(
                        likedCount = likedTrackIds.size,
                        downloadedCount = tracks.count { it.isDownloaded },
                        isLoggedIn = isLoggedIn,
                        userDisplayName = userName,
                        userAvatarUrl = userAvatar,
                        onToggleLoginState = onToggleLoginState
                    )
                }
            }

            // Full Player Modal Overlay
            if (isFullPlayerVisible && playerState.currentTrack != null) {
                FullPlayerModal(
                    track = playerState.currentTrack,
                    isPlaying = playerState.isPlaying,
                    progressMs = playerState.positionMs,
                    durationMs = playerState.durationMs,
                    isShuffle = playerState.isShuffle,
                    isRepeat = playerState.isRepeat,
                    isDownloading = downloadingTrackIds.contains(playerState.currentTrack?.id ?: ""),
                    onDismiss = { isFullPlayerVisible = false },
                    onPlayPauseToggle = { playerManager.togglePlayPause() },
                    onSeek = { pos -> playerManager.seekTo(pos) },
                    onSkipNext = { playerManager.skipNext() },
                    onSkipPrevious = { playerManager.skipPrevious() },
                    onToggleShuffle = { playerManager.toggleShuffle() },
                    onToggleRepeat = { playerManager.toggleRepeat() },
                    onToggleLike = { trackId -> repository.toggleLike(trackId) },
                    onDownloadClick = { track ->
                        scope.launch {
                            repository.downloadTrack(track)
                        }
                    }
                )
            }
        }
    }
}
