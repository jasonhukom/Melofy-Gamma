package com.melofy.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.melofy.app.model.Track
import com.melofy.app.ui.theme.CyanAccent
import com.melofy.app.ui.theme.GreenOnline
import com.melofy.app.ui.theme.PurplePrimary
import com.melofy.app.ui.theme.RedLiked
import com.melofy.app.ui.theme.SurfaceBorder
import com.melofy.app.ui.theme.SurfaceCard
import com.melofy.app.ui.theme.TextMuted
import com.melofy.app.ui.theme.TextPrimary

@Composable
fun TrackRow(
    track: Track,
    isPlayingCurrent: Boolean,
    isDownloading: Boolean,
    onTrackClick: (Track) -> Unit,
    onToggleLike: (String) -> Unit,
    onDownloadClick: (Track) -> Unit,
    showRank: Boolean = false,
    rank: Int = 0,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPlayingCurrent) Color(0x1FA855F7) else SurfaceCard)
            .border(
                1.dp,
                if (isPlayingCurrent) PurplePrimary else SurfaceBorder,
                RoundedCornerShape(12.dp)
            )
            .clickable { onTrackClick(track) }
            .padding(vertical = 8.dp, horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showRank) {
            Text(
                text = "#$rank",
                color = if (rank <= 3) PurplePrimary else TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp)
            )
        }

        // Cover
        Box(contentAlignment = Alignment.Center) {
            AsyncImage(
                model = track.coverUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            )
            if (isPlayingCurrent) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x80000000)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = "Playing",
                        tint = PurplePrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and artist
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                color = if (isPlayingCurrent) PurplePrimary else TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${track.artist} • ${track.genre}",
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (track.isDownloaded) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "• Offline",
                        color = GreenOnline,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Actions
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { onToggleLike(track.id) },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (track.isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Like",
                    tint = if (track.isLiked) RedLiked else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = { onDownloadClick(track) },
                enabled = !isDownloading,
                modifier = Modifier.size(36.dp)
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = PurplePrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = if (track.isDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                        contentDescription = "Download",
                        tint = if (track.isDownloaded) CyanAccent else TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
