package com.melofy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melofy.app.model.Track
import com.melofy.app.ui.components.TrackRow
import com.melofy.app.ui.theme.BgVoid
import com.melofy.app.ui.theme.CyanAccent
import com.melofy.app.ui.theme.GreenOnline
import com.melofy.app.ui.theme.IndigoSecondary
import com.melofy.app.ui.theme.PurplePrimary
import com.melofy.app.ui.theme.SurfaceBorder
import com.melofy.app.ui.theme.SurfaceCard
import com.melofy.app.ui.theme.TextMuted
import com.melofy.app.ui.theme.TextPrimary

@Composable
fun DownloadScreen(
    tracks: List<Track>,
    currentTrack: Track?,
    downloadingTrackIds: Set<String>,
    onTrackClick: (Track) -> Unit,
    onPlayAllOffline: (List<Track>) -> Unit,
    onShuffleOffline: (List<Track>) -> Unit,
    onToggleLike: (String) -> Unit,
    onRemoveDownloadClick: (Track) -> Unit
) {
    val downloadedTracks = tracks.filter { it.isDownloaded }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgVoid)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.OfflinePin,
                        contentDescription = "Offline Downloads",
                        tint = CyanAccent,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Offline Music Library",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Playable anytime without internet connection",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Storage Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF0F2B32), Color(0xFF13182C))
                            )
                        )
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(GreenOnline)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "OFFLINE STORAGE ACTIVE",
                                    color = GreenOnline,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }

                            Text(
                                text = "${downloadedTracks.size} Tracks Saved",
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { if (downloadedTracks.isNotEmpty()) onPlayAllOffline(downloadedTracks) },
                                enabled = downloadedTracks.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PurplePrimary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Play All", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { if (downloadedTracks.isNotEmpty()) onShuffleOffline(downloadedTracks) },
                                enabled = downloadedTracks.isNotEmpty(),
                                modifier = Modifier.weight(1f),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(PurplePrimary, IndigoSecondary))),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Shuffle,
                                    contentDescription = null,
                                    tint = PurplePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Shuffle", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // List Title
            item {
                Text(
                    text = "Downloaded Tracks",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            if (downloadedTracks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp, bottom = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.DownloadDone,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No downloaded tracks yet",
                                color = TextMuted,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Tap the download icon on any song to listen offline",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                items(downloadedTracks) { track ->
                    Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                        TrackRow(
                            track = track,
                            isPlayingCurrent = currentTrack?.id == track.id,
                            isDownloading = downloadingTrackIds.contains(track.id),
                            onTrackClick = onTrackClick,
                            onToggleLike = onToggleLike,
                            onDownloadClick = { onRemoveDownloadClick(track) }
                        )
                    }
                }
            }
        }
    }
}
