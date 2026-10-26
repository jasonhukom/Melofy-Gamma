package com.melofy.app.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.melofy.app.MainActivity;
import com.melofy.app.databinding.FragmentDownloadBinding;
import com.melofy.app.model.Track;
import com.melofy.app.ui.adapters.TrackAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DownloadFragment extends Fragment {

    private FragmentDownloadBinding binding;
    private TrackAdapter trackAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentDownloadBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        trackAdapter = new TrackAdapter(new TrackAdapter.TrackActionListener() {
            @Override
            public void onTrackClick(Track track) {
                activity.playTrack(track, activity.getRepository().getDownloadedTracks());
            }

            @Override
            public void onToggleLike(Track track) {
                activity.getRepository().toggleLike(track.getId());
            }

            @Override
            public void onDownloadClick(Track track) {
                activity.getRepository().removeDownload(track);
            }
        });

        binding.recyclerDownloadedTracks.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerDownloadedTracks.setAdapter(trackAdapter);

        binding.btnPlayAllOffline.setOnClickListener(v -> {
            List<Track> downloaded = activity.getRepository().getDownloadedTracks();
            if (!downloaded.isEmpty()) {
                activity.playTrack(downloaded.get(0), downloaded);
            }
        });

        binding.btnShuffleOffline.setOnClickListener(v -> {
            List<Track> downloaded = new ArrayList<>(activity.getRepository().getDownloadedTracks());
            if (!downloaded.isEmpty()) {
                Collections.shuffle(downloaded);
                activity.playTrack(downloaded.get(0), downloaded);
            }
        });

        updateUi();
    }

    public void updateUi() {
        if (binding == null || getActivity() == null) return;
        MainActivity activity = (MainActivity) getActivity();

        List<Track> downloaded = activity.getRepository().getDownloadedTracks();
        binding.textDownloadCount.setText(downloaded.size() + " Tracks Saved");

        trackAdapter.setTracks(
                downloaded,
                activity.getPlayerManager().getCurrentTrack(),
                activity.getRepository().getDownloadingTrackIds(),
                false
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
