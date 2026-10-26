package com.melofy.app.ui.fragments;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.melofy.app.MainActivity;
import com.melofy.app.R;
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
            binding.btnToggleAuth.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#3316140E")));

            binding.imgProfileAvatar.setPadding(0, 0, 0, 0);
            binding.imgProfileAvatar.setScaleType(ImageView.ScaleType.CENTER_CROP);
            binding.imgProfileAvatar.setImageTintList(null);

            if (activity.getUserAvatarUrl() != null) {
                Glide.with(this)
                        .load(activity.getUserAvatarUrl())
                        .into(binding.imgProfileAvatar);
            } else {
                binding.imgProfileAvatar.setImageResource(R.drawable.ic_anonymous_pfp);
            }
        } else {
            binding.textUserName.setText("Anonymous Listener");
            binding.textUserStatus.setText("Guest Mode • Offline Listening Available");
            binding.btnToggleAuth.setText("Connect YouTube / Google Account");
            binding.btnToggleAuth.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FACC15")));

            int pad = (int) (12 * getResources().getDisplayMetrics().density);
            binding.imgProfileAvatar.setPadding(pad, pad, pad, pad);
            binding.imgProfileAvatar.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            binding.imgProfileAvatar.setImageResource(R.drawable.ic_anonymous_pfp);
            binding.imgProfileAvatar.setImageTintList(ColorStateList.valueOf(Color.parseColor("#FACC15")));
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
