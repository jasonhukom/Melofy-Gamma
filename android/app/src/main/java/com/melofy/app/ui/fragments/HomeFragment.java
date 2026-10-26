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
import com.melofy.app.databinding.FragmentHomeBinding;
import com.melofy.app.model.Track;
import com.melofy.app.ui.adapters.TrackAdapter;
import com.melofy.app.ui.adapters.TrackCardAdapter;

import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private TrackAdapter trackAdapter;
    private TrackCardAdapter cardAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        cardAdapter = new TrackCardAdapter(track -> activity.playTrack(track, activity.getRepository().getAllTracks()));
        binding.recyclerQuickPicks.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.recyclerQuickPicks.setAdapter(cardAdapter);

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

        binding.recyclerMadeForYou.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerMadeForYou.setAdapter(trackAdapter);

        updateUi();
    }

    public void updateUi() {
        if (binding == null || getActivity() == null) return;
        MainActivity activity = (MainActivity) getActivity();

        List<Track> allTracks = activity.getRepository().getAllTracks();
        cardAdapter.setTracks(allTracks.size() > 4 ? allTracks.subList(0, 4) : allTracks);

        trackAdapter.setTracks(
                allTracks,
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
