package com.melofy.app.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.melofy.app.MainActivity;
import com.melofy.app.databinding.FragmentProfileBinding;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        MainActivity activity = (MainActivity) getActivity();
        if (activity == null) return;

        binding.btnToggleAuth.setOnClickListener(v -> activity.toggleAuthMode());

        updateUi();
    }

    public void updateUi() {
        if (binding == null || getActivity() == null) return;
        MainActivity activity = (MainActivity) getActivity();

        int likedCount = activity.getRepository().getLikedTrackIds().size();
        int downloadedCount = activity.getRepository().getDownloadedTracks().size();

        binding.textStatLiked.setText(String.valueOf(likedCount));
        binding.textStatDownloaded.setText(String.valueOf(downloadedCount));

        if (activity.isLoggedIn()) {
            binding.textUserName.setText(activity.getUserName());
            binding.textUserStatus.setText("YouTube Connected Account");
            binding.btnToggleAuth.setText("Sign Out");

            if (activity.getUserAvatarUrl() != null) {
                Glide.with(this)
                        .load(activity.getUserAvatarUrl())
                        .into(binding.imgProfileAvatar);
            }
        } else {
            binding.textUserName.setText("Anonymous Listener");
            binding.textUserStatus.setText("Guest Mode • Offline Listening Available");
            binding.btnToggleAuth.setText("Connect YouTube / Google Account");
            binding.imgProfileAvatar.setImageResource(android.R.drawable.ic_menu_myplaces);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
