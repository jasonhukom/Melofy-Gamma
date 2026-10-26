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
import com.melofy.app.databinding.FragmentTrendingBinding;
import com.melofy.app.model.Track;
import com.melofy.app.ui.adapters.TrackAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrendingFragment extends Fragment {

    private FragmentTrendingBinding binding;
    private TrackAdapter trackAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTrendingBinding.inflate(inflater, container, false);
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
                activity.playTrack(track, activity.getRepository().getAllTracks());
            }

            @Override
            public void onToggleLike(Track track) {
                activity.getRepository().toggleLike(track.getId());
            }

            @Override
            public void onDownloadClick(Track track) {
                activity.getRepository().downloadTrack(track);
            }
        });

        binding.recyclerTrending.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerTrending.setAdapter(trackAdapter);

        updateUi();
    }

    public void updateUi() {
        if (binding == null || getActivity() == null) return;
        MainActivity activity = (MainActivity) getActivity();

        List<Track> allTracks = new ArrayList<>(activity.getRepository().getAllTracks());
        Collections.sort(allTracks, (t1, t2) -> Integer.compare(t1.getRank(), t2.getRank()));

        trackAdapter.setTracks(
                allTracks,
                activity.getPlayerManager().getCurrentTrack(),
                activity.getRepository().getDownloadingTrackIds(),
                true
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
