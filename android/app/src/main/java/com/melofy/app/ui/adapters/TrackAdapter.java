package com.melofy.app.ui.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.melofy.app.databinding.ItemTrackRowBinding;
import com.melofy.app.model.Track;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    public interface TrackActionListener {
        void onTrackClick(Track track);
        void onToggleLike(Track track);
        void onDownloadClick(Track track);
    }

    private final List<Track> tracks = new ArrayList<>();
    private Track currentPlayingTrack = null;
    private Set<String> downloadingTrackIds = null;
    private boolean showRank = false;
    private TrackActionListener listener;

    public TrackAdapter(TrackActionListener listener) {
        this.listener = listener;
    }

    public void setTracks(List<Track> newTracks, Track currentPlayingTrack, Set<String> downloadingTrackIds, boolean showRank) {
        this.tracks.clear();
        if (newTracks != null) {
            this.tracks.addAll(newTracks);
        }
        this.currentPlayingTrack = currentPlayingTrack;
        this.downloadingTrackIds = downloadingTrackIds;
        this.showRank = showRank;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTrackRowBinding binding = ItemTrackRowBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new TrackViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        Track track = tracks.get(position);
        holder.bind(track, currentPlayingTrack, downloadingTrackIds, showRank, position + 1, listener);
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    static class TrackViewHolder extends RecyclerView.ViewHolder {
        private final ItemTrackRowBinding binding;

        public TrackViewHolder(ItemTrackRowBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Track track, Track currentPlaying, Set<String> downloadingIds, boolean showRank, int rankNumber, TrackActionListener listener) {
            boolean isCurrent = currentPlaying != null && currentPlaying.getId().equals(track.getId());
            boolean isDownloading = downloadingIds != null && downloadingIds.contains(track.getId());

            binding.textTitle.setText(track.getTitle());
            binding.textTitle.setTextColor(isCurrent ? Color.parseColor("#A855F7") : Color.parseColor("#F8FAFC"));

            binding.textArtist.setText(track.getArtist() + " • " + track.getGenre());

            if (showRank) {
                binding.textRank.setVisibility(View.VISIBLE);
                binding.textRank.setText("#" + (track.getRank() > 0 ? track.getRank() : rankNumber));
            } else {
                binding.textRank.setVisibility(View.GONE);
            }

            if (track.isDownloaded()) {
                binding.tagOfflineItem.setVisibility(View.VISIBLE);
                binding.btnDownload.setImageResource(android.R.drawable.stat_sys_download_done);
                binding.btnDownload.setColorFilter(Color.parseColor("#06B6D4"));
            } else {
                binding.tagOfflineItem.setVisibility(View.GONE);
                binding.btnDownload.setImageResource(android.R.drawable.stat_sys_download);
                binding.btnDownload.setColorFilter(Color.parseColor("#94A3B8"));
            }

            if (isDownloading) {
                binding.progressDownload.setVisibility(View.VISIBLE);
                binding.btnDownload.setVisibility(View.GONE);
            } else {
                binding.progressDownload.setVisibility(View.GONE);
                binding.btnDownload.setVisibility(View.VISIBLE);
            }

            if (track.isLiked()) {
                binding.btnLike.setImageResource(android.R.drawable.btn_star_big_on);
                binding.btnLike.setColorFilter(Color.parseColor("#EF4444"));
            } else {
                binding.btnLike.setImageResource(android.R.drawable.btn_star_big_off);
                binding.btnLike.setColorFilter(Color.parseColor("#94A3B8"));
            }

            Glide.with(binding.imgCover.getContext())
                    .load(track.getCoverUrl())
                    .into(binding.imgCover);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onTrackClick(track);
            });

            binding.btnLike.setOnClickListener(v -> {
                if (listener != null) listener.onToggleLike(track);
            });

            binding.btnDownload.setOnClickListener(v -> {
                if (listener != null) listener.onDownloadClick(track);
            });
        }
    }
}
