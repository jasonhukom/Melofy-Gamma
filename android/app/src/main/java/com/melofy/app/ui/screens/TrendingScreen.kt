package com.melofy.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melofy.app.model.Track
import com.melofy.app.ui.components.TrackRow
import com.melofy.app.ui.theme.BgVoid
import com.melofy.app.ui.theme.PurplePrimary
import com.melofy.app.ui.theme.SurfaceBorder
import com.melofy.app.ui.theme.SurfaceCard
import com.melofy.app.ui.theme.SurfaceElevated
import com.melofy.app.ui.theme.TextMuted
import com.melofy.app.ui.theme.TextPrimary

@Composable
fun TrendingScreen(
    tracks: List<Track>,
    currentTrack: Track?,
    downloadingTrackIds: Set<String>,
    onTrackClick: (Track) -> Unit,
    onToggleLike: (String) -> Unit,
    onDownloadClick: (Track) -> Unit
) {
    val genres = listOf("All", "Lo-Fi", "Electronic", "Pop", "Ambient", "Hip-Hop", "R&B")
    var selectedGenre by remember { mutableStateOf("All") }

    val filteredTracks = if (selectedGenre == "All") {
        tracks.sortedBy { it.rank }
    } else {
        tracks.filter { it.genre.equals(selectedGenre, ignoreCase = true) }
    }

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
                        imageVector = Icons.Filled.Whatshot,
                        contentDescription = "Trending",
                        tint = PurplePrimary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Trending Charts",
                            color = TextPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Top played tracks this week",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Genre Chips
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(genres) { genre ->
                        val isSelected = genre == selectedGenre
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) PurplePrimary else SurfaceCard)
                                .border(
                                    1.dp,
                                    if (isSelected) PurplePrimary else SurfaceBorder,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable { selectedGenre = genre }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = genre,
                                color = if (isSelected) Color.White else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Ranked List
            items(filteredTracks) { track ->
                Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)) {
                    TrackRow(
                        track = track,
                        isPlayingCurrent = currentTrack?.id == track.id,
                        isDownloading = downloadingTrackIds.contains(track.id),
                        onTrackClick = onTrackClick,
                        onToggleLike = onToggleLike,
                        onDownloadClick = onDownloadClick,
                        showRank = true,
                        rank = if (track.rank > 0) track.rank else filteredTracks.indexOf(track) + 1
                    )
                }
            }
        }
    }
}
