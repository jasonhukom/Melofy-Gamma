package com.melofy.app.ui.fragments;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.melofy.app.MainActivity;
import com.melofy.app.R;
import com.melofy.app.databinding.FragmentSearchBinding;
import com.melofy.app.model.Track;
import com.melofy.app.ui.adapters.CategoryAdapter;
import com.melofy.app.ui.adapters.TrackAdapter;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private TrackAdapter trackAdapter;
    private CategoryAdapter categoryAdapter;
    private String currentQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        categoryAdapter = new CategoryAdapter(categoryName -> {
            binding.editSearchQuery.setText(categoryName.replace(" Beats", "").replace(" Tracks", ""));
        });

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

        binding.editSearchQuery.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().trim();
                updateUi();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        updateUi();
    }

    public void updateUi() {
        if (binding == null || getActivity() == null) return;
        MainActivity activity = (MainActivity) getActivity();

        if (currentQuery.isEmpty()) {
            binding.textSearchSection.setText("Browse Categories");
            binding.recyclerSearchResults.setLayoutManager(new GridLayoutManager(getContext(), 2));

            List<CategoryAdapter.CategoryItem> categories = new ArrayList<>();
            categories.add(new CategoryAdapter.CategoryItem("Chill Lo-Fi", R.drawable.bg_card_elevated));
            categories.add(new CategoryAdapter.CategoryItem("Synth Wave", R.drawable.bg_hero_banner));
            categories.add(new CategoryAdapter.CategoryItem("Acoustic Vibes", R.drawable.bg_card_surface));
            categories.add(new CategoryAdapter.CategoryItem("Offline Tracks", R.drawable.bg_card_elevated));
            categories.add(new CategoryAdapter.CategoryItem("Hip-Hop Beats", R.drawable.bg_hero_banner));
            categories.add(new CategoryAdapter.CategoryItem("Ambient & Zen", R.drawable.bg_card_surface));

            categoryAdapter.setCategories(categories);
            binding.recyclerSearchResults.setAdapter(categoryAdapter);
        } else {
            binding.textSearchSection.setText("Search Results");
            binding.recyclerSearchResults.setLayoutManager(new LinearLayoutManager(getContext()));

            List<Track> all = activity.getRepository().getAllTracks();
            List<Track> filtered = new ArrayList<>();
            for (Track t : all) {
                if (t.getTitle().toLowerCase().contains(currentQuery.toLowerCase()) ||
                    t.getArtist().toLowerCase().contains(currentQuery.toLowerCase()) ||
                    t.getGenre().toLowerCase().contains(currentQuery.toLowerCase())) {
                    filtered.add(t);
                }
            }

            trackAdapter.setTracks(
                    filtered,
                    activity.getPlayerManager().getCurrentTrack(),
                    activity.getRepository().getDownloadingTrackIds(),
                    false
            );
            binding.recyclerSearchResults.setAdapter(trackAdapter);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
