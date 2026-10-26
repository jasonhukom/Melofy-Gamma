package com.melofy.app.ui.adapters;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.melofy.app.databinding.ItemTrackCardBinding;
import com.melofy.app.model.Track;

import java.util.ArrayList;
import java.util.List;

public class TrackCardAdapter extends RecyclerView.Adapter<TrackCardAdapter.CardViewHolder> {

    public interface OnTrackCardClickListener {
        void onCardClick(Track track);
    }

    private final List<Track> tracks = new ArrayList<>();
    private final OnTrackCardClickListener listener;

    public TrackCardAdapter(OnTrackCardClickListener listener) {
        this.listener = listener;
    }

    public void setTracks(List<Track> newTracks) {
        this.tracks.clear();
        if (newTracks != null) {
            this.tracks.addAll(newTracks);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTrackCardBinding binding = ItemTrackCardBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );
        return new CardViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Track track = tracks.get(position);
        holder.bind(track, listener);
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    static class CardViewHolder extends RecyclerView.ViewHolder {
        private final ItemTrackCardBinding binding;

        public CardViewHolder(ItemTrackCardBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(Track track, OnTrackCardClickListener listener) {
            binding.textCardTitle.setText(track.getTitle());
            binding.textCardArtist.setText(track.getArtist());

            Glide.with(binding.imgCardCover.getContext())
                    .load(track.getCoverUrl())
                    .into(binding.imgCardCover);

            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) listener.onCardClick(track);
            });
        }
    }
}
